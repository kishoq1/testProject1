package com.example.testproject

import android.app.Application
import com.example.testproject.util.DownloaderImpl
import org.schabi.newpipe.extractor.NewPipe

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NewPipe.init(DownloaderImpl.init(null))
    }
}