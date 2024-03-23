package edu.sdsu.cs646.memory_matrix

import kotlin.random.Random

enum class GameState {
    SUCCESS,
    FAIL,
    PENDING
}

class MemoryMatrixGame(private var startLevel: Int) {

    private var level: Int = startLevel
    private lateinit var grid: Array<BooleanArray>
    private var memorizeTimer: Long = 0
    private var memorizePhase: Boolean = true
    private val userSelectedCells: MutableSet<Pair<Int, Int>> = mutableSetOf()

    val gameState: GameState
        get() {
            for (pair in userSelectedCells) {
                if (!grid[pair.first][pair.second]) {
                    return GameState.FAIL
                }
            }
            return if (userSelectedCells.size == level) {
                GameState.SUCCESS
            } else {
                GameState.PENDING
            }
        }

    init {
        initializeGrid()
    }

    private fun initializeGrid() {
        grid = Array(level) { BooleanArray(level) }
    }

    fun start() {
        level = startLevel
    }

    fun currentLevel(): Int {
        return level
    }

    fun nextLevel() {
        level++
    }

    fun startMemorizePhase() {
        // Reset the grid and select random cells for memorization
        initializeGrid()
        userSelectedCells.clear()
        selectRandomCells()
        memorizeTimer = System.currentTimeMillis()
        memorizePhase = true
    }

    private fun selectRandomCells() {
        // Select cells for memorization
        for (i in 1..level) {
            var row: Int
            var col: Int
            do {
                row = Random.nextInt(level)
                col = Random.nextInt(level)
            } while (grid[row][col]) // Ensure we don't select the same cell multiple times
            grid[row][col] = true
        }
    }

    fun endMemorizePhase() {
        memorizePhase = false
    }

    fun selectCell(row: Int, col: Int) {
        userSelectedCells.add(Pair(row, col))
    }

    fun isInMemorizePhase(): Boolean {
        // Check if the game is currently in the memorization phase
        return memorizePhase
    }

    fun isCellSelected(row: Int, col: Int): Boolean {
        return grid[row][col]
    }

    fun isCellSelectedByUser(row: Int, col: Int): Boolean {
        return userSelectedCells.contains(Pair(row, col))
    }
}
