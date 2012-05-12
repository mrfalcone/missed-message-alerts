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

import android.app.Service;
import android.content.*;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.util.Log;
import com.mma.missedmessagealerts.AlertPreferences;
import com.mma.missedmessagealerts.AppPreferences;

import java.util.Calendar;

/**
 * Alerts the user according the the application's
 * shared preference values specified by the user.
 *
 * @author Michael R. Falcone
 */
public class AlerterService extends Service {


    /**
     * Action that causes the AlerterService to start executing alerts. Has no effect
     * if the service is already alerting. Can be used as the action of an intent
     * sent to the <code>startService</code> method. If no communication type is specified
     * in the EXTRA_ALERT_FOR extra, alerts will be started for all.
     */
    public static final String START_ALERTS = "com.mma.missedmessagealerts.START_ALERTS";

    /**
     * Action that causes the AlerterService to stop executing alerts. Has no effect
     * if the service is not currently alerting. If no communication type is specified
     * in the EXTRA_ALERT_FOR extra, alerts will be stopped for all.
     */
    public static final String STOP_ALERTS = "com.mma.missedmessagealerts.STOP_ALERTS";


    /**
     * Broadcast by the AlerterService when the service stops alerting.
     */
    public static final String ALERTS_STOPPED = "com.mma.missedmessagealerts.ALERTS_STOPPED";

    /**
     * Extra used with AlerterService actions to store the communication type
     * that the action is for.
     */
    public static final String EXTRA_ALERT_FOR = "alertFor";

    /**
     * Specifies that the action refers to alerts for text messages.
     */
    public static final int ALERT_FOR_TEXT = 0;

    /**
     * Specifies that the action refers to alerts for missed calls.
     */
    public static final int ALERT_FOR_MISSED_CALL = 1;

    /**
     * Specifies that the action refers to alerts for voice mail.
     */
    public static final int ALERT_FOR_VOICE_MAIL = 2;

    /**
     * Vibrate style: two short vibrations
     */
    public static final long[] VIBRATE_PATTERN_0 = {5, 120, 60, 120};

    /**
     * Vibrate style: one long vibration
     */
    public static final long[] VIBRATE_PATTERN_1 = {5, 800};

    /**
     * Vibrate style: two long vibrations
     */
    public static final long[] VIBRATE_PATTERN_2 = {5, 260, 100, 260};

    /**
     * Vibrate style: three medium vibrations
     */
    public static final long[] VIBRATE_PATTERN_3 = {5, 100, 20, 100, 20, 100};

    /**
     * Vibrate style: long short long
     */
    public static final long[] VIBRATE_PATTERN_4 = {5, 300, 60, 120, 60, 300};





    // MEMBER VARIABLES -----------------------------------------


    private static final String WAKELOCK_TAG = "com.mma.missedmessagealerts.wakelock";
    private static final String SCREEN_WAKELOCK_TAG = "com.mma.missedmessagealerts.screenwakelock";

    private static final long WAKE_DURATION_MS = 1500;

    private static final int INFINITE_DURATION = 0;    // value defined in xml for "Forever" duration



    private AppPreferences mPreferences;

    private AudioManager mAudioManager;

    private PowerManager mPowerManager;
    private WakeLock mWakeLock;
    private WakeLock mScreenWakeLock;
    private int mScreenWakelockFlags = 0;

    private Vibrator mVibrator;

    private boolean mIsTextAlerting;
    private boolean mIsMissedCallAlerting;
    private boolean mIsVoiceMailAlerting;

    private long mTextAlertStartTime;
    private long mMissedCallAlertStartTime;
    private long mVoicemailAlertStartTime;

    private MediaPlayer mTextAlertMediaPlayer;
    private MediaPlayer mMissedCallAlertMediaPlayer;
    private MediaPlayer mVoiceMailAlertMediaPlayer;


    private Handler mHandler = new Handler();

    private AlertsControlReceiver mControlReceiver;

    private MissedMessageListenerService mListenerService;





    // SERVICE CONNECTION -----------------------------------------
    private ServiceConnection mListenerConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service){
            mListenerService = ((MissedMessageListenerService.ListenerServiceBinder)service).getService();
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mListenerService = null;
        }
    };




    // BROADCAST RECEIVER -----------------------------------------

    private class AlertsControlReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            int alert_for = -1;

            if(intent.hasExtra(EXTRA_ALERT_FOR))
                alert_for = intent.getExtras().getInt(EXTRA_ALERT_FOR);

            if (action.equals(START_ALERTS)){

                switch(alert_for){
                    case ALERT_FOR_TEXT:
                        startTextAlerts();
                        break;
                    case ALERT_FOR_MISSED_CALL:
                        startMissedCallAlerts();
                        break;
                    case ALERT_FOR_VOICE_MAIL:
                        startVoiceMailAlerts();
                        break;
                    default:
                        startTextAlerts();
                        startMissedCallAlerts();
                        startVoiceMailAlerts();
                        break;
                }
            }

            else if (action.equals(STOP_ALERTS)){

                switch(alert_for){
                    case ALERT_FOR_TEXT:
                        stopTextAlerts();
                        break;
                    case ALERT_FOR_MISSED_CALL:
                        stopMissedCallAlerts();
                        break;
                    case ALERT_FOR_VOICE_MAIL:
                        stopVoiceMailAlerts();
                        break;
                    default:
                        stopSelf();
                }

                if(!(mIsTextAlerting || mIsMissedCallAlerting || mIsVoiceMailAlerting) && alert_for != -1)
                    stopSelf();
            }
        }
    }


    // ALERT UPDATE RUNNABLES -----------------------------------------
    private Runnable mTextAlertRunnable = new Runnable() {

        @Override
        public void run() {

            long duration_seconds = (System.currentTimeMillis() - mTextAlertStartTime);
            int max_duration = Integer.parseInt(mPreferences.getTextAlertPreferences().getDuration()) * 1000;
            int interval = Integer.parseInt(mPreferences.getTextAlertPreferences().getInterval()) * 1000;


            if (!mListenerService.isTextPending() || !mPreferences.getAlertsEnabled()
                    || (duration_seconds >= max_duration && max_duration != INFINITE_DURATION)) {

                mIsTextAlerting = false;
            }

            if(!(mIsTextAlerting || mIsMissedCallAlerting || mIsVoiceMailAlerting)){

                stopSelf();
                return;
            }
            else if(!mIsTextAlerting){

                return;
            }

            alert(mPreferences.getTextAlertPreferences());

            mHandler.postDelayed(this, interval);
        }
    };

    private Runnable mMissedCallAlertRunnable = new Runnable() {

        @Override
        public void run() {

            long duration_seconds = (System.currentTimeMillis() - mMissedCallAlertStartTime);
            int max_duration = Integer.parseInt(mPreferences.getMissedCallAlertPreferences().getDuration()) * 1000;
            int interval = Integer.parseInt(mPreferences.getMissedCallAlertPreferences().getInterval()) * 1000;


            if (!mListenerService.isMissedCallPending() || !mPreferences.getAlertsEnabled()
                    || (duration_seconds >= max_duration && max_duration != INFINITE_DURATION)) {

                mIsMissedCallAlerting = false;
            }

            if(!(mIsTextAlerting || mIsMissedCallAlerting || mIsVoiceMailAlerting)){

                stopSelf();
                return;
            }
            else if(!mIsMissedCallAlerting){

                return;
            }

            alert(mPreferences.getMissedCallAlertPreferences());

            mHandler.postDelayed(this, interval);
        }
    };

    private Runnable mVoicemailAlertRunnable = new Runnable() {

        @Override
        public void run() {

            long duration_seconds = (System.currentTimeMillis() - mVoicemailAlertStartTime);
            int max_duration = Integer.parseInt(mPreferences.getVoiceMailAlertPreferences().getDuration()) * 1000;
            int interval = Integer.parseInt(mPreferences.getVoiceMailAlertPreferences().getInterval()) * 1000;


            if (!mListenerService.isVoiceMailPending() || !mPreferences.getAlertsEnabled()
                    || (duration_seconds >= max_duration && max_duration != INFINITE_DURATION)) {

                mIsVoiceMailAlerting = false;
            }

            if(!(mIsTextAlerting || mIsMissedCallAlerting || mIsVoiceMailAlerting)){

                stopSelf();
                return;
            }
            else if(!mIsVoiceMailAlerting){

                return;
            }

            alert(mPreferences.getVoiceMailAlertPreferences());

            mHandler.postDelayed(this, interval);
        }
    };





    // OVERRIDE METHODS -----------------------------------------

    @Override
    public IBinder onBind(Intent intent) {
        // do not allow clients to bind to this service
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        int alert_for = -1;

        if(intent.hasExtra(EXTRA_ALERT_FOR))
            alert_for = intent.getExtras().getInt(EXTRA_ALERT_FOR);
        
        if (intent.getAction().equals(START_ALERTS)){

            switch(alert_for){
                case ALERT_FOR_TEXT:
                    startTextAlerts();
                    break;
                case ALERT_FOR_MISSED_CALL:
                    startMissedCallAlerts();
                    break;
                case ALERT_FOR_VOICE_MAIL:
                    startVoiceMailAlerts();
                    break;
                default:
                    startTextAlerts();
                    startMissedCallAlerts();
                    startVoiceMailAlerts();
                    break;
            }
        }

        if(!(mIsTextAlerting || mIsMissedCallAlerting || mIsVoiceMailAlerting) && alert_for != -1)
            stopSelf();

        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {

        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG);

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        mPreferences = new AppPreferences(this);

        bindService(new Intent(this, MissedMessageListenerService.class), mListenerConnection, Context.BIND_AUTO_CREATE);

        mControlReceiver = new AlertsControlReceiver();
        registerReceiver(mControlReceiver, new IntentFilter(START_ALERTS));
        registerReceiver(mControlReceiver, new IntentFilter(STOP_ALERTS));

        startTextAlerts();
        startMissedCallAlerts();
        startVoiceMailAlerts();
    }

    @Override
    public void onDestroy() {

        unbindService(mListenerConnection);
        unregisterReceiver(mControlReceiver);
        sendBroadcast(new Intent(ALERTS_STOPPED));
        stopTextAlerts();
        stopMissedCallAlerts();
        stopVoiceMailAlerts();
    }


    // PRIVATE METHODS -----------------------------------------


    private void startTextAlerts() {

        AlertPreferences alertPrefs = mPreferences.getTextAlertPreferences();
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar)now.clone();
        Calendar end = (Calendar)now.clone();
        start.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourStart());
        start.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteStart());
        end.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourEnd());
        end.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteEnd());
        

        if (mIsTextAlerting || !mPreferences.getTextAlertPreferences().getEnabled()
                || (alertPrefs.getSchedulingEnabled() && now.compareTo(start) >= 0 && now.compareTo(end) < 0))
            return;

        mIsTextAlerting = true;

        mTextAlertStartTime = System.currentTimeMillis();

        mHandler.removeCallbacks(mTextAlertRunnable);
        mHandler.postDelayed(mTextAlertRunnable, Integer.parseInt(alertPrefs.getInterval()) * 1000);


        if(!mWakeLock.isHeld()){

            try{
                mWakeLock.acquire();
            }
            catch (RuntimeException e){}
        }
    }

    private void stopTextAlerts() {

        mIsTextAlerting = false;
        mHandler.removeCallbacks(mTextAlertRunnable);

        if(mTextAlertMediaPlayer != null){
            mTextAlertMediaPlayer.stop();
            mTextAlertMediaPlayer.release();
        }

        if(!(mIsMissedCallAlerting || mIsVoiceMailAlerting) && mWakeLock != null && mWakeLock.isHeld()){

            try{
                mWakeLock.release();
            }
            catch (RuntimeException e){}
        }
    }

    private void startMissedCallAlerts() {

        AlertPreferences alertPrefs = mPreferences.getMissedCallAlertPreferences();
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar)now.clone();
        Calendar end = (Calendar)now.clone();
        start.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourStart());
        start.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteStart());
        end.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourEnd());
        end.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteEnd());

        if(mIsMissedCallAlerting || !mPreferences.getMissedCallAlertPreferences().getEnabled()
                || (alertPrefs.getSchedulingEnabled() && now.compareTo(start) >= 0 && now.compareTo(end) < 0))
            return;

        mIsMissedCallAlerting = true;

        mMissedCallAlertStartTime = System.currentTimeMillis();

        mHandler.removeCallbacks(mMissedCallAlertRunnable);
        mHandler.postDelayed(mMissedCallAlertRunnable, Integer.parseInt(mPreferences.getMissedCallAlertPreferences().getInterval()) * 1000);


        if(!mWakeLock.isHeld()){

            try{
                mWakeLock.acquire();
            }
            catch (RuntimeException e){}
        }
    }

    private void stopMissedCallAlerts() {

        mIsMissedCallAlerting = false;
        mHandler.removeCallbacks(mMissedCallAlertRunnable);

        if(mMissedCallAlertMediaPlayer != null){
            mMissedCallAlertMediaPlayer.stop();
            mMissedCallAlertMediaPlayer.release();
        }

        if(!(mIsTextAlerting || mIsVoiceMailAlerting) && mWakeLock != null && mWakeLock.isHeld()){

            try{
                mWakeLock.release();
            }
            catch (RuntimeException e){}
        }
    }

    private void startVoiceMailAlerts() {

        AlertPreferences alertPrefs = mPreferences.getVoiceMailAlertPreferences();
        Calendar now = Calendar.getInstance();
        Calendar start = (Calendar)now.clone();
        Calendar end = (Calendar)now.clone();
        start.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourStart());
        start.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteStart());
        end.set(Calendar.HOUR_OF_DAY, alertPrefs.getSchedulingHourEnd());
        end.set(Calendar.MINUTE, alertPrefs.getSchedulingMinuteEnd());

        if(mIsVoiceMailAlerting || !mPreferences.getVoiceMailAlertPreferences().getEnabled()
                || (alertPrefs.getSchedulingEnabled() && now.compareTo(start) >= 0 && now.compareTo(end) < 0))
            return;

        mIsVoiceMailAlerting = true;

        mVoicemailAlertStartTime = System.currentTimeMillis();

        mHandler.removeCallbacks(mVoicemailAlertRunnable);
        mHandler.postDelayed(mVoicemailAlertRunnable, Integer.parseInt(mPreferences.getVoiceMailAlertPreferences().getInterval()) * 1000);


        if(!mWakeLock.isHeld()){

            try{
                mWakeLock.acquire();
            }
            catch (RuntimeException e){}
        }
    }

    private void stopVoiceMailAlerts() {

        mIsVoiceMailAlerting = false;
        mHandler.removeCallbacks(mVoicemailAlertRunnable);

        if(mVoiceMailAlertMediaPlayer != null){
            mVoiceMailAlertMediaPlayer.stop();
            mVoiceMailAlertMediaPlayer.release();
        }

        if(!(mIsMissedCallAlerting || mIsTextAlerting) && mWakeLock != null && mWakeLock.isHeld()){

            try{
                mWakeLock.release();
            }
            catch (RuntimeException e){}
        }
    }


    private void alert(AlertPreferences alertPrefs) {

        // if the screen is already on, user is probably interacting with device
        //  and does not require alerts
        if (mPowerManager.isScreenOn() && mScreenWakeLock != null && !mScreenWakeLock.isHeld()
                || mPowerManager.isScreenOn() && mScreenWakeLock == null) {

            return;
        }

        // TODO do not alert if screen is unlocked

        if (alertPrefs.getVibrateEnabled()) {

            long[] pattern;

            switch (Integer.parseInt(alertPrefs.getVibrateStyle())){

                case 0: pattern = VIBRATE_PATTERN_0;
                        break;
                case 1: pattern = VIBRATE_PATTERN_1;
                        break;
                case 2: pattern = VIBRATE_PATTERN_2;
                        break;
                case 3: pattern = VIBRATE_PATTERN_3;
                        break;
                case 4: pattern = VIBRATE_PATTERN_4;
                        break;
                default:
                    pattern = VIBRATE_PATTERN_0;
            }

            try {
                mVibrator.vibrate(pattern, -1);
            } catch (Exception e) {
                Log.e("AlerterService", "Vibrator error: " + e.getMessage());
            }
        }


        if (alertPrefs.getAudioEnabled()) {

            String uri_str = alertPrefs.getAlertTone();

            boolean play_in_silent = !(alertPrefs.getAudioDisabledOnSilent());

            float volume = alertPrefs.getAlertVolume() / 100.0f;

            int sys_volume = mAudioManager.getStreamVolume(AudioManager.STREAM_SYSTEM);


            try {

                if (uri_str.length() != 0 && (play_in_silent || sys_volume > 0)) {
                    
                    MediaPlayer mp = null;
                    
                    if(alertPrefs.getPrefix().equals(AppPreferences.PREFIX_TEXT))
                        mp = mTextAlertMediaPlayer;
                    else if(alertPrefs.getPrefix().equals(AppPreferences.PREFIX_CALLS))
                        mp = mMissedCallAlertMediaPlayer;
                    else if(alertPrefs.getPrefix().equals(AppPreferences.PREFIX_VOICEMAIL))
                        mp = mVoiceMailAlertMediaPlayer;

                    if (mp == null) {

                        mp = new MediaPlayer();

                        // TODO manage error states

                        mp.setDataSource(this, Uri.parse(uri_str));

                        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mediaPlayer) {
                                mediaPlayer.release();
                                mediaPlayer = null;
                            }
                        });

                        mp.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK);

                        if (volume < 0.9f)
                            mp.setVolume(volume, volume);

                        mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        mp.prepare();
                        mp.start();
                    }
                }
            } catch (Exception e) {
            }
        }

        if (alertPrefs.getFlashScreenEnabled() && checkAndMakeScreenWakelock(alertPrefs)) {

            if(mScreenWakeLock != null && !mScreenWakeLock.isHeld()){

                try{
                    mScreenWakeLock.acquire(WAKE_DURATION_MS);
                }
                catch (RuntimeException e){}
            }
        }

    }



    private boolean checkAndMakeScreenWakelock(AlertPreferences alertPrefs){

        int screen_wakelock_flags = PowerManager.ACQUIRE_CAUSES_WAKEUP;

        if (alertPrefs.getDimFlashEnabled())
            screen_wakelock_flags |= PowerManager.SCREEN_DIM_WAKE_LOCK;
        else
            screen_wakelock_flags |= PowerManager.FULL_WAKE_LOCK;


        if (mScreenWakelockFlags != screen_wakelock_flags || mScreenWakeLock == null) {

            try{

                mScreenWakeLock = mPowerManager.newWakeLock(screen_wakelock_flags, SCREEN_WAKELOCK_TAG);
                mScreenWakelockFlags = screen_wakelock_flags;
            }
            catch (Exception e){
                return false;
            }
        }

        return mScreenWakeLock != null;
    }
}
