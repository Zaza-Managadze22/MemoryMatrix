package edu.sdsu.cs646.memory_matrix

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.workDataOf
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

private const val MEMORIZE_DURATION_SECONDS = 3
private const val START_LEVEL = 2
private const val FINAL_LEVEL = 8
private const val GUESS_ANSWER_DURATION_SECONDS = 10
const val KEY_MILLISECONDS_DURATION = "edu.sdsu.cs646.memory_matrix.MILLIS_DURATION"

class GameFragment : Fragment() {
    private lateinit var gameBoard: GridLayout
    private lateinit var startButton: Button
    private lateinit var game: MemoryMatrixGame
    private lateinit var progressBar: ProgressBar
    private lateinit var levelIndicator: TextView
    private lateinit var soundEffects: SoundEffects
    private lateinit var timerIndicator: CircularProgressIndicator
    private lateinit var workManager: WorkManager

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_game, container, false)

        // Initialize views
        gameBoard = view.findViewById(R.id.gameBoard)
        startButton = view.findViewById(R.id.startButton)
        progressBar = view.findViewById(R.id.progress_bar)
        levelIndicator = view.findViewById(R.id.levelIndicator)
        timerIndicator = view.findViewById(R.id.timerIndicator)


        // Initialize work manager
        workManager = WorkManager.getInstance(requireActivity())

        game = MemoryMatrixGame(START_LEVEL)

        soundEffects = SoundEffects.getInstance(requireActivity())

        // Set click listener for the start/restart button
        startButton.setOnClickListener {
            // Start or restart the game
            startGame()
        }

        return view
    }

    private fun startGame() {
        progressBar.visibility = View.VISIBLE
        progressBar.progress = 0
        levelIndicator.text = ""
        startButton.visibility = View.INVISIBLE
        game.timesUp = false
        game.start()
        nextRound()
    }

    private fun endGame() {
        workManager.cancelAllWork()

        // Display alert dialog when game is over
        val dialogBuilder = AlertDialog.Builder(requireActivity())
        if (game.gameState == GameState.SUCCESS) {
            soundEffects.win()
            dialogBuilder.setTitle(getString(R.string.congratulations_you_won))
        } else {
            soundEffects.lose()
            dialogBuilder.setTitle(getString(R.string.game_over))
        }
        // Set positive button to restart and negative button to quit
        dialogBuilder.setMessage(R.string.play_again_question)
            .setPositiveButton(R.string.restart) { _, _ -> startGame() }
            .setNegativeButton(R.string.quit_game) { _, _ ->
                requireActivity().finish()
                exitProcess(0)
            }
        dialogBuilder.create().show()
    }

    private fun nextRound() {
        // Advance to the next round
        populateGameBoard()
        game.startMemorizePhase()
        levelIndicator.text = getString(R.string.level_number, game.currentLevel(), FINAL_LEVEL)
        timerIndicator.visibility = View.INVISIBLE
        setCellColors()
        workManager.cancelAllWork()

        // End the memorization phase after the specified duration
        Handler(Looper.getMainLooper()).postDelayed({
            game.endMemorizePhase()
            setCellColors(true)
            setTimer()
        }, MEMORIZE_DURATION_SECONDS * 1000L)
    }

    private fun setTimer() {
        timerIndicator.progress = 0
        timerIndicator.visibility = View.VISIBLE
        val timerIndicatorWorkRequest: WorkRequest = OneTimeWorkRequestBuilder<TimerIndicatorWorker>()
            .setInputData(workDataOf(
                KEY_MILLISECONDS_DURATION to GUESS_ANSWER_DURATION_SECONDS * 1000L
            )).build()

        workManager.enqueue(timerIndicatorWorkRequest)
        val workId = timerIndicatorWorkRequest.id
        workManager.getWorkInfoByIdLiveData(workId).observe(requireActivity()) { workInfo ->
            if (workInfo != null) {
                if (workInfo.state == WorkInfo.State.RUNNING) {
                    val progress = workInfo.progress.getInt(KEY_TIMER_PROGRESS, 0)
                    timerIndicator.progress = progress
                } else if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    timerIndicator.progress = 100
                    game.timesUp = true
                    setCellColors()
                    endGame()
                }
            }
        }
    }


    private fun populateGameBoard() {
        // Clear existing views from the game board
        gameBoard.removeAllViews()
        val gridSize = game.currentLevel()

        // Set column and row count of the GridLayout
        gameBoard.columnCount = gridSize
        gameBoard.rowCount = gridSize

        // Calculate cell size based on screen width and grid size
        val cellSize = (resources.displayMetrics.widthPixels) / 10

        val scope = CoroutineScope(Dispatchers.Main)

        // Add buttons to represent the grid cells
        for (row in 0 until gridSize) {
            for (col in 0 until gridSize) {
                val button = Button(requireActivity())
                val layoutParams = GridLayout.LayoutParams().apply {
                    width = cellSize
                    height = cellSize
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                }
                button.layoutParams = layoutParams

                val neutralColor = ContextCompat.getColor(requireActivity(), R.color.brown)
                button.setBackgroundColor(neutralColor)

                // Set select listener
                button.setOnClickListener {
                    scope.launch { onSelectCell(it, row, col) }
                }

                gameBoard.addView(button)
            }
        }
    }

    private suspend fun onSelectCell(view: View, row: Int, col: Int) {
        if (!game.isInMemorizePhase() && game.gameState == GameState.PENDING) {
            game.selectCell(row, col)

            // Start the animations
            val rotateCell = ObjectAnimator.ofFloat(view, "rotationX", 180f)
            rotateCell.duration = 300
            val changeCellColor = ObjectAnimator.ofArgb(view, "backgroundColor", getCellColor(row, col))
            changeCellColor.duration = 300
            val animation = AnimatorSet()
            animation.play(rotateCell).with(changeCellColor)
            animation.start()

            // Delay execution before advancing to next round
            delay(400)

            if (game.gameState == GameState.SUCCESS) {
                progressBar.progress = getProgress()
                if (game.currentLevel() == FINAL_LEVEL) {
                    endGame()
                } else {
                    soundEffects.advance()
                    game.nextLevel()
                    nextRound()
                }
            } else if (game.gameState == GameState.FAIL) {
                setCellColors()
                endGame()
            }
        }
    }

    private fun getProgress(): Int {
        return (game.currentLevel() - START_LEVEL + 1) * 100 / (FINAL_LEVEL - START_LEVEL + 1)
    }

    private fun setCellColors(reset: Boolean = false) {
        val unselectedColor = ContextCompat.getColor(requireActivity(), R.color.brown)
        val gridSize = game.currentLevel()

        // Set all buttons' background color
        for (cellIndex in 0 until gameBoard.childCount) {
            val gridButton = gameBoard.getChildAt(cellIndex) as Button

            // Find the button's row and col
            val row = cellIndex / gridSize
            val col = cellIndex % gridSize

            val color = if (reset) unselectedColor else getCellColor(row, col)
            gridButton.setBackgroundColor(color)
        }
    }

    private fun getCellColor(row: Int, col: Int): Int {
        val unselectedColor = ContextCompat.getColor(requireActivity(), R.color.brown)
        val memorizeSelectedColor = ContextCompat.getColor(requireActivity(), R.color.yellow)
        val selectionCorrectColor = ContextCompat.getColor(requireActivity(), R.color.green)
        val selectionWrongColor = ContextCompat.getColor(requireActivity(), R.color.red)
        return if (game.isInMemorizePhase()) {
            if (game.isCellSelected(row, col)) {
                memorizeSelectedColor
            } else {
                unselectedColor
            }
        } else {
            if (game.isCellSelected(row, col)) {
                selectionCorrectColor
            } else if (game.isCellSelectedByUser(row, col)) {
                selectionWrongColor
            } else {
                unselectedColor
            }
        }
    }
}