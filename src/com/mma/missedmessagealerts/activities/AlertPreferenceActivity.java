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

package com.mma.missedmessagealerts.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.preference.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TimePicker;
import com.mma.missedmessagealerts.AlertPreferences;
import com.mma.missedmessagealerts.AppPreferences;
import com.mma.missedmessagealerts.R;
import com.mma.missedmessagealerts.services.AlerterService;
import com.mma.seekbarpreference.DefinedSeekbarPreference;

/**
 * Preference activity for alerts. Allows the user to set behavior
 * for alerting for a specific communication type.
 *
 * @author Michael R. Falcone
 */
public class AlertPreferenceActivity extends PreferenceActivity {



    // CONSTANTS ---------------------------------------

    /**
     * Extra used to specify the communication type for which
     * to edit alert preferences.
     */
    public static final String EXTRA_COM_TYPE = "ComType";

    /**
     * Value of the <code>EXTRA_COM_TYPE</code> extra to specify
     * editing of alerts for text messages.
     */
    public static final int COM_TYPE_TEXT = 0;

    /**
     * Value of the <code>EXTRA_COM_TYPE</code> extra to specify
     * editing of alerts for missed calls.
     */
    public static final int COM_TYPE_MISSEDCALL = 1;

    /**
     * Value of the <code>EXTRA_COM_TYPE</code> extra to specify
     * editing of alerts for voicemail.
     */
    public static final int COM_TYPE_VOICEMAIL = 2;



    
    
    // MEMBER VARIABLES ---------------------------------------
    private static final int START_TIME_DIALOG_ID = 0;
    private static final int END_TIME_DIALOG_ID = 1;

    private AlertPreferences mAlertPrefs;

    private static final String KEY_AUDIO_PREVIEW = "PREF_AUDIO_PREVIEW";
    private static final String KEY_PICK_SCHEDULE_START = "PREF_PICK_SCHEDULE_START";
    private static final String KEY_PICK_SCHEDULE_END = "PREF_PICK_SCHEDULE_END";

    private static final int MENU_RESET = Menu.FIRST;

    private MediaPlayer mPreviewPlayer;

    private CheckBoxPreference mEnableComtypeCheckbox;
    private CheckBoxPreference mFlashScreenAlertCheckbox;
    private CheckBoxPreference mVibrateAlertCheckbox;
    private CheckBoxPreference mAudioAlertCheckbox;
    private CheckBoxPreference mDimFlashModeCheckbox;
    private CheckBoxPreference mAudioDisableOnSilentCheckbox;
    private CheckBoxPreference mEnableSchedulingCheckbox;
    private ListPreference mAlertIntervalList;
    private DefinedSeekbarPreference mAudioVolumeSeekbar;
    private ListPreference mAlertDurationList;
    private RingtonePreference mAlertTonePreference;
    private ListPreference mVibrateStyleList;
    private Preference mPreviewPreference;
    private Preference mPickScheduleStartPreference;
    private Preference mPickScheduleEndPreference;




    // OVERRIDE METHODS -------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.alert_prefs);

        AppPreferences appPrefs = new AppPreferences(this);

        switch(getIntent().getIntExtra(EXTRA_COM_TYPE, -1)){
            
            case COM_TYPE_TEXT:
                mAlertPrefs = appPrefs.getTextAlertPreferences();
                break;
            
            case COM_TYPE_MISSEDCALL:
                mAlertPrefs = appPrefs.getMissedCallAlertPreferences();
                break;
            
            case COM_TYPE_VOICEMAIL:
                mAlertPrefs = appPrefs.getVoiceMailAlertPreferences();
                break;
            
            default:
                throw new RuntimeException("No communication type specified.");
        }


        mEnableComtypeCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_ENABLED);
        mFlashScreenAlertCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_FLASH_SCREEN_ALERT);
        mVibrateAlertCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_VIBRATE_ALERT);
        mAudioAlertCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_AUDIO_ALERT);
        mDimFlashModeCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_DIM_FLASH_MODE);
        mAudioDisableOnSilentCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_AUDIO_DISABLE_ON_SILENT);
        mEnableSchedulingCheckbox = (CheckBoxPreference) findPreference(AlertPreferences.NOPREFIX_KEY_ENABLE_SCHEDULING);
        mAlertIntervalList = (ListPreference) findPreference(AlertPreferences.NOPREFIX_KEY_ALERT_INTERVAL);
        mAudioVolumeSeekbar = (DefinedSeekbarPreference) findPreference(AlertPreferences.NOPREFIX_KEY_AUDIO_ALERT_VOLUME);
        mAlertDurationList = (ListPreference) findPreference(AlertPreferences.NOPREFIX_KEY_ALERT_DURATION);
        mAlertTonePreference = (RingtonePreference)findPreference(AlertPreferences.NOPREFIX_KEY_ALERT_TONE);
        mVibrateStyleList = (ListPreference) findPreference(AlertPreferences.NOPREFIX_KEY_VIBRATE_STYLE);
        mPreviewPreference = findPreference(KEY_AUDIO_PREVIEW);
        mPickScheduleStartPreference = findPreference(KEY_PICK_SCHEDULE_START);
        mPickScheduleEndPreference = findPreference(KEY_PICK_SCHEDULE_END);


        restorePreferenceStates();
        initPreferences();
        updateEnabledViews();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem reset_item = menu.add(0, MENU_RESET, Menu.NONE, R.string.pref_menu_reset);
        reset_item.setIcon(android.R.drawable.ic_menu_revert);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_RESET) {

            resetToDefaultValues();
            return true;
        }

        return false;
    }


    @Override
    protected Dialog onCreateDialog(int id) {


        if (id == START_TIME_DIALOG_ID) {

            return new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int hour, int minute) {

                    mAlertPrefs.setSchedulingHourStart(hour);
                    mAlertPrefs.setSchedulingMinuteStart(minute);
                    updateSchedulingStartEndTimes();
                }
            }, mAlertPrefs.getSchedulingHourStart(), mAlertPrefs.getSchedulingMinuteStart(), false);
        }
        
        else if(id == END_TIME_DIALOG_ID){

            return new TimePickerDialog(this, new TimePickerDialog.OnTimeSetListener() {
                @Override
                public void onTimeSet(TimePicker timePicker, int hour, int minute) {

                    mAlertPrefs.setSchedulingHourEnd(hour);
                    mAlertPrefs.setSchedulingMinuteEnd(minute);
                    updateSchedulingStartEndTimes();
                }
            }, mAlertPrefs.getSchedulingHourEnd(), mAlertPrefs.getSchedulingMinuteEnd(), false);
        }

        return null;
    }
    
    

    // PRIVATE METHODS -------------------------------------

    private void resetToDefaultValues() {

        mAlertPrefs.resetToDefaults();
        restorePreferenceStates();
        updateEnabledViews();
    }


    private void updateEnabledViews() {

        if (mEnableComtypeCheckbox.isChecked())
            enableViews();
        else
            disableViews();
    }


    private void disableViews() {

        mDimFlashModeCheckbox.setEnabled(false);
        mAlertIntervalList.setEnabled(false);
        mAlertDurationList.setEnabled(false);
        mFlashScreenAlertCheckbox.setEnabled(false);
        mVibrateAlertCheckbox.setEnabled(false);
        mVibrateStyleList.setEnabled(false);
        mAudioAlertCheckbox.setEnabled(false);
        mAlertTonePreference.setEnabled(false);
        mAudioDisableOnSilentCheckbox.setEnabled(false);
        mAudioVolumeSeekbar.setEnabled(false);
        mPreviewPreference.setEnabled(false);
        mEnableSchedulingCheckbox.setEnabled(false);
        mPickScheduleStartPreference.setEnabled(false);
        mPickScheduleEndPreference.setEnabled(false);
    }

    private void enableViews() {

        mAlertIntervalList.setEnabled(true);
        mAlertDurationList.setEnabled(true);
        mFlashScreenAlertCheckbox.setEnabled(true);
        mVibrateAlertCheckbox.setEnabled(true);
        mVibrateStyleList.setEnabled(mVibrateAlertCheckbox.isChecked());
        mAudioAlertCheckbox.setEnabled(true);
        mAlertTonePreference.setEnabled(mAudioAlertCheckbox.isChecked());
        mDimFlashModeCheckbox.setEnabled(mFlashScreenAlertCheckbox.isChecked());
        mAudioDisableOnSilentCheckbox.setEnabled(mAudioAlertCheckbox.isChecked());
        mAudioVolumeSeekbar.setEnabled(mAudioAlertCheckbox.isChecked());
        mPreviewPreference.setEnabled(mAudioAlertCheckbox.isChecked());
        mEnableSchedulingCheckbox.setEnabled(true);
        mPickScheduleStartPreference.setEnabled(mEnableSchedulingCheckbox.isChecked());
        mPickScheduleEndPreference.setEnabled(mEnableSchedulingCheckbox.isChecked());
    }


    private void restorePreferenceStates(){

        mEnableComtypeCheckbox.setChecked(mAlertPrefs.getEnabled());
        mFlashScreenAlertCheckbox.setChecked(mAlertPrefs.getFlashScreenEnabled());
        mVibrateAlertCheckbox.setChecked(mAlertPrefs.getVibrateEnabled());
        mAudioAlertCheckbox.setChecked(mAlertPrefs.getAudioEnabled());
        mDimFlashModeCheckbox.setChecked(mAlertPrefs.getDimFlashEnabled());
        mAudioDisableOnSilentCheckbox.setChecked(mAlertPrefs.getAudioDisabledOnSilent());
        mAlertIntervalList.setValue(mAlertPrefs.getInterval());
        mAudioVolumeSeekbar.setProgress(mAlertPrefs.getAlertVolume());
        mAlertDurationList.setValue(mAlertPrefs.getDuration());
        mVibrateStyleList.setValue(mAlertPrefs.getVibrateStyle());
        mEnableSchedulingCheckbox.setChecked(mAlertPrefs.getSchedulingEnabled());

        mAlertTonePreference.setKey(mAlertPrefs.KEY_ALERT_TONE);

        updateSchedulingStartEndTimes();
    }


    private void initPreferences(){

        setTitle(getTitle() + " " + mAlertPrefs.getAlertName());

        mEnableComtypeCheckbox.setTitle(mEnableComtypeCheckbox.getTitle() + " " + mAlertPrefs.getAlertName());
        mEnableComtypeCheckbox.setSummary(mEnableComtypeCheckbox.getSummary() + " " + mAlertPrefs.getAlertName());

        mPreviewPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {

                String uri_str = mAlertPrefs.getAlertTone();


                if(mPreviewPlayer == null){

                    if (uri_str.length() != 0) {

                        float volume = mAlertPrefs.getAlertVolume() / 100.0f;

                        try {
                            mPreviewPlayer = new MediaPlayer();
                            mPreviewPlayer.setDataSource(AlertPreferenceActivity.this, Uri.parse(uri_str));

                            mPreviewPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mediaPlayer) {
                                    mPreviewPlayer.release();
                                    mPreviewPlayer = null;
                                    mPreviewPreference.setTitle(R.string.pref_title_audiopreviewplay);
                                    mPreviewPreference.setSummary(R.string.pref_sum_audiopreviewplay);
                                }
                            });

                            if (volume < 0.9f)
                                mPreviewPlayer.setVolume(volume, volume);

                            mPreviewPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                            mPreviewPlayer.prepare();
                            mPreviewPlayer.start();

                            mPreviewPreference.setTitle(R.string.pref_title_audiopreviewstop);
                            mPreviewPreference.setSummary(R.string.pref_sum_audiopreviewstop);
                        }
                        catch (Exception e){
                            Log.e("AlertPreferenceActivity","Error initializing preview player: " + e.getMessage());
                        }
                    }
                }

                else{

                    mPreviewPlayer.stop();
                    mPreviewPlayer.release();
                    mPreviewPlayer = null;
                    mPreviewPreference.setTitle(R.string.pref_title_audiopreviewplay);
                    mPreviewPreference.setSummary(R.string.pref_sum_audiopreviewplay);
                }

                return true;
            }
        });



        mEnableSchedulingCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mAlertPrefs.setSchedulingEnabled(value);

                mPickScheduleStartPreference.setEnabled(value);
                mPickScheduleEndPreference.setEnabled(value);

                return true;
            }
        });


        
        mPickScheduleStartPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                showDialog(START_TIME_DIALOG_ID);
                return true;
            }
        });

        mPickScheduleEndPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                showDialog(END_TIME_DIALOG_ID);
                return true;
            }
        });

        
        mFlashScreenAlertCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mDimFlashModeCheckbox.setEnabled(value);
                mAlertPrefs.setFlashScreenEnabled(value);
                return true;
            }
        });

        mVibrateAlertCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean)newValue;
                mAlertPrefs.setVibrateEnabled(value);

                mVibrateStyleList.setEnabled(value);
                return true;
            }
        });

        mAudioAlertCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mAlertPrefs.setAudioEnabled(value);

                mAlertTonePreference.setEnabled(value);
                mAudioDisableOnSilentCheckbox.setEnabled(value);
                mAudioVolumeSeekbar.setEnabled(value);

                if(mPreviewPlayer != null){

                    mPreviewPlayer.stop();
                    mPreviewPlayer.release();
                    mPreviewPlayer = null;
                    mPreviewPreference.setTitle(R.string.pref_title_audiopreviewplay);
                    mPreviewPreference.setSummary(R.string.pref_sum_audiopreviewplay);
                }

                mPreviewPreference.setEnabled(value);

                return true;
            }
        });

        mEnableComtypeCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mAlertPrefs.setEnabled(value);

                if (value) {

                    enableViews();
                } else {

                    disableViews();
                }

                return true;
            }
        });

        mDimFlashModeCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean)newValue;
                mAlertPrefs.setDimFlashEnabled(value);
                return true;
            }
        });


        mAudioDisableOnSilentCheckbox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean)newValue;
                mAlertPrefs.setAudioDisabledOnSilent(value);
                return true;
            }
        });

        mAlertIntervalList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                AlertDialog a = new AlertDialog.Builder(AlertPreferenceActivity.this).create();
                int interval = Integer.parseInt((String) newValue);
                int duration = Integer.parseInt(mAlertPrefs.getDuration());

                mAlertPrefs.setInterval((String) newValue);

                // alert user if alert duration is less than alert interval
                if(interval > duration && duration != 0){

                    a.setMessage(getString(R.string.pref_high_interval_message));
                    a.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.pref_message_button_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    a.show();
                }

                return true;
            }
        });


        mAudioVolumeSeekbar.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {


                int value = (Integer)newValue;
                mAlertPrefs.setAlertVolume(value);

                if(mPreviewPlayer != null){

                    float volume = value / 100.0f;

                    if (volume < 0.9f)
                        mPreviewPlayer.setVolume(volume, volume);
                    else
                        mPreviewPlayer.setVolume(1.0f, 1.0f);
                }

                return true;
            }
        });

        mAlertDurationList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                AlertDialog a = new AlertDialog.Builder(AlertPreferenceActivity.this).create();
                int duration = Integer.parseInt((String) newValue);
                mAlertPrefs.setDuration((String) newValue);

                // alert user upon choosing unlimited alert duration
                if (duration == 0) {

                    a.setMessage(getString(R.string.pref_duration_message));
                    a.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.pref_message_button_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    a.show();
                }

                // alert user if alert duration is less than alert interval
                else if(duration < Integer.parseInt(mAlertPrefs.getInterval())){

                    a.setMessage(getString(R.string.pref_high_interval_message));
                    a.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.pref_message_button_ok),
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            });
                    a.show();
                }

                return true;
            }
        });

        mAlertTonePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                String value = (String)newValue;
                mAlertPrefs.setAlertTone(value);
                return true;
            }
        });

        mVibrateStyleList.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                long[] pattern;

                switch (Integer.parseInt((String) newValue)){

                    case 0: pattern = AlerterService.VIBRATE_PATTERN_0;
                        break;
                    case 1: pattern = AlerterService.VIBRATE_PATTERN_1;
                        break;
                    case 2: pattern = AlerterService.VIBRATE_PATTERN_2;
                        break;
                    case 3: pattern = AlerterService.VIBRATE_PATTERN_3;
                        break;
                    case 4: pattern = AlerterService.VIBRATE_PATTERN_4;
                        break;
                    default:
                        pattern = AlerterService.VIBRATE_PATTERN_0;
                }

                try {
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    v.vibrate(pattern, -1);
                } catch (Exception e) {
                    Log.e("AlertPreferenceActivity", "Vibrator error: " + e.getMessage());
                }

                mAlertPrefs.setVibrateStyle((String) newValue);

                return true;
            }
        });
    }


    private void updateSchedulingStartEndTimes(){

        mPickScheduleStartPreference.setSummary(getString(R.string.pref_sum_pickschedulestart) + " " + mAlertPrefs.getSchedulingStartTimeString());
        mPickScheduleEndPreference.setSummary(getString(R.string.pref_sum_pickscheduleend) + " " + mAlertPrefs.getSchedulingEndTimeString());
    }
}
