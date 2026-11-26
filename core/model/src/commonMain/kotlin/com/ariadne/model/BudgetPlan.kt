package com.ariadne.model

/**
 * A trip's money, against its fence.
 *
 * Three pages had grown their own version of this arithmetic, and they could not
 * disagree more quietly: Today divided the budget by the days to get "a day of it",
 * Trip divided the spent total by the budget for a bar and the closed days by the
 * total days for a second bar, and Stats did both again with its own rounding. None
 * of that is hard, which is exactly why it drifted — and a page that says "₹3,400 a
 * day" next to a page that says "₹3,399.99 a day" is a page telling someone their
 * record is untrustworthy.
 *
 * So the arithmetic lives here, once, as a value: it is pure, it has no Compose in
 * it, and it is tested. The pages pass in what they know — a budget, the days in the
 * trip, what has been spent, how many days have been closed — and get back the
 * figures and fractions they draw.
 *
 * Two conventions worth stating:
 *
 *   - **A budget of zero is no budget.** `Trip.budgetMinor` is optional because a
 *     trip without one is still a trip, and a zero would turn every page into
 *     "₹0 left" for a fence nobody set.
 *   - **Pace is days, not money.** [paceFraction] is how much of the trip's *time*
 *     has passed, because the honest comparison for "am I spending too fast" is
 *     today against an even share of the days, not today against what other people
 *     spend.
 */
public data class BudgetPlan(
    /** The fence, in base minor units. Null — or zero — means there is no fence. */
    public val budgetMinor: Long?,
    /** Days in the trip. Clamped to at least one, because a trip has a start. */
    public val days: Int,
    /** Everything finished, in base minor units. What the totals already say. */
    public val spentMinor: Long,
    /** Days closed so far. Drives the pace tick; never more than [days]. */
    public val daysClosed: Int,
) {
    /** Days as a positive count: a trip always has a first day. */
    public val daysSafe: Int get() = days.coerceAtLeast(1)

    public val hasBudget: Boolean get() = (budgetMinor ?: 0L) > 0L

    /**
     * What one day of the budget is.
     *
     * Integer division, rounded *down*: a share quoted to the person is a spendable
     * number, and rounding it up would tell them they can spend more than the fence
     * allows. Null when there is no budget to divide.
     */
    public val dailyMinor: Long?
        get() = budgetMinor?.takeIf { it > 0L }?.let { it / daysSafe }

    /** What is left of the fence; negative when over. Null when there is no fence. */
    public val leftMinor: Long?
        get() = budgetMinor?.takeIf { it > 0L }?.let { it - spentMinor }

    /** True only when a fence exists and has been passed. */
    public val isOver: Boolean get() = (leftMinor ?: 0L) < 0L

    /** How much has been spent, as a fraction of the fence, clamped for a bar. */
    public val spendFraction: Float
        get() = fractionOf(spentMinor, budgetMinor?.takeIf { it > 0L })

    /** How much of the trip's time has passed, clamped. Zero when it has no days. */
    public val paceFraction: Float
        get() = if (days <= 0) 0f else (daysClosed.toFloat() / days.toFloat()).coerceIn(0f, 1f)

    /**
     * True when spending is ahead of the trip's own pace.
     *
     * The one comparison the bar's tick exists for. False without a budget: there is
     * nothing to be ahead of.
     */
    public val isAheadOfPace: Boolean
        get() = hasBudget && spendFraction > paceFraction

    /**
     * The day's share, minus what today has cost. Positive is room, negative is over.
     *
     * This is Today's line, and it is deliberately a *day* comparison rather than a
     * trip one: in the middle of a trip, "how am I doing today" is the only question
     * anyone can still act on.
     */
    public fun todayAgainstShare(todayMinor: Long): Long? =
        dailyMinor?.let { it - todayMinor }

    private fun fractionOf(part: Long, whole: Long?): Float {
        if (whole == null || whole <= 0L) return 0f
        return (part.toFloat() / whole.toFloat()).coerceIn(0f, 1f)
    }

    public companion object {
        /** A trip with no fence and nothing spent: the state before a budget is set. */
        public val None: BudgetPlan = BudgetPlan(budgetMinor = null, days = 0, spentMinor = 0, daysClosed = 0)

        public fun of(
            budgetMinor: Long?,
            days: Int,
            spentMinor: Long,
            daysClosed: Int,
        ): BudgetPlan = BudgetPlan(
            budgetMinor = budgetMinor,
            days = days,
            spentMinor = spentMinor,
            daysClosed = daysClosed,
        )
    }
}
