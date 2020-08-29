/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.nust.socialapp.linphone.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;


import com.nust.socialapp.R;
import com.nust.socialapp.linphone.Manager;
import com.nust.socialapp.linphone.assistant.MenuAssistantActivity;
import com.nust.socialapp.linphone.contacts.ContactsActivity;
import com.nust.socialapp.linphone.dialer.DialerActivity;
import com.nust.socialapp.linphone.service.LinphoneService;
import com.nust.socialapp.linphone.service.ServiceWaitThread;
import com.nust.socialapp.linphone.service.ServiceWaitThreadListener;
import com.nust.socialapp.linphone.settings.LinphonePreferences;

/** Creates LinphoneService and wait until Core is ready to start main Activity */
public class LauncherActivity extends Activity implements ServiceWaitThreadListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
            setContentView(R    .layout.dialer);
        // Otherwise use drawable/launch_screen layer list up until first activity starts
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            try {
                startService(new Intent().setClass(LauncherActivity.this, LinphoneService.class));
                new ServiceWaitThread(this).start();
            } catch (IllegalStateException ise) {
                Log.e("Linphone", "Exception raised while starting service: " + ise);
            }
        }
    }

    @Override
    public void onServiceReady() {
        final Class<? extends Activity> classToStart;

        boolean useFirstLoginActivity =
                getResources().getBoolean(R.bool.display_account_assistant_at_first_start);
        if (useFirstLoginActivity && LinphonePreferences.instance().isFirstLaunch()) {
            classToStart = MenuAssistantActivity.class;
        } else {
            if (getIntent().getExtras() != null) {
                String activity = getIntent().getExtras().getString("Activity", null);
                if (ContactsActivity.NAME.equals(activity)) {
                    classToStart = ContactsActivity.class;
                } else {
                    classToStart = DialerActivity.class;
                }
            } else {
                classToStart = DialerActivity.class;
            }
        }

        if (getResources().getBoolean(R.bool.check_for_update_when_app_starts)) {
            Manager.getInstance().checkForUpdate();
        }

        Intent intent = new Intent();
        intent.setClass(LauncherActivity.this, classToStart);
        if (getIntent() != null && getIntent().getExtras() != null) {
            intent.putExtras(getIntent().getExtras());
        }
        intent.setAction(getIntent().getAction());
        intent.setType(getIntent().getType());
        intent.setData(getIntent().getData());
        startActivity(intent);

        Manager.getInstance().changeStatusToOnline();
    }
}
