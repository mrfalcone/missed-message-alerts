/*
 * Copyright 2011 Michael R. Falcone
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mma.missedmessagealerts;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;

/**
 * Provides read and write access to a communication's
 * specific alert preferences in the shared preferences file.
 *
 * @author Michael R. Falcone
 */
public class AlertPreferences {

    // CONSTANTS -----------------------------------------
    public static final String NOPREFIX_KEY_ENABLED = "PREF_ENABLE_COM_TYPE";
    public static final String NOPREFIX_KEY_ALERT_INTERVAL = "PREF_ALERT_INTERVAL";
    public static final String NOPREFIX_KEY_ALERT_DURATION = "PREF_ALERT_DURATION";
    public static final String NOPREFIX_KEY_FLASH_SCREEN_ALERT = "PREF_FLASH_SCREEN_ALERT";
    public static final String NOPREFIX_KEY_DIM_FLASH_MODE = "PREF_DIM_FLASH_MODE";
    public static final String NOPREFIX_KEY_VIBRATE_ALERT = "PREF_VIBRATE_ALERT";
    public static final String NOPREFIX_KEY_VIBRATE_STYLE = "PREF_VIBRATE_STYLE";
    public static final String NOPREFIX_KEY_AUDIO_ALERT = "PREF_AUDIO_ALERT";
    public static final String NOPREFIX_KEY_ALERT_TONE = "PREF_ALERT_TONE";
    public static final String NOPREFIX_KEY_AUDIO_ALERT_VOLUME = "PREF_AUDIO_ALERT_VOLUME";
    public static final String NOPREFIX_KEY_AUDIO_DISABLE_ON_SILENT = "PREF_AUDIO_DISABLE_ON_SILENT";
    public static final String NOPREFIX_KEY_ENABLE_SCHEDULING = "PREF_ENABLE_SCHEDULING";
    public static final String NOPREFIX_KEY_SCHEDULED_HOUR_START = "PREF_SCHEDULED_HOUR_START";
    public static final String NOPREFIX_KEY_SCHEDULED_HOUR_END = "PREF_SCHEDULED_HOUR_END";
    public static final String NOPREFIX_KEY_SCHEDULED_MINUTE_START = "PREF_SCHEDULED_MINUTE_START";
    public static final String NOPREFIX_KEY_SCHEDULED_MINUTE_END = "PREF_SCHEDULED_MINUTE_END";


    // PRIVATE MEMBER VARIABLES -----------------------------------

    public final String KEY_ENABLED;
    public final String KEY_ALERT_INTERVAL;
    public final String KEY_ALERT_DURATION;
    public final String KEY_FLASH_SCREEN_ALERT;
    public final String KEY_DIM_FLASH_MODE;
    public final String KEY_VIBRATE_ALERT;
    public final String KEY_VIBRATE_STYLE;
    public final String KEY_AUDIO_ALERT;
    public final String KEY_ALERT_TONE;
    public final String KEY_AUDIO_ALERT_VOLUME;
    public final String KEY_AUDIO_DISABLE_ON_SILENT;
    public final String KEY_ENABLE_SCHEDULING;
    public final String KEY_SCHEDULED_HOUR_START;
    public final String KEY_SCHEDULED_HOUR_END;
    public final String KEY_SCHEDULED_MINUTE_START;
    public final String KEY_SCHEDULED_MINUTE_END;


    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPreferences;
    private Resources mResources;

    private String mName;
    private String mPrefix;

    private boolean mEnabledByDefault;



    // CONSTRUCTOR -----------------------------------------

    private AlertPreferences(){

        KEY_ENABLED = "";
        KEY_ALERT_INTERVAL = "";
        KEY_ALERT_DURATION = "";
        KEY_FLASH_SCREEN_ALERT = "";
        KEY_DIM_FLASH_MODE = "";
        KEY_VIBRATE_ALERT = "";
        KEY_VIBRATE_STYLE = "";
        KEY_AUDIO_ALERT = "";
        KEY_ALERT_TONE = "";
        KEY_AUDIO_ALERT_VOLUME = "";
        KEY_AUDIO_DISABLE_ON_SILENT = "";
        KEY_ENABLE_SCHEDULING = "";
        KEY_SCHEDULED_HOUR_START = "";
        KEY_SCHEDULED_HOUR_END = "";
        KEY_SCHEDULED_MINUTE_START = "";
        KEY_SCHEDULED_MINUTE_END = "";
    }


    /**
     * Constructs a new AlertPreferences object. This should not be called
     * directly. Instead, objects of this class should be accessed through
     * an AppPreferences object.
     * @param alertName name of the alert that uses these preferences
     * @param keyPrefix prefix used to access keys in the preferences file
     * @param defaultEnabled default enabled state of the alert
     * @param appContext the application context used to load the preferences file
     */
    public AlertPreferences(String alertName, String keyPrefix, boolean defaultEnabled, Context appContext){

        mName = alertName;
        mEnabledByDefault = defaultEnabled;

        mPreferences = PreferenceManager.getDefaultSharedPreferences(appContext.getApplicationContext());
        mResources = appContext.getResources();
        mEditor = mPreferences.edit();

        mPrefix = keyPrefix;

        KEY_ENABLED = keyPrefix + NOPREFIX_KEY_ENABLED;
        KEY_ALERT_INTERVAL = keyPrefix + NOPREFIX_KEY_ALERT_INTERVAL;
        KEY_ALERT_DURATION = keyPrefix + NOPREFIX_KEY_ALERT_DURATION;
        KEY_FLASH_SCREEN_ALERT = keyPrefix + NOPREFIX_KEY_FLASH_SCREEN_ALERT;
        KEY_DIM_FLASH_MODE = keyPrefix + NOPREFIX_KEY_DIM_FLASH_MODE;
        KEY_VIBRATE_ALERT = keyPrefix + NOPREFIX_KEY_VIBRATE_ALERT;
        KEY_VIBRATE_STYLE = keyPrefix + NOPREFIX_KEY_VIBRATE_STYLE;
        KEY_AUDIO_ALERT = keyPrefix + NOPREFIX_KEY_AUDIO_ALERT;
        KEY_ALERT_TONE = keyPrefix + NOPREFIX_KEY_ALERT_TONE;
        KEY_AUDIO_ALERT_VOLUME = keyPrefix + NOPREFIX_KEY_AUDIO_ALERT_VOLUME;
        KEY_AUDIO_DISABLE_ON_SILENT = keyPrefix + NOPREFIX_KEY_AUDIO_DISABLE_ON_SILENT;
        KEY_ENABLE_SCHEDULING = keyPrefix + NOPREFIX_KEY_ENABLE_SCHEDULING;
        KEY_SCHEDULED_HOUR_START = keyPrefix + NOPREFIX_KEY_SCHEDULED_HOUR_START;
        KEY_SCHEDULED_HOUR_END = keyPrefix + NOPREFIX_KEY_SCHEDULED_HOUR_END;
        KEY_SCHEDULED_MINUTE_START = keyPrefix + NOPREFIX_KEY_SCHEDULED_MINUTE_START;
        KEY_SCHEDULED_MINUTE_END = keyPrefix + NOPREFIX_KEY_SCHEDULED_MINUTE_END;


        // fix for ringtone chooser on certain devices
        if (mPreferences.getString(KEY_ALERT_TONE, null) == null) {

            mEditor.putString(KEY_ALERT_TONE, "");
            mEditor.commit();
        }
    }






    // PUBLIC METHODS -----------------------------------------

    /**
     * Resets all preferences to application defaults.
     */
    public void resetToDefaults(){

        mEditor.putBoolean(KEY_ENABLED, mEnabledByDefault);
        mEditor.putString(KEY_ALERT_INTERVAL, mResources.getString(R.string.pref_default_alertinterval));
        mEditor.putString(KEY_ALERT_DURATION, mResources.getString(R.string.pref_default_alertduration));
        mEditor.putBoolean(KEY_FLASH_SCREEN_ALERT, mResources.getBoolean(R.bool.pref_default_flashscreenalert));
        mEditor.putBoolean(KEY_DIM_FLASH_MODE, mResources.getBoolean(R.bool.pref_default_dimflashmode));
        mEditor.putBoolean(KEY_VIBRATE_ALERT, mResources.getBoolean(R.bool.pref_default_vibratealert));
        mEditor.putString(KEY_VIBRATE_STYLE, mResources.getString(R.string.pref_default_vibratestyle));
        mEditor.putBoolean(KEY_AUDIO_ALERT, mResources.getBoolean(R.bool.pref_default_audioalert));
        mEditor.putString(KEY_ALERT_TONE, mResources.getString(R.string.pref_default_alerttone));
        mEditor.putInt(KEY_AUDIO_ALERT_VOLUME, mResources.getInteger(R.integer.pref_default_audioalertvolume));
        mEditor.putBoolean(KEY_AUDIO_DISABLE_ON_SILENT, mResources.getBoolean(R.bool.pref_default_audiodisableonsilent));
        mEditor.putBoolean(KEY_ENABLE_SCHEDULING, mResources.getBoolean(R.bool.pref_default_enablescheduling));
        mEditor.putInt(KEY_SCHEDULED_HOUR_START, mResources.getInteger(R.integer.pref_default_scheduledhourstart));
        mEditor.putInt(KEY_SCHEDULED_HOUR_END, mResources.getInteger(R.integer.pref_default_scheduledhourend));
        mEditor.putInt(KEY_SCHEDULED_MINUTE_START, mResources.getInteger(R.integer.pref_default_scheduledminutestart));
        mEditor.putInt(KEY_SCHEDULED_MINUTE_END, mResources.getInteger(R.integer.pref_default_scheduledminuteend));
        mEditor.commit();
    }


    /**
     * Returns the prefix for storing preferences in the shared preferences file.
     */
    public String getPrefix(){
        
        return mPrefix;
    }


    /**
     * Returns the name of the alert.
     */
    public String getAlertName(){

        return mName;
    }


    /**
     * Returns whether alerts are currently enabled for this communication type.
     */
    public boolean getEnabled(){

        return mPreferences.getBoolean(KEY_ENABLED, mEnabledByDefault);
    }


    /**
     * Sets whether alerts are currently enabled for this communication type.
     */
    public void setEnabled(boolean enabled){

        mEditor.putBoolean(KEY_ENABLED, enabled);
        mEditor.commit();
    }


    /**
     * Returns the alert interval for this communication type.
     */
    public String getInterval(){

        return mPreferences.getString(KEY_ALERT_INTERVAL, mResources.getString(R.string.pref_default_alertinterval));
    }


    /**
     * Sets the alert interval for this communication type.
     */
    public void setInterval(String interval){

        mEditor.putString(KEY_ALERT_INTERVAL, interval);
        mEditor.commit();
    }


    /**
     * Returns the alert duration for this communication type.
     */
    public String getDuration(){

        return mPreferences.getString(KEY_ALERT_DURATION, mResources.getString(R.string.pref_default_alertduration));
    }


    /**
     * Sets the alert duration for this communication type.
     */
    public void setDuration(String duration){

        mEditor.putString(KEY_ALERT_DURATION, duration);
        mEditor.commit();
    }


    /**
     * Returns whether the flash screen alert is enabled for this communication type.
     */
    public boolean getFlashScreenEnabled(){

        return mPreferences.getBoolean(KEY_FLASH_SCREEN_ALERT, mResources.getBoolean(R.bool.pref_default_flashscreenalert));
    }


    /**
     * Sets whether the flash screen alert is enabled for this communication type.
     */
    public void setFlashScreenEnabled(boolean enabled){

        mEditor.putBoolean(KEY_FLASH_SCREEN_ALERT, enabled);
        mEditor.commit();
    }


    /**
     * Returns whether dim flash mode is enabled for this communication type. Used
     * if flash screen is enabled.
     */
    public boolean getDimFlashEnabled(){

        return mPreferences.getBoolean(KEY_DIM_FLASH_MODE, mResources.getBoolean(R.bool.pref_default_dimflashmode));
    }


    /**
     * Sets whether dim flash mode is enabled for this communication type. Used
     * if flash screen is enabled.
     */
    public void setDimFlashEnabled(boolean enabled){

        mEditor.putBoolean(KEY_DIM_FLASH_MODE, enabled);
        mEditor.commit();
    }


    /**
     * Returns whether the vibrate alert is enabled for this communication type.
     */
    public boolean getVibrateEnabled(){

        return mPreferences.getBoolean(KEY_VIBRATE_ALERT, mResources.getBoolean(R.bool.pref_default_vibratealert));
    }


    /**
     * Sets whether the vibrate alert is enabled for this communication type.
     */
    public void setVibrateEnabled(boolean enabled){

        mEditor.putBoolean(KEY_VIBRATE_ALERT, enabled);
        mEditor.commit();
    }


    /**
     * Returns the vibration style for this communication type. Used if vibration
     * is enabled.
     */
    public String getVibrateStyle(){

        return mPreferences.getString(KEY_VIBRATE_STYLE, mResources.getString(R.string.pref_default_vibratestyle));
    }


    /**
     * Sets the vibration for this communication type. Used if vibration
     * is enabled.
     */
    public void setVibrateStyle(String style){

        mEditor.putString(KEY_VIBRATE_STYLE, style);
        mEditor.commit();
    }


    /**
     * Returns whether the audio alert is enabled for this communication type.
     */
    public boolean getAudioEnabled(){

        return mPreferences.getBoolean(KEY_AUDIO_ALERT, mResources.getBoolean(R.bool.pref_default_audioalert));
    }


    /**
     * Sets whether the audio alert is enabled for this communication type.
     */
    public void setAudioEnabled(boolean enabled){

        mEditor.putBoolean(KEY_AUDIO_ALERT, enabled);
        mEditor.commit();
    }


    /**
     * Returns the alert tone style for this communication type. Used if audio alert
     * is enabled.
     */
    public String getAlertTone(){

        return mPreferences.getString(KEY_ALERT_TONE, mResources.getString(R.string.pref_default_alerttone));
    }


    /**
     * Sets the alert tone for this communication type. Used if audio alert
     * is enabled.
     */
    public void setAlertTone(String uri){

        mEditor.putString(KEY_ALERT_TONE, uri);
        mEditor.commit();
    }


    /**
     * Returns the alert volume for this communication type, between 0-100. Used if audio alert
     * is enabled.
     */
    public int getAlertVolume(){

        return mPreferences.getInt(KEY_AUDIO_ALERT_VOLUME, mResources.getInteger(R.integer.pref_default_audioalertvolume));
    }


    /**
     * Sets the alert volume for this communication type, between 0-100. Used if audio alert
     * is enabled.
     */
    public void setAlertVolume(int volume){

        mEditor.putInt(KEY_AUDIO_ALERT_VOLUME, volume);
        mEditor.commit();
    }


    /**
     * Returns whether the audio alert is disabled while the phone is in silent mode.
     * Used when audio alert is enabled.
     */
    public boolean getAudioDisabledOnSilent(){

        return mPreferences.getBoolean(KEY_AUDIO_DISABLE_ON_SILENT, mResources.getBoolean(R.bool.pref_default_audiodisableonsilent));
    }


    /**
     * Sets whether the audio alert is disabled while the phone is in silent mode.
     * Used when audio alert is enabled.
     */
    public void setAudioDisabledOnSilent(boolean disabled){

        mEditor.putBoolean(KEY_AUDIO_DISABLE_ON_SILENT, disabled);
        mEditor.commit();
    }


    /**
     * Gets whether scheduling options are enabled for this communication type.
     */
    public boolean getSchedulingEnabled(){

        return mPreferences.getBoolean(KEY_ENABLE_SCHEDULING, mResources.getBoolean(R.bool.pref_default_enablescheduling));
    }


    /**
     * Sets whether scheduling options are enabled for this communication type.
     */
    public void setSchedulingEnabled(boolean enabled){

        mEditor.putBoolean(KEY_ENABLE_SCHEDULING, enabled);
        mEditor.commit();
    }


    /**
     * Gets the hour that scheduling options begin to take effect.
     */
    public int getSchedulingHourStart(){

        return mPreferences.getInt(KEY_SCHEDULED_HOUR_START, mResources.getInteger(R.integer.pref_default_scheduledhourstart));
    }


    /**
     * Sets the hour that scheduling options begin to take effect.
     */
    public void setSchedulingHourStart(int hour){

        mEditor.putInt(KEY_SCHEDULED_HOUR_START, hour);
        mEditor.commit();
    }


    /**
     * Gets the minute that scheduling options begin to take effect.
     */
    public int getSchedulingMinuteStart(){

        return mPreferences.getInt(KEY_SCHEDULED_MINUTE_START, mResources.getInteger(R.integer.pref_default_scheduledminutestart));
    }


    /**
     * Sets the minute that scheduling options begin to take effect.
     */
    public void setSchedulingMinuteStart(int minute){

        mEditor.putInt(KEY_SCHEDULED_MINUTE_START, minute);
        mEditor.commit();
    }


    /**
     * Gets the hour that scheduling options stop taking effect.
     */
    public int getSchedulingHourEnd(){

        return mPreferences.getInt(KEY_SCHEDULED_HOUR_END, mResources.getInteger(R.integer.pref_default_scheduledhourend));
    }


    /**
     * Sets the hour that scheduling options stop taking effect.
     */
    public void setSchedulingHourEnd(int hour){

        mEditor.putInt(KEY_SCHEDULED_HOUR_END, hour);
        mEditor.commit();
    }


    /**
     * Gets the minute that scheduling options stop taking effect.
     */
    public int getSchedulingMinuteEnd(){

        return mPreferences.getInt(KEY_SCHEDULED_MINUTE_END, mResources.getInteger(R.integer.pref_default_scheduledminuteend));
    }


    /**
     * Sets the minute that scheduling options stop taking effect.
     */
    public void setSchedulingMinuteEnd(int minute){

        mEditor.putInt(KEY_SCHEDULED_MINUTE_END, minute);
        mEditor.commit();
    }


    /**
     * Creates a String representation of the scheduling options start time.
     */
    public String getSchedulingStartTimeString(){
        
        int hour = getSchedulingHourStart();
        
        String time = "";

        String hour_str;
        
        if(hour > 12)
            hour_str = String.valueOf(hour % 12);
        else if(hour == 0)
            hour_str = "12";
        else
            hour_str = String.valueOf(hour);

        String minute_str = String.valueOf(getSchedulingMinuteStart());
        
        if(hour_str.length() < 2)
            time = "0";
        
        time += hour_str + ":";

        if(minute_str.length() < 2)
            time += "0";

        time += minute_str;

        if(hour >= 12)
            time += " PM";
        else
            time += " AM";
        
        return time;
    }


    /**
     * Creates a String representation of the scheduling options end time.
     */
    public String getSchedulingEndTimeString(){

        int hour = getSchedulingHourEnd();

        String time = "";

        String hour_str;

        if(hour > 12)
            hour_str = String.valueOf(hour % 12);
        else if(hour == 0)
            hour_str = "12";
        else
            hour_str = String.valueOf(hour);

        String minute_str = String.valueOf(getSchedulingMinuteEnd());

        if(hour_str.length() < 2)
            time = "0";

        time += hour_str + ":";

        if(minute_str.length() < 2)
            time += "0";

        time += minute_str;

        if(hour >= 12)
            time += " PM";
        else
            time += " AM";

        return time;
    }
}
