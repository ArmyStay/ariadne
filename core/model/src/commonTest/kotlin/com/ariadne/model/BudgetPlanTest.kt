package com.ariadne.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The trip's money against its fence.
 *
 * These are the figures three pages used to compute for themselves, so the cases
 * that matter are the ones where the three could have disagreed: a budget of zero, a
 * trip with no days, spending past the fence, and a day's share that does not divide
 * evenly.
 */
class BudgetPlanTest {

    private fun plan(
        budget: Long? = 2_500_000L,
        days: Int = 10,
        spent: Long = 1_000_000L,
        closed: Int = 5,
    ) = BudgetPlan.of(budgetMinor = budget, days = days, spentMinor = spent, daysClosed = closed)

    @Test
    fun aDayOfTheBudgetDividesDown() {
        // ₹25,000 over 7 days is ₹3,571.42 a day; the share quoted is 3_571.42's
        // floor in minor units, never a rounded-up figure someone cannot spend.
        val seven = plan(budget = 2_500_000L, days = 7)
        assertEquals(357_142L, seven.dailyMinor)
    }

    @Test
    fun noBudgetMeansNoShareAndNoFence() {
        val none = plan(budget = null)
        assertFalse(none.hasBudget)
        assertNull(none.dailyMinor)
        assertNull(none.leftMinor)
        assertFalse(none.isOver)
        assertEquals(0f, none.spendFraction)
        assertFalse(none.isAheadOfPace)
        assertNull(none.todayAgainstShare(50_000L))
    }

    @Test
    fun aBudgetOfZeroIsABudgetNobodySet() {
        // `budgetMinor` is optional, so a stored zero has to mean "no fence" too —
        // otherwise every page reads "₹0 left" at someone who never set one.
        val zero = plan(budget = 0L, spent = 50_000L)
        assertFalse(zero.hasBudget)
        assertNull(zero.dailyMinor)
        assertFalse(zero.isOver)
        assertFalse(zero.isAheadOfPace)
    }

    @Test
    fun leftAndOverAreTheSameFigureWithASign() {
        assertEquals(1_500_000L, plan(spent = 1_000_000L).leftMinor)
        assertEquals(-500_000L, plan(spent = 3_000_000L).leftMinor)
        assertFalse(plan(spent = 1_000_000L).isOver)
        assertTrue(plan(spent = 3_000_000L).isOver)
    }

    @Test
    fun spendingExactlyTheFenceIsNotOverIt() {
        val exact = plan(spent = 2_500_000L)
        assertEquals(0L, exact.leftMinor)
        assertFalse(exact.isOver)
        assertEquals(1f, exact.spendFraction)
    }

    @Test
    fun fractionsAreClampedForABar() {
        assertEquals(0.4f, plan(spent = 1_000_000L).spendFraction)
        // Past the fence the bar is full, not longer than its track.
        assertEquals(1f, plan(spent = 9_000_000L).spendFraction)
        assertEquals(0f, plan(spent = 0L).spendFraction)
    }

    @Test
    fun paceIsTimeNotMoney() {
        assertEquals(0.5f, plan(days = 10, closed = 5).paceFraction)
        assertEquals(0f, plan(days = 10, closed = 0).paceFraction)
        assertEquals(1f, plan(days = 10, closed = 10).paceFraction)
        // A day count from a stale state cannot push the tick past the end.
        assertEquals(1f, plan(days = 10, closed = 14).paceFraction)
        assertEquals(0f, plan(days = 0, closed = 3).paceFraction)
    }

    @Test
    fun aheadOfPaceComparesSpendingWithElapsedTime() {
        // 40% spent, 50% of the trip gone: behind pace, which is the good news.
        assertFalse(plan(spent = 1_000_000L, closed = 5).isAheadOfPace)
        // 80% spent, 20% of the trip gone: ahead of it.
        assertTrue(plan(spent = 2_000_000L, closed = 2).isAheadOfPace)
        // Exactly on the pace is not ahead of it.
        assertFalse(plan(spent = 1_250_000L, closed = 5).isAheadOfPace)
    }

    @Test
    fun todayIsMeasuredAgainstTheDaysShareNotTheTrip() {
        val tenDays = plan(budget = 2_500_000L, days = 10)
        assertEquals(250_000L, tenDays.dailyMinor)
        assertEquals(250_000L - 40_000L, tenDays.todayAgainstShare(40_000L))
        assertEquals(250_000L - 400_000L, tenDays.todayAgainstShare(400_000L))
    }

    @Test
    fun aTripWithNoDaysStillDividesByOne() {
        // A trip whose dates have not been resolved yet has no days. Dividing by it
        // would be an arithmetic error on a page someone is looking at.
        val noDays = plan(days = 0, budget = 100_000L)
        assertEquals(1, noDays.daysSafe)
        assertEquals(100_000L, noDays.dailyMinor)
        assertEquals(0f, noDays.paceFraction)
    }

    @Test
    fun noneIsTheStateBeforeABudgetExists() {
        assertFalse(BudgetPlan.None.hasBudget)
        assertNull(BudgetPlan.None.dailyMinor)
        assertEquals(0f, BudgetPlan.None.spendFraction)
        assertEquals(0f, BudgetPlan.None.paceFraction)
    }
}
