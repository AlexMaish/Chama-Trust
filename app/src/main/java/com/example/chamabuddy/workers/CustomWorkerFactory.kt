//package com.example.chamabuddy.workers
//
//import android.content.Context
//import androidx.work.ListenableWorker
//import androidx.work.WorkerFactory
//import androidx.work.WorkerParameters
//import com.example.chamabuddy.util.SyncLogger
//import javax.inject.Inject
//import javax.inject.Provider
//
//class CustomWorkerFactory @Inject constructor(
//    private val workerFactories: Map<Class<out ListenableWorker>, @JvmSuppressWildcards Provider<ChildWorkerFactory>>
//) : WorkerFactory() {
//
//    override fun createWorker(
//        appContext: Context,
//        workerClassName: String,
//        workerParameters: WorkerParameters
//    ): ListenableWorker? {
//        SyncLogger.d("Creating worker: $workerClassName")
//
//        return try {
//            val workerClass = Class.forName(workerClassName).asSubclass(ListenableWorker::class.java)
//            val factoryProvider = workerFactories[workerClass]
//                ?: throw IllegalArgumentException("Unknown worker class $workerClassName")
//
//            factoryProvider.get().create(appContext, workerParameters).also {
//                SyncLogger.d("Worker created: ${it::class.simpleName}")
//            }
//        } catch (e: Exception) {
//            SyncLogger.e("Worker creation failed: ${e.message}", e)
//            null
//        }
//    }
//}