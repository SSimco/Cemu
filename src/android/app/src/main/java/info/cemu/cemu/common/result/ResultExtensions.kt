package info.cemu.cemu.common.result

inline fun Result<Unit>.then(next: () -> Result<Unit>): Result<Unit> =
    fold(onSuccess = { next() }, onFailure = { Result.failure(it) })

fun <T> Result.Companion.failure(message: String) = Result.failure<T>(Exception(message))
