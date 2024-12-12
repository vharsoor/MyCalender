package dev.sudhanshu.calender.presentation.view

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLauncher : AppCompatActivity() {

    private val devicePolicyManager by lazy { getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    // Rename to avoid conflict with getComponentName()
    private val myComponentName by lazy { MyDeviceAdminReceiver.ComponentName(this) } // Replace with your DeviceAdminReceiver component
    private var targetAppPackage = "com.android.chrome" // Default target app package, can be changed dynamically

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if Lock Task Mode is permitted for the target app
        if (devicePolicyManager.isLockTaskPermitted(targetAppPackage)) {
            Log.d("AppLauncher", "Lock Task is permitted for the target app")
            launchTargetApp()
        } else {
            Log.e("AppLauncher", "Lock Task is not permitted for the target app")
        }
    }

    private fun launchTargetApp() {
        val intent = packageManager.getLaunchIntentForPackage(targetAppPackage)
        if (intent != null) {
            startActivity(intent) // Launch the target app
            lifecycleScope.launch {
                delay(500) // Ensure the app is launched before pinning
                startLockTask() // Pin the target app
                Log.d("AppLauncher", "Target app pinned in Lock Task mode")
            }
        } else {
            Log.e("AppLauncher", "Failed to find or launch the target app")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Exit Lock Task mode when this activity is destroyed
        stopLockTask()
        Log.d("AppLauncher", "Exited Lock Task mode")
    }

    // Function to dynamically set the target app package
    fun setTargetAppPackage(packageName: String) {
        targetAppPackage = packageName
    }
}
