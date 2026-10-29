package com.example.util

interface SoundPlayer {
    fun init()
    fun playStartSignal()
    fun playCountdownBeep(secondsRemaining: Int = 1)
}

class DefaultSoundPlayer : SoundPlayer {
    override fun init() = SoundUtil.init()
    override fun playStartSignal() = SoundUtil.playStartSignal()
    override fun playCountdownBeep(secondsRemaining: Int) = SoundUtil.playCountdownBeep(secondsRemaining)
}

class NoOpSoundPlayer : SoundPlayer {
    override fun init() {}
    override fun playStartSignal() {}
    override fun playCountdownBeep(secondsRemaining: Int) {}
}
