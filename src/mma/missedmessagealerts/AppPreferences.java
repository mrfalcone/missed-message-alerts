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
 * Provides read and write access to the application's
 * specific preferences in the shared preferences file.
 *
 * @author Michael R. Falcone
 */
public class AppPreferences {


    // CONSTANTS -----------------------------------------
    public static final String PREFIX_TEXT = "TEXT";
    public static final String PREFIX_CALLS = "MISSEDCALLS";
    public static final String PREFIX_VOICEMAIL = "VOICEMAIL";

    public static final String KEY_ENABLE_ALERTS = "PREF_ENABLE_ALERTS";
    public static final String KEY_DISABLE_ON_BATTERY = "PREF_DISABLE_ON_BATTERY";
    public static final String KEY_LOW_BATTERY_PERCENTAGE = "PREF_LOW_BATTERY_PERCENTAGE";
    public static final String KEY_SHOW_NOTIFICATION = "PREF_SHOW_NOTIFICATION";

    private static final String ALERTNAME_TEXT = "Text Alerts";
    private static final String ALERTNAME_CALLS = "Missed Call Alerts";
    private static final String ALERTNAME_VOICEMAIL = "Voice Mail Alerts";





    // PRIVATE MEMBER VARIABLES -----------------------------------

    private SharedPreferences.Editor mEditor;
    private SharedPreferences mPreferences;
    private Resources mResources;

    private AlertPreferences mTextAlertPreferences;
    private AlertPreferences mCallsAlertPreferences;
    private AlertPreferences mVoicemailAlertPreferences;



    // CONSTRUCTOR -----------------------------------------

    private AppPreferences(){}

    /**
     * Constructs a new AppPreferences object.
     * @param appContext the application context used to load the preferences file
     */
    public AppPreferences(Context appContext){

        mPreferences = PreferenceManager.getDefaultSharedPreferences(appContext.getApplicationContext());
        mResources = appContext.getResources();
        mEditor = mPreferences.edit();

        mTextAlertPreferences = new AlertPreferences(ALERTNAME_TEXT, PREFIX_TEXT,
                mResources.getBoolean(R.bool.pref_default_textmessages), appContext);
        mCallsAlertPreferences = new AlertPreferences(ALERTNAME_CALLS, PREFIX_CALLS,
                mResources.getBoolean(R.bool.pref_default_missedcalls), appContext);
        mVoicemailAlertPreferences = new AlertPreferences(ALERTNAME_VOICEMAIL, PREFIX_VOICEMAIL,
                mResources.getBoolean(R.bool.pref_default_voicemail), appContext);
    }




    // PUBLIC METHODS -----------------------------------------

    /**
     * Resets all preferences, including alerts, to application defaults.
     */
    public void resetToDefaults(){

        mEditor.putBoolean(KEY_ENABLE_ALERTS, mResources.getBoolean(R.bool.pref_default_enablealerts));
        mEditor.putBoolean(KEY_DISABLE_ON_BATTERY, mResources.getBoolean(R.bool.pref_default_disableonbattery));
        mEditor.putBoolean(KEY_SHOW_NOTIFICATION, mResources.getBoolean(R.bool.pref_default_shownotification));
        mEditor.putInt(KEY_LOW_BATTERY_PERCENTAGE, mResources.getInteger(R.integer.pref_default_lowbatterypercentage));
        mEditor.commit();

        mTextAlertPreferences.resetToDefaults();
        mCallsAlertPreferences.resetToDefaults();
        mVoicemailAlertPreferences.resetToDefaults();
    }


    /**
     * Gets an AlertPreferences object for text alerts.
     */
    public AlertPreferences getTextAlertPreferences(){
        
        return mTextAlertPreferences;
    }


    /**
     * Gets an AlertPreferences object for missed call alerts.
     */
    public AlertPreferences getMissedCallAlertPreferences(){

        return mCallsAlertPreferences;
    }


    /**
     * Gets an AlertPreferences object for voicemail alerts.
     */
    public AlertPreferences getVoiceMailAlertPreferences(){

        return mVoicemailAlertPreferences;
    }

    
    /**
     * Returns whether alerts are currently enabled.
            */
    public boolean getAlertsEnabled(){

        return mPreferences.getBoolean(KEY_ENABLE_ALERTS, mResources.getBoolean(R.bool.pref_default_enablealerts));
    }


    /**
     * Sets whether alerts are currently enabled.
     */
    public void setAlertsEnabled(boolean enabled){

        mEditor.putBoolean(KEY_ENABLE_ALERTS, enabled);
        mEditor.commit();
    }


    /**
     * Returns whether to disable on low battery.
     */
    public boolean getDisableOnLowBattery(){

        return mPreferences.getBoolean(KEY_DISABLE_ON_BATTERY, mResources.getBoolean(R.bool.pref_default_disableonbattery));
    }


    /**
     * Sets whether to disable on low battery.
     */
    public void setDisableOnLowBattery(boolean disable){

        mEditor.putBoolean(KEY_DISABLE_ON_BATTERY, disable);
        mEditor.commit();
    }


    /**
     * Returns whether status bar notification is enabled.
     */
    public boolean getNotificationEnabled(){

        return mPreferences.getBoolean(KEY_SHOW_NOTIFICATION, mResources.getBoolean(R.bool.pref_default_shownotification));
    }


    /**
     * Sets whether status bar notification is enabled.
     */
    public void setNotificationEnabled(boolean enabled){

        mEditor.putBoolean(KEY_SHOW_NOTIFICATION, enabled);
        mEditor.commit();
    }


    /**
     * Returns low battery percentage.
     */
    public int getLowBatteryPercentage(){

        return mPreferences.getInt(KEY_LOW_BATTERY_PERCENTAGE, mResources.getInteger(R.integer.pref_default_lowbatterypercentage));
    }


    /**
     * Sets low battery percentage.
     */
    public void setLowBatteryPercentage(int percentage){

        mEditor.putInt(KEY_LOW_BATTERY_PERCENTAGE, percentage);
        mEditor.commit();
    }

}
