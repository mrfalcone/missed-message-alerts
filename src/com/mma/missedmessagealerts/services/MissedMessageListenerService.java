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

package com.mma.missedmessagealerts.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.provider.CallLog;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.mma.missedmessagealerts.AppPreferences;
import com.mma.missedmessagealerts.R;

/**
 * Service that listens for desired messages and notifies AlerterService
 * to start and stop alerting as needed.
 *
 * @author Michael R. Falcone
 */
public class MissedMessageListenerService extends Service {


    /**
     * Action that causes the MissedMessageListenerService to begin listening
     * to missed messages, and start the AlerterService if necessary.
     */
    public static final String START_LISTENER = "com.mma.missedmessagealerts.START_LISTENER";


    // MEMBER VARIABLES -----------------------------------------

    private static long UPDATE_INTERVAL_MS = 1000;
    private static long NEW_MESSAGE_TIMEOUT_MS = 3000;  // maximum time to wait for new message to be confirmed after being detected

    private static int NOTIFICATION_ID = 1;

    private static final String ACTION_SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private static final String ACTION_MMS_RECEIVED = "android.provider.Telephony.MMS_RECEIVED";

    private static final String STOP_ALL_ALERTS = "com.mma.missedmessagealerts.STOP_ALL_ALERTS";

    private AppPreferences mPreferences;
    
    private Handler mHandler = new Handler();
    private NotificationManager mNotificationManager;
    private Notification mNotification;
    private TelephonyManager mTelephonyManager;

    private ServiceControlReceiver mServiceControlReceiver;
    private BatteryReceiver mBatteryReceiver;
    private SmsReceiver mSmsReceiver;
    private MmsReceiver mMmsReceiver;


    private boolean mPendingSms = false;
    private int mLastSmsCount = 0;
    private long mFirstSmsDetectTime = 0;

    private boolean mPendingMms = false;
    private int mLastMmsCount = 0;
    private long mFirstMmsDetectTime = 0;

    private boolean mPendingCall = false;
    private int mLastMissedCallCount = 0;
    private long mFirstMissedCallDetectTime = 0;

    private boolean mPendingVoicemail = false;

    private boolean mCurrentlyInCall = false;




    // SERVICE BINDER -----------------------------------------

    private final IBinder mListenerServiceBinder = new ListenerServiceBinder();

    public final class ListenerServiceBinder extends Binder {
        public MissedMessageListenerService getService(){
            return MissedMessageListenerService.this;
        }
    }



    // UPDATE RUNNABLE -----------------------------------------

    /*
      * Runs when a message is pending.
      */
    private Runnable mUpdateRunnable = new Runnable() {

        @Override
        public void run() {

            updateService();
        }
    };


    // BROADCAST RECEIVERS -----------------------------------------

    private class ServiceControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();

            if (action.equals(STOP_ALL_ALERTS) || action.equals(AlerterService.ALERTS_STOPPED)){

                mPendingCall = false;
                stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
                mPendingSms = false;
                mPendingMms = false;
                stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
                mPendingVoicemail = false;
                stopAlertingUser(AlerterService.ALERT_FOR_VOICE_MAIL);
            }
        }
    }


    private class BatteryReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            // if the battery is too low and device is not charging, stop service
            if (isBatteryLevelTooLow(intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 100))
                    && intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) == 0)
                stopSelf();
        }
    }


    private class SmsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            if (mPreferences.getAlertsEnabled() && !mPendingSms
                    && mPreferences.getTextAlertPreferences().getEnabled()
                    && !mCurrentlyInCall) {


                mPendingSms = true;
                mLastSmsCount = 0;
                mFirstSmsDetectTime = System.currentTimeMillis();
                mHandler.removeCallbacks(mUpdateRunnable);
                mHandler.postDelayed(mUpdateRunnable, UPDATE_INTERVAL_MS);
            }

        }
    }


    private class MmsReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {


            if (mPreferences.getAlertsEnabled() && !mPendingMms
                    && mPreferences.getTextAlertPreferences().getEnabled()
                    && !mCurrentlyInCall) {


                mPendingMms = true;
                mLastMmsCount = 0;
                mFirstMmsDetectTime = System.currentTimeMillis();
                mHandler.removeCallbacks(mUpdateRunnable);
                mHandler.postDelayed(mUpdateRunnable, UPDATE_INTERVAL_MS);
            }

        }
    }


    // PHONE STATE LISTENER -----------------------------------------


    PhoneStateListener mPhoneStateListener = new PhoneStateListener() {

        private boolean startedRinging = false;
        private boolean wasAnswered = false;

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {


            if (!(mPreferences.getAlertsEnabled()
                    && mPreferences.getMissedCallAlertPreferences().getEnabled()))
                return;


            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    startedRinging = true;
                    wasAnswered = false;
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    wasAnswered = true;
                    mCurrentlyInCall = true;
                    break;

                case TelephonyManager.CALL_STATE_IDLE:
                    if (startedRinging && !wasAnswered && !mPendingCall) {     // call was missed

                        mPendingCall = true;
                        mLastMissedCallCount = 0;
                        mFirstMissedCallDetectTime = System.currentTimeMillis();
                        mHandler.removeCallbacks(mUpdateRunnable);
                        mHandler.postDelayed(mUpdateRunnable, UPDATE_INTERVAL_MS);
                    }

                    startedRinging = false;
                    wasAnswered = false;

                    if (mCurrentlyInCall && mPreferences.getAlertsEnabled()) {

                        if (mPreferences.getTextAlertPreferences().getEnabled()) {
                            mPendingSms = true;
                            mPendingMms = true;
                        }

                        if (mPreferences.getMissedCallAlertPreferences().getEnabled()) {
                            mPendingCall = true;
                        }

                        updateService();
                    }

                    mCurrentlyInCall = false;

                    break;

                default:
                    break;
            }


        }


        @Override
        public void onMessageWaitingIndicatorChanged(boolean mwi) {


            if (mwi && !mPendingVoicemail && mPreferences.getAlertsEnabled()
                    && mPreferences.getVoiceMailAlertPreferences().getEnabled()
                    && !mCurrentlyInCall) {

                mPendingVoicemail = true;
                startAlertingUser(AlerterService.ALERT_FOR_VOICE_MAIL);
                mHandler.removeCallbacks(mUpdateRunnable);
                mHandler.postDelayed(mUpdateRunnable, UPDATE_INTERVAL_MS);
            } else{
                mPendingVoicemail = false;
                stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
            }
        }

    };


    // OVERRIDE METHODS -----------------------------------------

    @Override
    public IBinder onBind(Intent intent) {

        return mListenerServiceBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {

        mPreferences = new AppPreferences(this);

        // make notification object for status bar that will send this service STOP_ALL_ALERTS
        PendingIntent pi = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(STOP_ALL_ALERTS), 0);
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotification = new Notification(R.drawable.ic_notification, getResources().getString(R.string.notify_ticker),
                System.currentTimeMillis());
        mNotification.flags |= Notification.FLAG_ONGOING_EVENT;
        mNotification.setLatestEventInfo(getApplicationContext(), getResources().getString(R.string.notify_title),
                getResources().getString(R.string.notify_text), pi);


        mServiceControlReceiver = new ServiceControlReceiver();
        registerReceiver(mServiceControlReceiver, new IntentFilter(STOP_ALL_ALERTS));
        registerReceiver(mServiceControlReceiver, new IntentFilter(AlerterService.ALERTS_STOPPED));

        mBatteryReceiver = new BatteryReceiver();
        registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));


        mSmsReceiver = new SmsReceiver();
        mMmsReceiver = new MmsReceiver();
        registerReceiver(mSmsReceiver, new IntentFilter(ACTION_SMS_RECEIVED));
        registerReceiver(mMmsReceiver, new IntentFilter(ACTION_MMS_RECEIVED));


        // register telephony listener
        mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_MESSAGE_WAITING_INDICATOR);


        // run the update method in case there are already missed messages
        //  when the service starts
        if (mPreferences.getAlertsEnabled()) {

            if (mPreferences.getTextAlertPreferences().getEnabled()) {
                mPendingSms = true;
                mPendingMms = true;
            }

            if (mPreferences.getMissedCallAlertPreferences().getEnabled()) {
                mPendingCall = true;
            }

            updateService();
        }
    }

    @Override
    public void onDestroy() {

        mPendingCall = false;
        stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
        mPendingSms = false;
        mPendingMms = false;
        stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
        mPendingVoicemail = false;
        stopAlertingUser(AlerterService.ALERT_FOR_VOICE_MAIL);

        unregisterReceiver(mServiceControlReceiver);
        unregisterReceiver(mBatteryReceiver);

        unregisterReceiver(mSmsReceiver);
        unregisterReceiver(mMmsReceiver);

        mTelephonyManager.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);

        stopService(new Intent(this, AlerterService.class));
    }




    // PUBLIC METHODS -----------------------------------------

    /**
     * Returns whether a text message (MMS or SMS) is currently pending and unread.
     */
    public boolean isTextPending(){
        return mPendingMms || mPendingSms;
    }


    /**
     * Returns whether a missed call is currently pending.
     */
    public boolean isMissedCallPending(){
        return mPendingCall;
    }


    /**
     * Returns whether a voice mail is currently pending.
     */
    public boolean isVoiceMailPending(){
        return mPendingVoicemail;
    }




    // PRIVATE METHODS -----------------------------------------


    /*
     * Runs checks to see if pending messages have been acknowledged by the
     * user, then stops causes alerts to stop or reschedules an update
     * as needed.
     */
    private void updateService() {

        long cur_time = System.currentTimeMillis();

        if (mPendingSms)
            checkAndUpdatePendingSms(cur_time);

        if (mPendingMms)
            checkAndUpdatePendingMms(cur_time);

        if (mPendingCall)
            checkAndUpdatePendingCall(cur_time);


        if (mPendingSms || mPendingMms || mPendingCall || mPendingVoicemail)
            mHandler.postDelayed(mUpdateRunnable, UPDATE_INTERVAL_MS);
    }


    /*
     * If user has chosen to disable service on low battery level, and
     * the given level is below the specified level, this will return true.
     */
    private boolean isBatteryLevelTooLow(int level) {

        return mPreferences.getDisableOnLowBattery() && level <= mPreferences.getLowBatteryPercentage();
    }


    /*
     * Tells the AlerterService to start alerts and places the notification
     * in the status bar.
     */
    private void startAlertingUser(int communicationType) {
        
        Intent start = new Intent(AlerterService.START_ALERTS);
        start.putExtra(AlerterService.EXTRA_ALERT_FOR, communicationType);

        startService(start);

        if (mPreferences.getNotificationEnabled())
            mNotificationManager.notify(NOTIFICATION_ID, mNotification);
    }


    /*
     * Tells the AlerterService to stop alerting and removes the notification
     * from the status bar.
     */
    private void stopAlertingUser(int communicationType) {

        Intent stop = new Intent(AlerterService.STOP_ALERTS);
        stop.putExtra(AlerterService.EXTRA_ALERT_FOR, communicationType);
        
        sendBroadcast(stop);

        if(!(mPendingCall || mPendingSms || mPendingMms || mPendingVoicemail)){

            mNotificationManager.cancel(NOTIFICATION_ID);
            mHandler.removeCallbacks(mUpdateRunnable);
        }
    }


    /*
     * After a new sms message is detected by way of receiving a broadcast message,
     * this method first verifies that a new message was actually received. If so,
     * this method sends a start alert message to the AlerterService and begins
     * checking for user acknowledgement of the message.
     *
     * If no new sms message can be verified after NEW_MESSAGE_TIMEOUT_MS time passes,
     * the alerts do not begin and this method is no longer called.
     */
    private void checkAndUpdatePendingSms(long currentTime) {

        final String[] proj = {"_id"};
        final String where = "read = 0";

        Cursor c = getContentResolver().query(Uri.parse("content://sms"), proj, where, null, null);

        if (c == null)
            return;

        try {
            int sms_count = c.getCount();

            if (sms_count == 0 && mLastSmsCount != 0){

                mPendingSms = false;
                stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
            }

            else if (sms_count > 0) {

                if (mLastSmsCount == 0)
                    startAlertingUser(AlerterService.ALERT_FOR_TEXT);

                mLastSmsCount = sms_count;

            }
            else if (currentTime - mFirstSmsDetectTime >= NEW_MESSAGE_TIMEOUT_MS){

                mPendingSms = false;
                stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
            }
        } catch (Exception e) {
            mPendingSms = false;
            stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
        } finally {

            c.close();
        }
    }


    /*
    * After a new mms message is detected by way of receiving a broadcast message,
    * this method first verifies that a new message was actually received. If so,
    * this method sends a start alert message to the AlerterService and begins
    * checking for user acknowledgement of the message.
    *
    * If no new mms message can be verified after NEW_MESSAGE_TIMEOUT_MS time passes,
    * the alerts do not begin and this method is no longer called.
    */
    private void checkAndUpdatePendingMms(long currentTime) {

        final String[] proj = {"_id"};
        final String where = "read = 0";

        Cursor c = getContentResolver().query(Uri.parse("content://mms"), proj, where, null, null);

        if (c == null)
            return;

        try {
            int mms_count = c.getCount();

            if (mms_count == 0 && mLastMmsCount != 0){

                mPendingMms = false;
                stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
            }

            else if (mms_count > 0) {

                if (mLastMmsCount == 0)
                    startAlertingUser(AlerterService.ALERT_FOR_TEXT);

                mLastMmsCount = mms_count;
            }
            else if (currentTime - mFirstMmsDetectTime >= NEW_MESSAGE_TIMEOUT_MS){

                mPendingMms = false;
                stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
            }

        } catch (Exception e) {
            mPendingMms = false;
            stopAlertingUser(AlerterService.ALERT_FOR_TEXT);
        } finally {

            c.close();
        }
    }



    /*
     * After a missed call is detected by way of receiving a broadcast message,
     * this method first verifies that a missed call actually occurred. If so,
     * this method sends a start alert message to the AlerterService and begins
     * checking for user acknowledgement of the missed call.
     *
     * If no missed calls can be verified after NEW_MESSAGE_TIMEOUT_MS time passes,
     * the alerts do not begin and this method is no longer called.
     */
    private void checkAndUpdatePendingCall(long currentTime) {

        final String[] proj = {CallLog.Calls._ID};
        final String where = CallLog.Calls.TYPE + " = " + CallLog.Calls.MISSED_TYPE + " AND "
                + CallLog.Calls.NEW + " = 1";

        Cursor c = getContentResolver().query(CallLog.Calls.CONTENT_URI, proj, where, null, null);

        if (c == null)
            return;

        try {

            int missed_call_count = c.getCount();

            if (missed_call_count == 0 && mLastMissedCallCount != 0){

                mPendingCall = false;
                stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
            }

            else if (missed_call_count > 0) {

                if (mLastMissedCallCount == 0)
                    startAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);

                mLastMissedCallCount = missed_call_count;
            }
            else if (currentTime - mFirstMissedCallDetectTime >= NEW_MESSAGE_TIMEOUT_MS){

                mPendingCall = false;
                stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
            }
        } catch (Exception e) {

            mPendingCall = false;
            stopAlertingUser(AlerterService.ALERT_FOR_MISSED_CALL);
        } finally {

            c.close();
        }
    }
}
