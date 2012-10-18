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

package com.mma.missedmessagealerts.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.mma.missedmessagealerts.AppPreferences;
import com.mma.missedmessagealerts.services.MissedMessageListenerService;


/**
 * Receives the system boot broadcast and starts the listener service
 * if it has been enabled by the user.
 *
 * @author Michael R. Falcone
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        AppPreferences prefs = new AppPreferences(context);

        if(prefs.getAlertsEnabled())
            context.startService(new Intent(context, MissedMessageListenerService.class));
    }
}
