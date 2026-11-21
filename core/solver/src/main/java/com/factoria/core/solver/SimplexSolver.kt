package com.factoria.core.solver

import kotlin.math.abs

/**
 * Robust Two-Phase Simplex Algorithm for Linear Programming.
 * Solves:
 *   Minimize:   c^T * x
 *   Subject to: A * x (<=, >=, ==) b
 *               x >= 0
 */
class SimplexSolver(private val epsilon: Double = 1e-9) {

    enum class Status {
        OPTIMAL,
        INFEASIBLE,
        UNBOUNDED,
        CYCLE_OR_MAX_ITERATIONS
    }

    enum class ConstraintType {
        LESS_THAN_OR_EQUAL,    // <=  (type 0)
        GREATER_THAN_OR_EQUAL, // >=  (type 1)
        EQUAL                  // ==  (type 2)
    }

    data class Constraint(
        val coefficients: DoubleArray,
        val type: ConstraintType,
        val rhs: Double
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Constraint) return false
            return coefficients.contentEquals(other.coefficients) && type == other.type && rhs == other.rhs
        }

        override fun hashCode(): Int {
            var result = coefficients.contentHashCode()
            result = 31 * result + type.hashCode()
            result = 31 * result + rhs.hashCode()
            return result
        }
    }

    data class LpResult(
        val status: Status,
        val objectiveValue: Double,
        val variableValues: DoubleArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is LpResult) return false
            return status == other.status &&
                objectiveValue == other.objectiveValue &&
                variableValues.contentEquals(other.variableValues)
        }

        override fun hashCode(): Int {
            var result = status.hashCode()
            result = 31 * result + objectiveValue.hashCode()
            result = 31 * result + variableValues.contentHashCode()
            return result
        }
    }

    fun solve(
        objective: DoubleArray,
        constraints: List<Constraint>,
        maxIterations: Int = 10000
    ): LpResult {
        val n = objective.size
        val m = constraints.size

        if (m == 0) {
            return LpResult(Status.OPTIMAL, 0.0, DoubleArray(n))
        }

        // Copy and normalize RHS >= 0
        val aCoeffs = Array(m) { DoubleArray(n) }
        val bRhs = DoubleArray(m)
        val eqTypes = Array(m) { ConstraintType.EQUAL }

        for (i in 0 until m) {
            val c = constraints[i]
            var rhs = c.rhs
            val coeff = c.coefficients.clone()
            var t = c.type

            if (rhs < 0.0) {
                rhs = -rhs
                for (j in 0 until n) {
                    coeff[j] = -coeff[j]
                }
                t = when (t) {
                    ConstraintType.LESS_THAN_OR_EQUAL -> ConstraintType.GREATER_THAN_OR_EQUAL
                    ConstraintType.GREATER_THAN_OR_EQUAL -> ConstraintType.LESS_THAN_OR_EQUAL
                    ConstraintType.EQUAL -> ConstraintType.EQUAL
                }
            }
            aCoeffs[i] = coeff
            bRhs[i] = rhs
            eqTypes[i] = t
        }

        val numSlack = eqTypes.count { it == ConstraintType.LESS_THAN_OR_EQUAL }
        val numSurplus = eqTypes.count { it == ConstraintType.GREATER_THAN_OR_EQUAL }
        val numArt = eqTypes.count { it == ConstraintType.GREATER_THAN_OR_EQUAL || it == ConstraintType.EQUAL }

        val totalVars = n + numSlack + numSurplus + numArt
        // Row 0: Phase 1 objective
        // Row 1: Phase 2 objective
        // Rows 2..m+1: Constraints
        val tab = Array(m + 2) { DoubleArray(totalVars + 1) }
        val basis = IntArray(m)

        var sCol = n
        var surCol = n + numSlack
        var artCol = n + numSlack + numSurplus
        val artStart = artCol

        for (i in 0 until m) {
            val row = i + 2
            for (j in 0 until n) {
                tab[row][j] = aCoeffs[i][j]
            }
            tab[row][totalVars] = bRhs[i]

            when (eqTypes[i]) {
                ConstraintType.LESS_THAN_OR_EQUAL -> {
                    tab[row][sCol] = 1.0
                    basis[i] = sCol
                    sCol++
                }
                ConstraintType.GREATER_THAN_OR_EQUAL -> {
                    tab[row][surCol] = -1.0
                    tab[row][artCol] = 1.0
                    basis[i] = artCol
                    surCol++
                    artCol++
                }
                ConstraintType.EQUAL -> {
                    tab[row][artCol] = 1.0
                    basis[i] = artCol
                    artCol++
                }
            }
        }

        // Phase 2 objective (minimize c^T * x):
        // We initialize row 1 with c
        for (j in 0 until n) {
            tab[1][j] = objective[j]
        }

        // Phase 1 objective: minimize sum of artificial variables
        for (j in artStart until totalVars) {
            tab[0][j] = 1.0
        }
        for (i in 0 until m) {
            if (basis[i] >= artStart) {
                val row = i + 2
                for (j in 0..totalVars) {
                    tab[0][j] -= tab[row][j]
                }
            }
        }

        // Phase 1 execution
        if (numArt > 0) {
            val p1Status = solvePhase(tab, basis, 0, totalVars, m, maxIterations)
            if (p1Status != Status.OPTIMAL || abs(tab[0][totalVars]) > 1e-5) {
                return LpResult(Status.INFEASIBLE, 0.0, DoubleArray(n))
            }

            // Pivot out any remaining artificial variables in basis
            for (i in 0 until m) {
                if (basis[i] >= artStart) {
                    val row = i + 2
                    for (j in 0 until artStart) {
                        if (abs(tab[row][j]) > epsilon) {
                            pivot(tab, basis, row, j)
                            break
                        }
                    }
                }
            }
        }

        // Prepare Phase 2: eliminate basic variables from row 1
        for (i in 0 until m) {
            val bVar = basis[i]
            if (bVar < artStart) {
                val factor = tab[1][bVar]
                if (abs(factor) > epsilon) {
                    val row = i + 2
                    for (j in 0..totalVars) {
                        tab[1][j] -= factor * tab[row][j]
                    }
                }
            }
        }

        // Phase 2 execution
        val p2Status = solvePhase(tab, basis, 1, artStart, m, maxIterations)
        if (p2Status != Status.OPTIMAL) {
            return LpResult(p2Status, 0.0, DoubleArray(n))
        }

        val resultVars = DoubleArray(n)
        for (i in 0 until m) {
            if (basis[i] < n) {
                resultVars[basis[i]] = maxOf(0.0, tab[i + 2][totalVars])
            }
        }

        // Compute exact objective value from original objective coefficients
        var calculatedObj = 0.0
        for (j in 0 until n) {
            calculatedObj += objective[j] * resultVars[j]
        }

        return LpResult(Status.OPTIMAL, calculatedObj, resultVars)
    }

    private fun solvePhase(
        tab: Array<DoubleArray>,
        basis: IntArray,
        objRow: Int,
        maxCol: Int,
        m: Int,
        maxIterations: Int
    ): Status {
        val totalCols = tab[0].size - 1

        for (iter in 0 until maxIterations) {
            // Find most negative reduced cost (minimization)
            var col = -1
            var minVal = -epsilon
            for (j in 0 until maxCol) {
                if (tab[objRow][j] < minVal) {
                    minVal = tab[objRow][j]
                    col = j
                }
            }

            if (col == -1) {
                return Status.OPTIMAL
            }

            // Ratio test
            var row = -1
            var minRatio = Double.POSITIVE_INFINITY
            for (i in 0 until m) {
                val r = i + 2
                val `val` = tab[r][col]
                if (`val` > epsilon) {
                    val ratio = tab[r][totalCols] / `val`
                    if (ratio < minRatio - epsilon) {
                        minRatio = ratio
                        row = r
                    }
                }
            }

            if (row == -1) {
                return Status.UNBOUNDED
            }

            pivot(tab, basis, row, col)
        }
        return Status.CYCLE_OR_MAX_ITERATIONS
    }

    private fun pivot(tab: Array<DoubleArray>, basis: IntArray, pivotRow: Int, pivotCol: Int) {
        val totalCols = tab[0].size
        val pivotVal = tab[pivotRow][pivotCol]

        for (j in 0 until totalCols) {
            tab[pivotRow][j] /= pivotVal
        }

        for (r in tab.indices) {
            if (r != pivotRow) {
                val factor = tab[r][pivotCol]
                if (abs(factor) > epsilon) {
                    for (c in 0 until totalCols) {
                        tab[r][c] -= factor * tab[pivotRow][c]
                    }
                }
            }
        }
        basis[pivotRow - 2] = pivotCol
    }
}
