package info.cemu.cemu.common.either

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

data class Success<out T, out E>(val data: T) : Either<T, E>()
data class Error<out T, out E>(val error: E) : Either<T, E>()

sealed class Either<out T, out E> {
    inline fun <R> fold(onSuccess: (T) -> R, onError: (E) -> R): R = when (this) {
        is Success -> onSuccess(data)
        is Error -> onError(error)
    }

    inline fun onError(block: (E) -> Unit) {
        if (this is Error) {
            block(error)
        }
    }
}

inline fun attempt(block: () -> Unit): Either<Unit, String> = try {
    block()
    Success(Unit)
} catch (e: Exception) {
    Error(e.message ?: "Unknown error")
}

suspend inline fun attemptWithContext(
    context: CoroutineContext,
    noinline block: suspend CoroutineScope.() -> Unit
): Either<Unit, String> = withContext(context) {
    attempt { block() }
}

inline fun Either<Unit, String>.bind(next: () -> Either<Unit, String>): Either<Unit, String> =
    fold(onSuccess = { next() }, onError = { Error(it) })
