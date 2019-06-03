package io.github.wulkanowy

import android.content.Context
import androidx.multidex.MultiDex
import androidx.work.Configuration
import androidx.work.WorkManager
import com.jakewharton.threetenabp.AndroidThreeTen
import dagger.android.AndroidInjector
import dagger.android.support.DaggerApplication
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.utils.Log
import io.github.wulkanowy.BuildConfig.DEBUG
import io.github.wulkanowy.di.DaggerAppComponent
import io.github.wulkanowy.services.sync.SyncWorkerFactory
import io.github.wulkanowy.utils.ActivityLifecycleLogger
import io.github.wulkanowy.utils.CrashlyticsTree
import io.github.wulkanowy.utils.DebugLogTree
import io.github.wulkanowy.utils.initCrashlytics
import io.reactivex.exceptions.UndeliverableException
import io.reactivex.plugins.RxJavaPlugins
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class WulkanowyApp : DaggerApplication() {

    @Inject
    lateinit var workerFactory: SyncWorkerFactory

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        MultiDex.install(this)
    }

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        WorkManager.initialize(this, Configuration.Builder().setWorkerFactory(workerFactory).build())
        RxJavaPlugins.setErrorHandler(::onError)

        initCrashlytics(applicationContext)
        initLogging()
    }

    private fun initLogging() {
        if (DEBUG) {
            Timber.plant(DebugLogTree())
            FlexibleAdapter.enableLogs(Log.Level.DEBUG)
        } else {
            Timber.plant(CrashlyticsTree())
        }
        registerActivityLifecycleCallbacks(ActivityLifecycleLogger())
    }

    private fun onError(error: Throwable) {
        if (error is UndeliverableException && error.cause is IOException || error.cause is InterruptedException) {
            Timber.e(error.cause, "An undeliverable error occurred")
        } else throw error
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return DaggerAppComponent.factory().create(this)
    }
}
