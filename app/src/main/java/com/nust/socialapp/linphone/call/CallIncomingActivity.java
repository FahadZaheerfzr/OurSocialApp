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
package com.nust.socialapp.linphone.call;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.view.KeyEvent;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.Call.State;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.tools.Log;

import java.util.ArrayList;

import com.nust.socialapp.R;
import com.nust.socialapp.linphone.Context;
import com.nust.socialapp.linphone.Manager;
import com.nust.socialapp.linphone.activities.LinphoneGenericActivity;
import com.nust.socialapp.linphone.call.views.CallIncomingAnswerButton;
import com.nust.socialapp.linphone.call.views.CallIncomingButtonListener;
import com.nust.socialapp.linphone.call.views.CallIncomingDeclineButton;
import com.nust.socialapp.linphone.compatibility.Compatibility;
import com.nust.socialapp.linphone.contacts.ContactsManager;
import com.nust.socialapp.linphone.contacts.LinphoneContact;
import com.nust.socialapp.linphone.contacts.views.ContactAvatar;
import com.nust.socialapp.linphone.settings.LinphonePreferences;
import com.nust.socialapp.linphone.utils.LinphoneUtils;

public class CallIncomingActivity extends LinphoneGenericActivity {
    private TextView mName, mNumber;
    private Call mCall;
    private CoreListenerStub mListener;
    private boolean mAlreadyAcceptedOrDeniedCall;
    private TextureView mVideoDisplay;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Compatibility.setShowWhenLocked(this, true);
        Compatibility.setTurnScreenOn(this, true);

        setContentView(R.layout.call_incoming);

        mName = findViewById(R.id.contact_name);
        mNumber = findViewById(R.id.contact_number);
        mVideoDisplay = findViewById(R.id.videoSurface);

        CallIncomingAnswerButton mAccept = findViewById(R.id.answer_button);
        CallIncomingDeclineButton mDecline = findViewById(R.id.decline_button);
        ImageView mAcceptIcon = findViewById(R.id.acceptIcon);
        lookupCurrentCall();

        if (LinphonePreferences.instance() != null
                && mCall != null
                && mCall.getRemoteParams() != null
                && LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()
                && mCall.getRemoteParams().videoEnabled()) {
            mAcceptIcon.setImageResource(R.drawable.call_video_start);
        }

        KeyguardManager mKeyguardManager =
                (KeyguardManager) getSystemService(android.content.Context.KEYGUARD_SERVICE);
        boolean doNotUseSliders =
                getResources()
                        .getBoolean(
                                R.bool.do_not_use_sliders_to_answer_hangup_call_if_phone_unlocked);
        if (doNotUseSliders && !mKeyguardManager.inKeyguardRestrictedInputMode()) {
            mAccept.setSliderMode(false);
            mDecline.setSliderMode(false);
        } else {
            mAccept.setSliderMode(true);
            mDecline.setSliderMode(true);
            mAccept.setDeclineButton(mDecline);
            mDecline.setAnswerButton(mAccept);
        }
        mAccept.setListener(
                new CallIncomingButtonListener() {
                    @Override
                    public void onAction() {
                        answer();
                    }
                });
        mDecline.setListener(
                new CallIncomingButtonListener() {
                    @Override
                    public void onAction() {
                        decline();
                    }
                });

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, State state, String message) {
                        if (state == State.End || state == State.Released) {
                            mCall = null;
                            finish();
                        }
                    }
                };
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkAndRequestCallPermissions();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();
        Core core = Manager.getCore();
        if (core != null) {
            core.addListener(mListener);
        }

        mAlreadyAcceptedOrDeniedCall = false;
        mCall = null;

        // Only one caller ringing at a time is allowed
        lookupCurrentCall();
        if (mCall == null) {
            // The incoming caller no longer exists.
            Log.d("Couldn't find incoming caller");
            finish();
            return;
        }

        Address address = mCall.getRemoteAddress();
        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        if (contact != null) {
            ContactAvatar.displayAvatar(contact, findViewById(R.id.avatar_layout), true);
            mName.setText(contact.getFullName());
        } else {
            String displayName = LinphoneUtils.getAddressDisplayName(address);
            ContactAvatar.displayAvatar(displayName, findViewById(R.id.avatar_layout), true);
            mName.setText(displayName);
        }
        mNumber.setText(address.asStringUriOnly());

        if (LinphonePreferences.instance().acceptIncomingEarlyMedia()) {
            if (mCall.getCurrentParams() != null && mCall.getCurrentParams().videoEnabled()) {
                findViewById(R.id.avatar_layout).setVisibility(View.GONE);
                mCall.getCore().setNativeVideoWindowId(mVideoDisplay);
            }
        }
    }

    @Override
    protected void onPause() {
        Core core = Manager.getCore();
        if (core != null) {
            core.removeListener(mListener);
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        mName = null;
        mNumber = null;
        mCall = null;
        mListener = null;
        mVideoDisplay = null;

        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (Context.isReady()
                && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME)
                && mCall != null) {
            mCall.terminate();
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    private void lookupCurrentCall() {
        if (Manager.getCore() != null) {
            for (Call call : Manager.getCore().getCalls()) {
                if (State.IncomingReceived == call.getState()
                        || State.IncomingEarlyMedia == call.getState()) {
                    mCall = call;
                    break;
                }
            }
        }
    }

    private void decline() {
        if (mAlreadyAcceptedOrDeniedCall) {
            return;
        }
        mAlreadyAcceptedOrDeniedCall = true;

        if (mCall != null) mCall.terminate();
        finish();
    }

    private void answer() {
        if (mAlreadyAcceptedOrDeniedCall) {
            return;
        }
        mAlreadyAcceptedOrDeniedCall = true;

        if (!Manager.getCallManager().acceptCall(mCall)) {
            // the above method takes care of Samsung Galaxy S
            Toast.makeText(this, R.string.couldnt_accept_call, Toast.LENGTH_LONG).show();
        }
    }

    private void checkAndRequestCallPermissions() {
        ArrayList<String> permissionsList = new ArrayList<>();

        int recordAudio =
                getPackageManager()
                        .checkPermission(Manifest.permission.RECORD_AUDIO, getPackageName());
        Log.i(
                "[Permission] Record audio permission is "
                        + (recordAudio == PackageManager.PERMISSION_GRANTED
                                ? "granted"
                                : "denied"));
        int camera =
                getPackageManager().checkPermission(Manifest.permission.CAMERA, getPackageName());
        Log.i(
                "[Permission] Camera permission is "
                        + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        int readPhoneState =
                getPackageManager()
                        .checkPermission(Manifest.permission.READ_PHONE_STATE, getPackageName());
        Log.i(
                "[Permission] Read phone state permission is "
                        + (camera == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));

        if (recordAudio != PackageManager.PERMISSION_GRANTED) {
            Log.i("[Permission] Asking for record audio");
            permissionsList.add(Manifest.permission.RECORD_AUDIO);
        }
        if (readPhoneState != PackageManager.PERMISSION_GRANTED) {
            Log.i("[Permission] Asking for read phone state");
            permissionsList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if (LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()
                && mCall != null
                && mCall.getRemoteParams() != null
                && mCall.getRemoteParams().videoEnabled()) {
            if (camera != PackageManager.PERMISSION_GRANTED) {
                Log.i("[Permission] Asking for camera");
                permissionsList.add(Manifest.permission.CAMERA);
            }
        }

        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, 0);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        for (int i = 0; i < permissions.length; i++) {
            Log.i(
                    "[Permission] "
                            + permissions[i]
                            + " is "
                            + (grantResults[i] == PackageManager.PERMISSION_GRANTED
                                    ? "granted"
                                    : "denied"));
            if (permissions[i].equals(Manifest.permission.CAMERA)
                    && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                LinphoneUtils.reloadVideoDevices();
            }
        }
    }
}
