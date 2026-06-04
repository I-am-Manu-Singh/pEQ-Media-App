package org.audiocular.peq.app.audio

import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer

interface BassLibrary : Library {
    fun BASS_Init(device: Int, freq: Int, flags: Int, win: Pointer?, clsid: Pointer?): Boolean
    fun BASS_Free(): Boolean
    fun BASS_StreamCreateFile(mem: Boolean, file: String, offset: Long, length: Long, flags: Int): Int
    fun BASS_ChannelPlay(handle: Int, restart: Boolean): Boolean
    fun BASS_ChannelPause(handle: Int): Boolean
    fun BASS_ChannelStop(handle: Int): Boolean
    fun BASS_ChannelGetPosition(handle: Int, mode: Int): Long
    fun BASS_ChannelSetPosition(handle: Int, pos: Long, mode: Int): Boolean
    fun BASS_ChannelBytes2Seconds(handle: Int, pos: Long): Double
    fun BASS_ChannelSeconds2Bytes(handle: Int, pos: Double): Long
    fun BASS_ChannelGetLevel(handle: Int): Int
    fun BASS_ChannelGetData(handle: Int, buffer: FloatArray, length: Int): Int
    fun BASS_ChannelSetAttribute(handle: Int, attrib: Int, value: Float): Boolean
    fun BASS_ChannelSetFX(handle: Int, type: Int, priority: Int): Int
    fun BASS_ChannelRemoveFX(handle: Int, fx: Int): Boolean
}

object Bass {
    var isLoaded = false
        private set

    val lib: BassLibrary? by lazy {
        try {
            val library = Native.load("bass", BassLibrary::class.java) as BassLibrary
            isLoaded = true
            library
        } catch (e: Throwable) {
            println("pEQControl: [BASS] Library binaries not found in search paths: ${e.message}")
            null
        }
    }
}
