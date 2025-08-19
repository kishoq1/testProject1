package com.example.testproject

import android.app.Application
import com.example.testproject.util.DownloaderImpl
import org.schabi.newpipe.extractor.NewPipe

class MyApplication : Application() {

    companion object {
        var isPlayerRunning = false
            private set

        fun onPlayerStart() {
            isPlayerRunning = true
        }

        fun onPlayerStop() {
            isPlayerRunning = false
        }
    }

    override fun onCreate() {
        super.onCreate()
        NewPipe.init(DownloaderImpl.init(null))
    }
}