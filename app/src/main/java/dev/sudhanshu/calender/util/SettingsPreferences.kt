package dev.sudhanshu.calender.util





import android.content.Context
import android.content.SharedPreferences

class SettingsPreferences private constructor(context: Context) {

    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "settings_prefs"
        private const val USER_ID_AVAILABLE = "user_id"

        @Volatile
        private var INSTANCE: SettingsPreferences? = null

        fun getInstance(context: Context): SettingsPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsPreferences(context).also { INSTANCE = it }
            }
        }
    }

    fun setUserId(userId: Int) {
        sharedPreferences.edit().putInt(USER_ID_AVAILABLE, userId).apply()
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(USER_ID_AVAILABLE, -1)
    }

    fun isUserAvailable(): Boolean {
        return sharedPreferences.getInt(USER_ID_AVAILABLE, -1) != -1
    }
}

