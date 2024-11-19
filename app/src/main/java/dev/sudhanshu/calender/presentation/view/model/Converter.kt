package dev.sudhanshu.calender.presentation.view.model
import androidx.room.TypeConverter
import dev.sudhanshu.calender.presentation.view.InsertTask.ConferenceData


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

class Converters {

    @TypeConverter
    fun fromConferenceData(conferenceData: ConferenceData?): String? {
        return Gson().toJson(conferenceData)
    }

    @TypeConverter
    fun toConferenceData(conferenceDataString: String?): ConferenceData? {
        return if (conferenceDataString == null) null else Gson().fromJson(conferenceDataString, object : TypeToken<ConferenceData>() {}.type)
    }
}