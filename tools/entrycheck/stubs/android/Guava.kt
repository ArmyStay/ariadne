// A slice of Guava, because CameraX's ProcessCameraProvider hands back a
// ListenableFuture and the entry check should compile against the same type the
// app compiles against.
package com.google.common.util.concurrent

import java.util.concurrent.Executor
import java.util.concurrent.Future

interface ListenableFuture<V> : Future<V> {
    fun addListener(listener: Runnable, executor: Executor)
}

object Futures {
    fun <V> immediateFailedFuture(t: Throwable): ListenableFuture<V> =
        object : ListenableFuture<V> {
            override fun addListener(listener: Runnable, executor: Executor) = Unit
            override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
            override fun isCancelled(): Boolean = true
            override fun isDone(): Boolean = true
            override fun get(): V = throw java.util.concurrent.ExecutionException(t)
            override fun get(timeout: Long, unit: java.util.concurrent.TimeUnit): V = get()
        }
}
