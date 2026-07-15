package com.grimnej.lmcomment.ocr

import com.google.android.gms.tasks.Task
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

private val DirectExecutor = Executor { command -> command.run() }

/**
 * Awaits a Google Task that is borrowing a caller-owned resource.
 *
 * Google Tasks used by ML Kit do not expose a supported cancellation channel.
 * If the coroutine is cancelled, this bridge therefore waits non-cancellably
 * for the Task to finish before returning cancellation to the caller.
 */
internal suspend fun <T, R> Task<T>.awaitBorrowedResult(
    cancellationMessage: String,
    transform: (T) -> R,
): R {
    val value = try {
        awaitValue(cancellationMessage)
    } catch (cancelled: CancellationException) {
        withContext(NonCancellable) {
            awaitTerminal()
        }
        throw cancelled
    }

    return transform(value)
}

private suspend fun <T> Task<T>.awaitValue(
    cancellationMessage: String,
): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener(DirectExecutor) { completed ->
        if (!continuation.isActive) return@addOnCompleteListener

        val outcome = when {
            completed.isCanceled -> Result.failure(
                CancellationException(cancellationMessage),
            )

            !completed.isSuccessful -> Result.failure(
                completed.exception
                    ?: IllegalStateException("Task completed without a result."),
            )

            else -> Result.success(completed.result)
        }

        continuation.resumeOutcome(outcome)
    }
}

private suspend fun <T> Task<T>.awaitTerminal(): Unit =
    suspendCancellableCoroutine { continuation ->
        addOnCompleteListener(DirectExecutor) {
            if (continuation.isActive) {
                continuation.resumeWith(Result.success(Unit))
            }
        }
    }

private fun <T> CancellableContinuation<T>.resumeOutcome(outcome: Result<T>) {
    resumeWith(outcome)
}
