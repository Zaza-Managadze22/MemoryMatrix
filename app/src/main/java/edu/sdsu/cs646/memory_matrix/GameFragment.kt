package edu.sdsu.cs646.memory_matrix

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
import kotlin.system.exitProcess

private const val MEMORIZE_DURATION_SECONDS = 3
private const val START_LEVEL = 2
private const val FINAL_LEVEL = 8

class GameFragment : Fragment() {
    private lateinit var gameBoard: GridLayout
    private lateinit var startButton: Button
    private lateinit var game: MemoryMatrixGame
    private lateinit var progressBar: ProgressBar
    private lateinit var levelIndicator: TextView

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

        game = MemoryMatrixGame(START_LEVEL)

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
        game.start()
        nextRound()
    }

    private fun endGame() {
        // Display alert dialog when game is over
        val dialogBuilder = AlertDialog.Builder(requireActivity())
        if (game.gameState == GameState.SUCCESS) {
            dialogBuilder.setTitle(getString(R.string.congratulations_you_won))
        } else {
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
        setCellColors()
        // End the memorization phase after the specified duration
        Handler(Looper.getMainLooper()).postDelayed({
            game.endMemorizePhase()
            setCellColors(true)
        }, MEMORIZE_DURATION_SECONDS * 1000L)
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

                button.setOnClickListener {
                    if (!game.isInMemorizePhase() && game.gameState == GameState.PENDING) {
                        game.selectCell(row, col)
                        it.setBackgroundColor(getCellColor(row, col))
                        if (game.gameState == GameState.SUCCESS) {
                            progressBar.progress = getProgress()
                            if (game.currentLevel() == FINAL_LEVEL) {
                                endGame()
                            } else {
                                game.nextLevel()
                                nextRound()
                            }
                        } else if (game.gameState == GameState.FAIL) {
                            setCellColors()
                            endGame()
                        }
                    }
                }

                gameBoard.addView(button)
            }
        }
    }

    private fun getProgress(): Int {
        return (game.currentLevel() - START_LEVEL + 1) * 100 / (FINAL_LEVEL - START_LEVEL + 1)
    }

    private fun setCellColors(reset: Boolean = false) {
        val unselectedColor = ContextCompat.getColor(requireActivity(), R.color.brown)
        val gridSize = game.currentLevel()

        // Set all buttons' background colo\
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