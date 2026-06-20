package org.audiocular.peq.app

import platform.posix.CLOCK_MONOTONIC
import platform.posix.clock_gettime
import platform.posix.remove
import kotlinx.cinterop.*

@OptIn(ExperimentalForeignApi::class)
actual fun clock_time_nanos(): Long = memScoped {
    val timespec = alloc<platform.posix.timespec>()
    clock_gettime(CLOCK_MONOTONIC.toUInt(), timespec.ptr)
    (timespec.tv_sec * 1_000_000_000L) + timespec.tv_nsec
}

actual fun deleteLocalFile(path: String): Boolean {
    return remove(path) == 0
}
