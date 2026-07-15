package com.grimnej.lmcomment.ocr

import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class BorrowedTaskAwaiterTest {
    @Test
    fun transformsSuccessfulTask() = runBlocking {
        val result = Tasks.forResult("Ada").awaitBorrowedResult(
            cancellationMessage = "cancelled",
            transform = { value -> value.uppercase() },
        )

        assertTrue(result == "ADA")
    }

    @Test
    fun propagatesTaskFailureWithoutTransformingIt() = runBlocking {
        val cause = IllegalStateException("recognizer unavailable")

        try {
            Tasks.forException<String>(cause).awaitBorrowedResult(
                cancellationMessage = "cancelled",
                transform = { value -> value.uppercase() },
            )
            fail("Expected OCR failure")
        } catch (error: IllegalStateException) {
            // kotlinx.coroutines may create an equivalent exception while
            // recovering a suspension stack trace; the original stays causal.
            assertTrue(error === cause || error.cause === cause)
            assertTrue(error.message == "recognizer unavailable")
        }
    }

    @Test
    fun callerCancellationWaitsForBorrowingTaskToReachTerminalState() = runBlocking {
        val taskSource = TaskCompletionSource<String>()
        var callerReturned = false
        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                taskSource.task.awaitBorrowedResult(
                    cancellationMessage = "cancelled",
                    transform = { value -> value.uppercase() },
                )
            } finally {
                callerReturned = true
            }
        }

        job.cancel(CancellationException("test cancellation"))
        yield()
        assertFalse("Borrowed resource was released before Task completion", callerReturned)

        taskSource.setResult("finished")
        job.join()
        assertTrue(callerReturned)
        assertTrue(job.isCancelled)
    }
}
