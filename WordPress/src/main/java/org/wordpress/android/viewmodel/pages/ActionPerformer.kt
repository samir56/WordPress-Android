package org.wordpress.android.viewmodel.pages

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.PostStore.OnPostUploaded
import javax.inject.Inject
import kotlinx.coroutines.experimental.suspendCancellableCoroutine
import kotlinx.coroutines.experimental.withTimeoutOrNull
import java.util.concurrent.TimeUnit.SECONDS
import kotlin.coroutines.experimental.Continuation

class ActionPerformer
@Inject constructor(dispatcher: Dispatcher) {
    private var continuation: Continuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    suspend fun performAction(action: PageAction) {
        val success = suspendCoroutineWithTimeout<Boolean>(30) { cont ->
            continuation = cont
            action.perform()
        }
        continuation = null

        if (success == true) {
            action.onSuccess()
        } else {
            action.onError()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostUploaded(event: OnPostUploaded) {
        if (event.post.isPage) {
            continuation?.resume(!event.isError)
        }
    }

    private suspend inline fun <T> suspendCoroutineWithTimeout(
        timeout: Long,
        crossinline block: (Continuation<T>) -> Unit
    ) = withTimeoutOrNull(timeout, SECONDS) {
        suspendCancellableCoroutine(block = block)
    }

    data class PageAction(val perform: () -> Unit) {
        var onSuccess: () -> Unit = { }
        var onError: () -> Unit = { }
        var undo: () -> Unit = { }
    }
}