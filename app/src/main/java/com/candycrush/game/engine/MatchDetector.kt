package com.candycrush.game.engine

import com.candycrush.game.model.BoardState
import com.candycrush.game.model.CandyType
import com.candycrush.game.model.MatchResult
import com.candycrush.game.model.Position

/**
 * Detects all matches of 3 or more candies on the board.
 *
 * The algorithm works in 4 steps:
 * 1. Scan each row for horizontal runs of 3+ same-colored candies
 * 2. Scan each column for vertical runs of 3+ same-colored candies
 * 3. Merge overlapping runs of the same color (this detects L and T shapes)
 * 4. Classify each merged group to determine what special candy it creates
 *
 * This is the most important algorithm in the game — it determines
 * when candies should be cleared and what special candies to create.
 */
class MatchDetector {

    /**
     * A run of consecutive same-colored candies in a line.
     * @param positions The positions in this run (in order)
     * @param candyType The color of the candies
     * @param isHorizontal True if this is a horizontal run, false if vertical
     */
    private data class Run(
        val positions: List<Position>,
        val candyType: CandyType,
        val isHorizontal: Boolean
    )

    /**
     * Find all matches on the board.
     *
     * @param board The current board state
     * @return A list of MatchResult objects, each representing a group of matched candies.
     *         Returns an empty list if there are no matches.
     */
    fun findAllMatches(board: BoardState): List<MatchResult> {
        // Step 1 & 2: Find all horizontal and vertical runs
        val allRuns = findHorizontalRuns(board) + findVerticalRuns(board)

        if (allRuns.isEmpty()) return emptyList()

        // Step 3: Merge overlapping runs of the same color
        val mergedGroups = mergeRuns(allRuns)

        // Step 4: Convert merged groups into MatchResults with classification
        return mergedGroups.map { group -> classifyMatch(group) }
    }

    /**
     * Step 1: Find all horizontal runs of 3+ same-colored candies.
     *
     * For each row, we scan left to right and track consecutive candies
     * of the same type. When the type changes, if the run is 3+ long,
     * we record it.
     */
    private fun findHorizontalRuns(board: BoardState): List<Run> {
        val runs = mutableListOf<Run>()

        for (row in 0 until board.rows) {
            var runStart = 0
            var currentType: CandyType? = null

            for (col in 0 until board.cols) {
                val candy = board.grid[row][col]

                if (candy != null && candy.type == currentType) {
                    // Continue the current run
                    continue
                }

                // The run has ended (different type or null)
                // Check if the previous run was 3+ long
                if (currentType != null && col - runStart >= 3) {
                    val positions = (runStart until col).map { c -> Position(row, c) }
                    runs.add(Run(positions, currentType, isHorizontal = true))
                }

                // Start a new run
                currentType = candy?.type
                runStart = col
            }

            // Don't forget to check the last run in the row
            if (currentType != null && board.cols - runStart >= 3) {
                val positions = (runStart until board.cols).map { c -> Position(row, c) }
                runs.add(Run(positions, currentType, isHorizontal = true))
            }
        }

        return runs
    }

    /**
     * Step 2: Find all vertical runs of 3+ same-colored candies.
     * Same logic as horizontal, but scanning top to bottom in each column.
     */
    private fun findVerticalRuns(board: BoardState): List<Run> {
        val runs = mutableListOf<Run>()

        for (col in 0 until board.cols) {
            var runStart = 0
            var currentType: CandyType? = null

            for (row in 0 until board.rows) {
                val candy = board.grid[row][col]

                if (candy != null && candy.type == currentType) {
                    continue
                }

                if (currentType != null && row - runStart >= 3) {
                    val positions = (runStart until row).map { r -> Position(r, col) }
                    runs.add(Run(positions, currentType, isHorizontal = false))
                }

                currentType = candy?.type
                runStart = row
            }

            if (currentType != null && board.rows - runStart >= 3) {
                val positions = (runStart until board.rows).map { r -> Position(r, col) }
                runs.add(Run(positions, currentType, isHorizontal = false))
            }
        }

        return runs
    }

    /**
     * Step 3: Merge overlapping runs of the same candy type.
     *
     * Two runs should be merged if:
     * - They have the same CandyType
     * - They share at least one position (overlap)
     *
     * This is how L-shapes and T-shapes are detected: a horizontal run
     * and a vertical run of the same color that share a corner position
     * get merged into a single group.
     *
     * We use a simple iterative merge approach (good enough for small boards).
     */
    private fun mergeRuns(runs: List<Run>): List<MergedGroup> {
        // Convert each run into a MergedGroup
        val groups = runs.map { run ->
            MergedGroup(
                positions = run.positions.toMutableSet(),
                candyType = run.candyType,
                horizontalLengths = if (run.isHorizontal) mutableListOf(run.positions.size) else mutableListOf(),
                verticalLengths = if (!run.isHorizontal) mutableListOf(run.positions.size) else mutableListOf(),
                hasHorizontal = run.isHorizontal,
                hasVertical = !run.isHorizontal
            )
        }.toMutableList()

        // Iteratively merge groups that overlap
        var merged = true
        while (merged) {
            merged = false
            for (i in groups.indices) {
                for (j in i + 1 until groups.size) {
                    if (groups[i].candyType == groups[j].candyType &&
                        groups[i].positions.any { it in groups[j].positions }
                    ) {
                        // Merge group j into group i
                        groups[i].positions.addAll(groups[j].positions)
                        groups[i].horizontalLengths.addAll(groups[j].horizontalLengths)
                        groups[i].verticalLengths.addAll(groups[j].verticalLengths)
                        groups[i].hasHorizontal = groups[i].hasHorizontal || groups[j].hasHorizontal
                        groups[i].hasVertical = groups[i].hasVertical || groups[j].hasVertical
                        groups.removeAt(j)
                        merged = true
                        break
                    }
                }
                if (merged) break
            }
        }

        return groups
    }

    /**
     * Step 4: Classify a merged group into a MatchResult.
     *
     * Determines:
     * - matchLength: The longest single run in the group
     * - isLShape: Whether the group has both horizontal and vertical components
     * - pivotPosition: Where a special candy should be placed
     */
    private fun classifyMatch(group: MergedGroup): MatchResult {
        val allLengths = group.horizontalLengths + group.verticalLengths
        val maxLength = allLengths.maxOrNull() ?: 3
        val isLShape = group.hasHorizontal && group.hasVertical

        // Determine the pivot position:
        // - For L/T shapes: find the intersection point (position shared by both directions)
        // - For straight lines: use the middle position
        val pivotPosition = if (isLShape) {
            findIntersection(group) ?: group.positions.first()
        } else {
            findMiddle(group.positions)
        }

        return MatchResult(
            positions = group.positions.toSet(),
            candyType = group.candyType,
            matchLength = maxLength,
            isLShape = isLShape,
            pivotPosition = pivotPosition
        )
    }

    /**
     * Find the intersection point of an L/T shaped match.
     * This is the position that appears in both a horizontal and vertical run.
     */
    private fun findIntersection(group: MergedGroup): Position? {
        // Find positions that could be at the corner/intersection
        // A position at the intersection will have neighbors in both row and column directions
        for (pos in group.positions) {
            val hasHorizontalNeighbor = Position(pos.row, pos.col - 1) in group.positions ||
                    Position(pos.row, pos.col + 1) in group.positions
            val hasVerticalNeighbor = Position(pos.row - 1, pos.col) in group.positions ||
                    Position(pos.row + 1, pos.col) in group.positions

            if (hasHorizontalNeighbor && hasVerticalNeighbor) {
                return pos
            }
        }
        return null
    }

    /**
     * Find the middle position of a set of positions.
     * For a straight line of 3: returns the center candy.
     * For a line of 4: returns the 2nd candy.
     * For a line of 5: returns the 3rd candy.
     */
    private fun findMiddle(positions: Set<Position>): Position {
        val sorted = positions.sortedWith(compareBy({ it.row }, { it.col }))
        return sorted[sorted.size / 2]
    }

    /**
     * Internal data class for tracking merged groups during the merge step.
     */
    private data class MergedGroup(
        val positions: MutableSet<Position>,
        val candyType: CandyType,
        val horizontalLengths: MutableList<Int>,
        val verticalLengths: MutableList<Int>,
        var hasHorizontal: Boolean,
        var hasVertical: Boolean
    )
}
