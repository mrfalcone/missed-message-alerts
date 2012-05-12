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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import com.mma.missedmessagealerts.R;


/**
 * Activity to display information about the app and services.
 *
 * @author Michael R. Falcone
 */
public class AboutActivity extends Activity {

    // OVERRIDE METHODS -------------------------------------

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about_layout);


        TextView version_text = (TextView) findViewById(R.id.versionTextView);
        TextView market_link = (TextView) findViewById(R.id.marketLinkTextView);

        String version = getString(R.string.version) + ": ";

        try {
            version += getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException nnfe) {
            version += "0.0";
        }

        version_text.setText(version);

        String market_link_html = "<a href=\"" + getString(R.string.marketlinkurl) + "\">"
                + getString(R.string.marketlinktext) + "</a>";
        market_link.setText(Html.fromHtml(market_link_html));
        market_link.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
