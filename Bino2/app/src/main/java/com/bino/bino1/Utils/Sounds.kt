package com.bino.bino1.Utils

import android.app.Activity
import android.media.MediaPlayer
import com.bino.bino1.R


    fun startSound(activity: Activity?, sound: Int) {
        val Sound: MediaPlayer = MediaPlayer.create(activity, sound)
        Sound.start()

}