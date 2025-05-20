package com.github.droidworksstudio.common

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.github.droidworksstudio.mlauncher.CrashReportActivity
import com.github.droidworksstudio.mlauncher.helper.getDeviceInfo
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.LinkedBlockingQueue
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    companion object {
        private val userActions = LinkedBlockingQueue<String>(50) // Stores last 50 user actions

        fun logUserAction(action: String) {
            val timeStamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            userActions.offer("$timeStamp - $action")
            if (userActions.size > 50) userActions.poll() // Remove oldest if over limit
        }

        fun customReportSender(context: Context): Uri? {
            val logFile: File = try {
                val packageManager = context.packageManager
                val packageInfo = packageManager.getPackageInfo(context.packageName, 0)

                // Use internal storage for saving the crash log
                val crashDir = File(context.filesDir, "crash_logs")  // Internal storage
                if (!crashDir.exists()) crashDir.mkdirs()

                val crashFile = File(crashDir, "${packageInfo.packageName}-crash-report.txt")

                // Check if the file exists before attempting to read
                if (crashFile.exists()) {
                    // Read the content of the file
                    val fileInputStream = FileInputStream(crashFile)
                    val inputStreamReader = InputStreamReader(fileInputStream)
                    val stringBuilder = StringBuilder()

                    // Read the file line by line
                    inputStreamReader.forEachLine { stringBuilder.append(it).append("\n") }

                    // Log the content of the crash report file
                    Log.d("CrashHandler", "Crash Report Content:\n${stringBuilder}")
                } else {
                    Log.e("CrashHandler", "Crash report file does not exist.")
                }

                File(crashDir, "${packageInfo.packageName}-crash-report.txt")
            } catch (e: Exception) {
                Log.e("CrashHandler", "Error determining crash log file location: ${e.message}")
                return null // Return null if something goes wrong
            }

            // Ensure the file exists
            if (!logFile.exists()) {
                return null
            }

            // Use FileProvider to get a content Uri for the file
            return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", logFile)
        }
    }

    override fun uncaughtException(thread: Thread, exception: Throwable) {
        Log.e("CrashHandler", "Caught exception: ${exception.message}", exception)

        // Step 1: Save custom crash log
        saveCrashLog(exception)

        // Step 2: Start CrashReportActivity with the crash details
        val intent = Intent(context, CrashReportActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        context.startActivity(intent)

        // Kill the process
        android.os.Process.killProcess(android.os.Process.myPid())
        exitProcess(1)
    }

    private fun saveCrashLog(exception: Throwable): File {
        val logFile: File = try {
            val packageManager = context.packageManager
            val packageInfo: PackageInfo = packageManager.getPackageInfo(context.packageName, 0)

            // Use internal storage for saving the crash log
            val crashDir = File(context.filesDir, "crash_logs")  // This is internal storage
            if (!crashDir.exists()) crashDir.mkdirs()

            File(crashDir, "${packageInfo.packageName}-crash-report.txt")
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error determining crash log file location: ${e.message}")
            // In case of error, use a default file name
            File(context.filesDir, "default-crash-report.txt")
        }

        try {
            val runtime = Runtime.getRuntime()
            val usedMemInMB = (runtime.totalMemory() - runtime.freeMemory()) / 1048576L
            val maxHeapSizeInMB = runtime.maxMemory() / 1048576L

            FileWriter(logFile).use { writer ->
                PrintWriter(writer).use { printWriter ->
                    printWriter.println("Crash Report - ${Date()}")
                    printWriter.println("Thread: ${Thread.currentThread().name}")

                    printWriter.println("\n=== Device Info ===")
                    printWriter.println(getDeviceInfo(context))

                    printWriter.println("\n=== Memory Info ===")
                    printWriter.println("Used Memory (MB): $usedMemInMB")
                    printWriter.println("Max Heap Size (MB): $maxHeapSizeInMB")

                    printWriter.println("\n=== Recent User Actions ===")
                    userActions.forEach { printWriter.println(it) }

                    printWriter.println("\n=== Crash LogCat ===")
                    val process = Runtime.getRuntime().exec("logcat -d -t 100 AndroidRuntime:E *:S")
                    val reader = BufferedReader(InputStreamReader(process.inputStream))
                    reader.forEachLine { printWriter.println(it) }

                    printWriter.println("\n=== Crash Stack Trace ===")
                    exception.printStackTrace(printWriter)

                }
            }
        } catch (e: Exception) {
            Log.e("CrashHandler", "Error writing crash log: ${e.message}")
        }
        return logFile
    }
}