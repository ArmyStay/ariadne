package com.ariadne.app

/**
 * A trusted, best-effort "rate the app" prompt, ported from the KMP contest
 * starter kit's in-app review feature. Android uses Play Core's Review; iOS
 * uses SKStoreReviewController.
 */
public interface AppReviewManager {
    public fun requestReview()
}

/** A review manager that does nothing — used where the native prompt does not exist. */
public object NoAppReviewManager : AppReviewManager {
    override fun requestReview() = Unit
}
