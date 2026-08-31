package com.bille.android

import android.app.Application
import com.bille.android.data.repository.DaemonSyncRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BilleApplication : Application() {

    @Inject
    lateinit var daemonSyncRepository: DaemonSyncRepository

    override fun onCreate() {
        super.onCreate()
        daemonSyncRepository.startSync()
    }
}
