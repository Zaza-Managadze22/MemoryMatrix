package edu.sdsu.cs646.memory_matrix

import android.content.Context
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters

const val KEY_TIMER_PROGRESS = "edu.sdsu.cs646.memory_matrix.PROGRESS"
class TimerIndicatorWorker (context: Context, parameters: WorkerParameters) :
    Worker(context, parameters) {
    override fun doWork(): Result {
        val duration = inputData.getLong(KEY_MILLISECONDS_DURATION, 0)
        if (duration <= 0) {
            return Result.failure()
        }

        val timer = Timer(duration)

        while (!timer.finished) {
            // Report progress every 0.1 seconds
            setProgressAsync(Data.Builder().putInt(KEY_TIMER_PROGRESS, timer.progressPercent).build())
            Thread.sleep(100)
        }

        return Result.success()
    }
}