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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.mma.missedmessagealerts.AppPreferences;
import com.mma.missedmessagealerts.R;
import com.mma.missedmessagealerts.services.MissedMessageListenerService;
import com.mma.seekbarpreference.DefinedSeekbarPreference;

/**
 * Main activity that user interacts with. Provides user with
 * preference options and control over the way other services
 * behave.
 *
 * @author Michael R. Falcone
 */
public class MessageAlertsPreferenceActivity extends PreferenceActivity {


    // MEMBER VARIABLES ---------------------------------------
    private static final String KEY_EDIT_TEXT_ALERT_PREFERENCE = "PREF_EDIT_TEXT_ALERT";
    private static final String KEY_EDIT_MISSEDCALL_ALERT_PREFERENCE = "PREF_EDIT_MISSEDCALL_ALERT";
    private static final String KEY_EDIT_VOICEMAIL_ALERT_PREFERENCE = "PREF_EDIT_VOICEMAIL_ALERT";
    
    private static final String KEY_WHATS_NEW_VERSION = "PREF_WHATS_NEW_VERSION";


    private static final int MENU_RESET = Menu.FIRST;
    private static final int MENU_ABOUT = Menu.FIRST + 1;
    private static final int MENU_WHATSNEW = Menu.FIRST + 2;

    private CheckBoxPreference mShowNotificationCheckbox;
    private CheckBoxPreference mEnableAlertsCheckbox;
    private CheckBoxPreference mDisableOnBatteryCheckbox;
    private DefinedSeekbarPreference mLowBatteryPercentageSeekbar;
    private Preference mEditTextAlertPreference;
    private Preference mEditMissedCallAlertPreference;
    private Preference mEditVoicemailAlertPreference;
    
    private AppPreferences mPreferences;


    // OVERRIDE METHODS -------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.main_prefs);


        mShowNotificationCheckbox = (CheckBoxPreference) findPreference(AppPreferences.KEY_SHOW_NOTIFICATION);
        mEnableAlertsCheckbox = (CheckBoxPreference) findPreference(AppPreferences.KEY_ENABLE_ALERTS);
        mDisableOnBatteryCheckbox = (CheckBoxPreference) findPreference(AppPreferences.KEY_DISABLE_ON_BATTERY);
        mLowBatteryPercentageSeekbar = (DefinedSeekbarPreference) findPreference(AppPreferences.KEY_LOW_BATTERY_PERCENTAGE);

        mEditTextAlertPreference = findPreference(KEY_EDIT_TEXT_ALERT_PREFERENCE);
        mEditMissedCallAlertPreference = findPreference(KEY_EDIT_MISSEDCALL_ALERT_PREFERENCE);
        mEditVoicemailAlertPreference = findPreference(KEY_EDIT_VOICEMAIL_ALERT_PREFERENCE);


        startService(new Intent(this, MissedMessageListenerService.class));

        mPreferences = new AppPreferences(this);
        restorePreferenceStates();
        initPreferences();

        updateEnabledViews();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuItem reset_item = menu.add(0, MENU_RESET, Menu.NONE, R.string.pref_menu_reset);
        reset_item.setIcon(android.R.drawable.ic_menu_revert);

        MenuItem about_item = menu.add(0, MENU_ABOUT, Menu.NONE, R.string.pref_menu_about);
        about_item.setIcon(android.R.drawable.ic_menu_info_details);

        MenuItem whatsnew_item = menu.add(0, MENU_WHATSNEW, Menu.NONE, R.string.pref_menu_whatsnew);
        whatsnew_item.setIcon(android.R.drawable.ic_menu_recent_history);

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        if (item.getItemId() == MENU_RESET) {

            resetToDefaultValues();
            return true;
        } else if (item.getItemId() == MENU_ABOUT) {

            startActivity(new Intent(this, AboutActivity.class));
            return true;
        } else if (item.getItemId() == MENU_WHATSNEW) {

            showWhatsNew();
            return true;
        }

        return false;
    }


    @Override
    public void onResume() {
        super.onResume();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        String whats_new_version = prefs.getString(KEY_WHATS_NEW_VERSION, "0.0");


        String current_version;

        try {
            current_version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            current_version = "0.0";
        }
        
        
        if(!current_version.equals(whats_new_version)){

            showWhatsNew();

            SharedPreferences.Editor e = prefs.edit();
            e.putString(KEY_WHATS_NEW_VERSION, current_version);
            e.commit();
        }
        
    }




    // PRIVATE METHODS -------------------------------------

    private void resetToDefaultValues() {

        mPreferences.resetToDefaults();
        restorePreferenceStates();
        updateEnabledViews();
    }



    private void showWhatsNew(){

        View v = LinearLayout.inflate(this, R.layout.whats_new, null);
        TextView version_header = (TextView)v.findViewById(R.id.versionHeaderTextView);
        TextView market_link = (TextView)v.findViewById(R.id.marketLinkTextView);

        String version;

        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            version = "0.0";
        }

        version_header.setText(getString(R.string.whats_new_header) + " " + version + "?");

        String market_link_html = "<a href=\"" + getString(R.string.marketlinkurl) + "\">"
                + getString(R.string.marketlinktext) + "</a>";
        market_link.setText(Html.fromHtml(market_link_html));
        market_link.setMovementMethod(LinkMovementMethod.getInstance());

        AlertDialog a = new AlertDialog.Builder(MessageAlertsPreferenceActivity.this).create();

        a.setView(v);
        a.setButton(DialogInterface.BUTTON_NEUTRAL, getString(R.string.pref_message_button_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        a.show();
    }



    private void updateEnabledViews() {

        if (mEnableAlertsCheckbox.isChecked())
            enableViews();
        else
            disableViews();
    }


    private void disableViews() {

        mDisableOnBatteryCheckbox.setEnabled(false);
        mLowBatteryPercentageSeekbar.setEnabled(false);
        mShowNotificationCheckbox.setEnabled(false);
        mEditTextAlertPreference.setEnabled(false);
        mEditVoicemailAlertPreference.setEnabled(false);
        mEditMissedCallAlertPreference.setEnabled(false);
    }

    private void enableViews() {

        mDisableOnBatteryCheckbox.setEnabled(true);
        mShowNotificationCheckbox.setEnabled(true);
        mLowBatteryPercentageSeekbar.setEnabled(mDisableOnBatteryCheckbox.isChecked());
        mEditTextAlertPreference.setEnabled(true);
        mEditVoicemailAlertPreference.setEnabled(true);
        mEditMissedCallAlertPreference.setEnabled(true);
    }


    private void restorePreferenceStates(){

        mShowNotificationCheckbox.setChecked(mPreferences.getNotificationEnabled());
        mEnableAlertsCheckbox.setChecked(mPreferences.getAlertsEnabled());
        mDisableOnBatteryCheckbox.setChecked(mPreferences.getDisableOnLowBattery());
        mLowBatteryPercentageSeekbar.setProgress(mPreferences.getLowBatteryPercentage());
    }


    private void initPreferences(){


        mShowNotificationCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean)newValue;
                mPreferences.setNotificationEnabled(value);
                return true;
            }
        });



        mEnableAlertsCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mPreferences.setAlertsEnabled(value);
                
                if (value) {

                    startService(new Intent(MessageAlertsPreferenceActivity.this, MissedMessageListenerService.class));
                    enableViews();
                } else {

                    stopService(new Intent(MessageAlertsPreferenceActivity.this, MissedMessageListenerService.class));
                    disableViews();
                }

                return true;
            }
        });


        mDisableOnBatteryCheckbox.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                boolean value = (Boolean) newValue;
                mPreferences.setDisableOnLowBattery(value);

                mLowBatteryPercentageSeekbar.setEnabled(value);

                // restart the listener service in case it was stopped due to low battery
                if (!value)
                    startService(new Intent(MessageAlertsPreferenceActivity.this, MissedMessageListenerService.class));

                return true;
            }
        });



        mLowBatteryPercentageSeekbar.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {

                mPreferences.setLowBatteryPercentage((Integer)newValue);
                // restart the listener service in case it was stopped due to low battery
                startService(new Intent(MessageAlertsPreferenceActivity.this, MissedMessageListenerService.class));
                return true;
            }
        });


        mEditTextAlertPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent i = new Intent(MessageAlertsPreferenceActivity.this, AlertPreferenceActivity.class);
                i.putExtra(AlertPreferenceActivity.EXTRA_COM_TYPE, AlertPreferenceActivity.COM_TYPE_TEXT);
                startActivity(i);

                return true;
            }
        });

        mEditMissedCallAlertPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent i = new Intent(MessageAlertsPreferenceActivity.this, AlertPreferenceActivity.class);
                i.putExtra(AlertPreferenceActivity.EXTRA_COM_TYPE, AlertPreferenceActivity.COM_TYPE_MISSEDCALL);
                startActivity(i);

                return true;
            }
        });

        mEditVoicemailAlertPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                Intent i = new Intent(MessageAlertsPreferenceActivity.this, AlertPreferenceActivity.class);
                i.putExtra(AlertPreferenceActivity.EXTRA_COM_TYPE, AlertPreferenceActivity.COM_TYPE_VOICEMAIL);
                startActivity(i);

                return true;
            }
        });

    }

}
