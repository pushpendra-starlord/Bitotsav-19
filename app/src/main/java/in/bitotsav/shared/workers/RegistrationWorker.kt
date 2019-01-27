package `in`.bitotsav.shared.workers

import `in`.bitotsav.profile.utils.fetchCollegeListAsync
import `in`.bitotsav.shared.workers.RegistrationWorkType.valueOf
import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking

private const val TAG = "RegistrationWorker"

enum class RegistrationWorkType {
    FETCH_COLLEGE_LIST
}

class RegistrationWorker(context: Context, params: WorkerParameters): Worker(context, params) {
    override fun doWork(): Result {

        return runBlocking {
            try {
                val type = inputData.getString("type")?.let { valueOf(it) }
                    ?: return@runBlocking Result.failure(workDataOf("Error" to "Invalid work type"))
                fetchCollegeListAsync().await()
                return@runBlocking Result.success()
            } catch (e: Exception) {
                Log.d(TAG, e.message)
                return@runBlocking Result.retry()
            }
        }
    }
}