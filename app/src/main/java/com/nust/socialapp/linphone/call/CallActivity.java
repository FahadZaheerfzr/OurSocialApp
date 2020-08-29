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
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.TextureView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;



import org.linphone.core.Address;
import org.linphone.core.Call;
import org.linphone.core.ChatMessage;
import org.linphone.core.ChatRoom;
import org.linphone.core.Core;
import org.linphone.core.CoreListener;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Locale;

import com.nust.socialapp.R;
import com.nust.socialapp.linphone.Context;
import com.nust.socialapp.linphone.Manager;
import com.nust.socialapp.linphone.activities.LinphoneGenericActivity;
import com.nust.socialapp.linphone.compatibility.Compatibility;
import com.nust.socialapp.linphone.contacts.ContactsManager;
import com.nust.socialapp.linphone.contacts.ContactsUpdatedListener;
import com.nust.socialapp.linphone.contacts.LinphoneContact;
import com.nust.socialapp.linphone.contacts.views.ContactAvatar;
import com.nust.socialapp.linphone.dialer.DialerActivity;
import com.nust.socialapp.linphone.service.LinphoneService;
import com.nust.socialapp.linphone.settings.LinphonePreferences;
import com.nust.socialapp.linphone.utils.LinphoneUtils;

public class CallActivity extends LinphoneGenericActivity
        implements ContactsUpdatedListener{
    private int seconds = 0;

    private boolean running ;
    private static final int SECONDS_BEFORE_HIDING_CONTROLS = 4000;
    private static final int SECONDS_BEFORE_DENYING_CALL_UPDATE = 30000;

    private static final int CAMERA_TO_TOGGLE_VIDEO = 0;
    private static final int MIC_TO_DISABLE_MUTE = 1;
    private static final int WRITE_EXTERNAL_STORAGE_FOR_RECORDING = 2;
    private static final int CAMERA_TO_ACCEPT_UPDATE = 3;
    private static final int ALL_PERMISSIONS = 4;

    private static class HideControlsRunnable implements Runnable {
        private WeakReference<CallActivity> mWeakCallActivity;

        public HideControlsRunnable(CallActivity activity) {
            mWeakCallActivity = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            // Make sure that at the time this is executed this is still required
            if (!Context.isReady()) return;

            Call call = Manager.getCore().getCurrentCall();
            if (call != null && call.getCurrentParams().videoEnabled()) {
                CallActivity activity = mWeakCallActivity.get();
                if (activity != null) activity.updateButtonsVisibility(false);
            }
        }
    }

    private final HideControlsRunnable mHideControlsRunnable = new HideControlsRunnable(this);

    private float mPreviewX, mPreviewY;
    private TextureView mLocalPreview, mRemoteVideo;
    private RelativeLayout mButtons,
            mActiveCalls,
            mContactAvatar,
            mActiveCallHeader,
            mConferenceHeader;
    private LinearLayout mCallsList, mCallPausedByRemote, mConferenceList;
    private ImageView mMicro, mSpeaker, mVideo;
    private ImageView mPause, mSwitchCamera, mRecordingInProgress;
    private ImageView mExtrasButtons, mAddCall, mTransferCall, mRecordCall, mConference;
    private ImageView mAudioRoute, mRouteEarpiece, mRouteSpeaker, mRouteBluetooth;
    private TextView mContactName, mMissedMessages;
    private ProgressBar mVideoInviteInProgress;
    private Chronometer mCallTimer;
    private CountDownTimer mCallUpdateCountDownTimer;
    private Dialog mCallUpdateDialog;
    boolean mIsSpeakerEnabled = false;
    private CallStatsFragment mStatsFragment;
    private Core mCore;
    private CoreListener mListener;
    private AndroidAudioManager mAudioManager;
    private VideoZoomHelper mZoomHelper;
    private ImageButton Speaker;
    private boolean mSpeakerIsSelected;
    private boolean mMicIsSelected;
    private TextView CallStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        running = true;
        mSpeakerIsSelected = true;
        mMicIsSelected = true;
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Compatibility.setShowWhenLocked(this, true);

        setContentView(R.layout.call_screen_layout);
        runTimer();
        CallStatus = (TextView) findViewById(R.id.textView);
        ImageButton Mutemic = (ImageButton) findViewById(R.id.imageButton2);
        Speaker = (ImageButton)findViewById(R.id.imageButton);
        ImageButton Hangup = (ImageButton)findViewById(R.id.imageButton3);

        Hangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Manager.getCallManager().terminateCurrentCallOrConferenceOrAll();

            }
        });

        Mutemic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Mutemic.setSelected(!mMicIsSelected);
                mMicIsSelected = !mMicIsSelected;
                mCore.enableMic(!mCore.micEnabled());
            }
        });

        Speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Speaker.setSelected(!mSpeakerIsSelected);
                mSpeakerIsSelected = !mSpeakerIsSelected;
                toggleSpeaker();
            }
        });
        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onMessageReceived(Core core, ChatRoom cr, ChatMessage message) {
                        updateMissedChatCount();
                    }

                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.Error) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotFound) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_user_not_found),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotAcceptable) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_incompatible_media),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.Busy) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_user_busy),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (message != null) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_unknown) + " - " + message,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else if (state == Call.State.End) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        CallActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else if (state == Call.State.Connected) {
                        }

                        if (state == Call.State.End || state == Call.State.Released) {
                            finish();
                        }
                    }
                };

        mCore = Manager.getCore();
        if (mCore != null) {
            boolean recordAudioPermissionGranted =
                    checkPermission(Manifest.permission.RECORD_AUDIO);
            if (!recordAudioPermissionGranted) {
                Log.w("[Call Activity] RECORD_AUDIO permission denied, muting microphone");
                mCore.enableMic(false);
            }

            Call call = mCore.getCurrentCall();
            boolean videoEnabled =
                    LinphonePreferences.instance().isVideoEnabled()
                            && call != null
                            && call.getCurrentParams().videoEnabled();

        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // This also must be done here in case of an outgoing caller accepted
        // before user granted or denied permissions
        // or if an incoming caller was answer from the notification
        checkAndRequestCallPermissions();

        mCore = Manager.getCore();
        if (mCore != null) {
            mCore.setNativeVideoWindowId(mRemoteVideo);
            mCore.setNativePreviewWindowId(mLocalPreview);
            mCore.addListener(mListener);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onResume() {
        super.onResume();

        mAudioManager = Manager.getAudioManager();

        ContactsManager.getInstance().addContactsListener(this);


        if (mCore.getCallsNb() == 0) {
            Log.w("[Call Activity] Resuming but no caller found...");
            finish();
        }

        if (LinphoneService.isReady()) LinphoneService.instance().destroyOverlay();
    }

    @Override
    protected void onPause() {
   /*     ContactsManager.getInstance().removeContactsListener(this);
        Manager.getCallManager().setCallInterface(null);

        Core core = Manager.getCore();
        if (LinphonePreferences.instance().isOverlayEnabled()
                && core != null
                && core.getCurrentCall() != null) {
            Call caller = core.getCurrentCall();
            if (caller.getState() == Call.State.StreamsRunning) {
                // Prevent overlay creation if video caller is paused by remote
                if (LinphoneService.isReady()) LinphoneService.instance().createOverlay();
            }
        }
    */
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        Core core = Manager.getCore();
        if (core != null) {
            core.removeListener(mListener);
            core.setNativeVideoWindowId(null);
            core.setNativePreviewWindowId(null);
        }


        mCallTimer = null;

        mListener = null;
        mLocalPreview = null;
        mRemoteVideo = null;
        mStatsFragment = null;

        mButtons = null;
        mActiveCalls = null;
        mContactAvatar = null;
        mActiveCallHeader = null;
        mConferenceHeader = null;
        mCallsList = null;
        mCallPausedByRemote = null;
        mConferenceList = null;
        mMicro = null;
        mSpeaker = null;
        mVideo = null;
        mPause = null;
        mSwitchCamera = null;
        mRecordingInProgress = null;
        mExtrasButtons = null;
        mAddCall = null;
        mTransferCall = null;
        mRecordCall = null;
        mConference = null;
        mAudioRoute = null;
        mRouteEarpiece = null;
        mRouteSpeaker = null;
        mRouteBluetooth = null;
        mContactName = null;
        mMissedMessages = null;
        mVideoInviteInProgress = null;
        mCallUpdateDialog = null;

        super.onDestroy();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mAudioManager.onKeyVolumeAdjust(keyCode)) return true;
        return super.onKeyDown(keyCode, event);
    }



    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // Permission not granted, won't change anything

        if (requestCode == ALL_PERMISSIONS) {
            for (int index = 0; index < permissions.length; index++) {
                int granted = grantResults[index];
                if (granted == PackageManager.PERMISSION_GRANTED) {
                    String permission = permissions[index];
                    if (Manifest.permission.RECORD_AUDIO.equals(permission)) {
                    } else if (Manifest.permission.CAMERA.equals(permission)) {
                        LinphoneUtils.reloadVideoDevices();
                    }
                }
            }
        } else {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) return;
            switch (requestCode) {
                case CAMERA_TO_TOGGLE_VIDEO:
                    LinphoneUtils.reloadVideoDevices();
                    toggleVideo();
                    break;
                case MIC_TO_DISABLE_MUTE:

                    break;
                case WRITE_EXTERNAL_STORAGE_FOR_RECORDING:
                    break;
                case CAMERA_TO_ACCEPT_UPDATE:
                    LinphoneUtils.reloadVideoDevices();
                    acceptCallUpdate(true);
                    break;
            }
        }
    }

    private boolean checkPermission(String permission) {
        int granted = getPackageManager().checkPermission(permission, getPackageName());
        Log.i(
                "[Permission] "
                        + permission
                        + " permission is "
                        + (granted == PackageManager.PERMISSION_GRANTED ? "granted" : "denied"));
        return granted == PackageManager.PERMISSION_GRANTED;
    }

    private boolean checkAndRequestPermission(String permission, int result) {
        if (!checkPermission(permission)) {
            Log.i("[Permission] Asking for " + permission);
            ActivityCompat.requestPermissions(this, new String[] {permission}, result);
            return false;
        }
        return true;
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

        Call call = mCore.getCurrentCall();
        if (LinphonePreferences.instance().shouldInitiateVideoCall()
                || (LinphonePreferences.instance().shouldAutomaticallyAcceptVideoRequests()
                        && call != null
                        && call.getRemoteParams().videoEnabled())) {
            if (camera != PackageManager.PERMISSION_GRANTED) {
                Log.i("[Permission] Asking for camera");
                permissionsList.add(Manifest.permission.CAMERA);
            }
        }

        if (permissionsList.size() > 0) {
            String[] permissions = new String[permissionsList.size()];
            permissions = permissionsList.toArray(permissions);
            ActivityCompat.requestPermissions(this, permissions, ALL_PERMISSIONS);
        }
    }

    @Override
    public void onContactsUpdated() {
        setCurrentCallContactInformation();
    }

    @Override
    public void onUserLeaveHint() {
        if (mCore == null) return;
        Call call = mCore.getCurrentCall();
        if (call == null) return;
        boolean videoEnabled =
                LinphonePreferences.instance().isVideoEnabled()
                        && call.getCurrentParams().videoEnabled();
        if (videoEnabled && getResources().getBoolean(R.bool.allow_pip_while_video_call)) {
            Compatibility.enterPipMode(this);
        }
    }

    public void resetCallControlsHidingTimer() {
        LinphoneUtils.removeFromUIThreadDispatcher(mHideControlsRunnable);
        LinphoneUtils.dispatchOnUIThreadAfter(
                mHideControlsRunnable, SECONDS_BEFORE_HIDING_CONTROLS);
    }

    // BUTTONS


    private void updateButtons() {
        Call call = mCore.getCurrentCall();


    }



    private void toggleVideo() {
        Call call = mCore.getCurrentCall();
        if (call == null) return;

        mVideoInviteInProgress.setVisibility(View.VISIBLE);
        mVideo.setEnabled(false);
        if (call.getCurrentParams().videoEnabled()) {
            Manager.getCallManager().removeVideo();
        } else {
            Manager.getCallManager().addVideo();
        }
    }

    private void togglePause(Call call) {
        if (call == null) return;

        if (call == mCore.getCurrentCall()) {
            call.pause();
            mPause.setSelected(true);
        } else if (call.getState() == Call.State.Paused) {
            call.resume();
            mPause.setSelected(false);
        }
    }

    private void toggleAudioRouteButtons() {
        mAudioRoute.setSelected(!mAudioRoute.isSelected());
        mRouteEarpiece.setVisibility(mAudioRoute.isSelected() ? View.VISIBLE : View.GONE);
        mRouteSpeaker.setVisibility(mAudioRoute.isSelected() ? View.VISIBLE : View.GONE);
        mRouteBluetooth.setVisibility(mAudioRoute.isSelected() ? View.VISIBLE : View.GONE);
    }

    private void toggleExtrasButtons() {
        mExtrasButtons.setSelected(!mExtrasButtons.isSelected());
        mAddCall.setVisibility(mExtrasButtons.isSelected() ? View.VISIBLE : View.GONE);
        mTransferCall.setVisibility(mExtrasButtons.isSelected() ? View.VISIBLE : View.GONE);
        mRecordCall.setVisibility(mExtrasButtons.isSelected() ? View.VISIBLE : View.GONE);
        mConference.setVisibility(mExtrasButtons.isSelected() ? View.VISIBLE : View.GONE);
    }


    private void updateMissedChatCount() {
        int count = 0;
        if (mCore != null) {
            count = mCore.getUnreadChatMessageCountFromActiveLocals();
        }


    }

    private void updateButtonsVisibility(boolean visible) {
        if (mActiveCalls != null) mActiveCalls.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (mButtons != null) mButtons.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void makeButtonsVisibleTemporary() {
        updateButtonsVisibility(true);
        resetCallControlsHidingTimer();
    }

    // VIDEO RELATED

    private void showVideoControls(boolean videoEnabled) {
        mContactAvatar.setVisibility(videoEnabled ? View.GONE : View.VISIBLE);
        mRemoteVideo.setVisibility(videoEnabled ? View.VISIBLE : View.GONE);
        mLocalPreview.setVisibility(videoEnabled ? View.VISIBLE : View.GONE);
        mSwitchCamera.setVisibility(videoEnabled ? View.VISIBLE : View.INVISIBLE);
        updateButtonsVisibility(!videoEnabled);
        mVideo.setSelected(videoEnabled);
        Manager.getInstance().enableProximitySensing(!videoEnabled);

        if (!videoEnabled) {
            LinphoneUtils.removeFromUIThreadDispatcher(mHideControlsRunnable);
        }
    }

    private void updateInterfaceDependingOnVideo() {
        Call call = mCore.getCurrentCall();
        if (call == null) {
            showVideoControls(false);
            return;
        }

        mVideoInviteInProgress.setVisibility(View.GONE);
        mVideo.setEnabled(
                LinphonePreferences.instance().isVideoEnabled()
                        && call != null
                        && !call.mediaInProgress());

        boolean videoEnabled =
                LinphonePreferences.instance().isVideoEnabled()
                        && call != null
                        && call.getCurrentParams().videoEnabled();
        showVideoControls(videoEnabled);
    }

    private boolean moveLocalPreview(View view, MotionEvent motionEvent) {
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mPreviewX = view.getX() - motionEvent.getRawX();
                mPreviewY = view.getY() - motionEvent.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                view.animate()
                        .x(motionEvent.getRawX() + mPreviewX)
                        .y(motionEvent.getRawY() + mPreviewY)
                        .setDuration(0)
                        .start();
                break;
            default:
                return false;
        }
        return true;
    }

    // NAVIGATION

    private void goBackToDialer() {
        Intent intent = new Intent();
        intent.setClass(this, DialerActivity.class);
        intent.putExtra("isTransfer", false);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    private void goBackToDialerAndDisplayTransferButton() {
        Intent intent = new Intent();
        intent.setClass(this, DialerActivity.class);
        intent.putExtra("isTransfer", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    // CALL UPDATE

    private void createTimerForDialog(long time) {
        mCallUpdateCountDownTimer =
                new CountDownTimer(time, 1000) {
                    public void onTick(long millisUntilFinished) {}

                    public void onFinish() {
                        if (mCallUpdateDialog != null) {
                            mCallUpdateDialog.dismiss();
                            mCallUpdateDialog = null;
                        }
                        acceptCallUpdate(false);
                    }
                }.start();
    }

    private void acceptCallUpdate(boolean accept) {
        if (mCallUpdateCountDownTimer != null) {
            mCallUpdateCountDownTimer.cancel();
        }
        Manager.getCallManager().acceptCallUpdate(accept);
    }


    private void toggleSpeaker() {
        mIsSpeakerEnabled = !mIsSpeakerEnabled;
        Speaker.setSelected(mIsSpeakerEnabled);
        if (mIsSpeakerEnabled) {
            Manager.getAudioManager().routeAudioToSpeaker();
        } else {
            Manager.getAudioManager().routeAudioToEarPiece();
        }

    }
    private void showAcceptCallUpdateDialog() {
        mCallUpdateDialog = new Dialog(this);
        mCallUpdateDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mCallUpdateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        mCallUpdateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        mCallUpdateDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Drawable d = new ColorDrawable(ContextCompat.getColor(this, R.color.dark_grey_color));
        d.setAlpha(200);
        mCallUpdateDialog.setContentView(R.layout.dialog);
        mCallUpdateDialog
                .getWindow()
                .setLayout(
                        WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
        mCallUpdateDialog.getWindow().setBackgroundDrawable(d);

        TextView customText = mCallUpdateDialog.findViewById(R.id.dialog_message);
        customText.setText(getResources().getString(R.string.add_video_dialog));
        mCallUpdateDialog.findViewById(R.id.dialog_delete_button).setVisibility(View.GONE);
        Button accept = mCallUpdateDialog.findViewById(R.id.dialog_ok_button);
        accept.setVisibility(View.VISIBLE);
        accept.setText(R.string.accept);
        Button cancel = mCallUpdateDialog.findViewById(R.id.dialog_cancel_button);
        cancel.setText(R.string.decline);

        accept.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (checkPermission(Manifest.permission.CAMERA)) {
                            acceptCallUpdate(true);
                        } else {
                            checkAndRequestPermission(
                                    Manifest.permission.CAMERA, CAMERA_TO_ACCEPT_UPDATE);
                        }
                        mCallUpdateDialog.dismiss();
                        mCallUpdateDialog = null;
                    }
                });

        cancel.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        acceptCallUpdate(false);
                        mCallUpdateDialog.dismiss();
                        mCallUpdateDialog = null;
                    }
                });
        mCallUpdateDialog.show();
    }

    // CONFERENCE

    private void displayConferenceCall(final Call call) {
        LinearLayout conferenceCallView =
                (LinearLayout)
                        LayoutInflater.from(this)
                                .inflate(R.layout.call_conference_cell, null, false);

        TextView contactNameView = conferenceCallView.findViewById(R.id.contact_name);
        LinphoneContact contact =
                ContactsManager.getInstance().findContactFromAddress(call.getRemoteAddress());
        if (contact != null) {
            ContactAvatar.displayAvatar(
                    contact, conferenceCallView.findViewById(R.id.avatar_layout), true);
            contactNameView.setText(contact.getFullName());
        } else {
            String displayName = LinphoneUtils.getAddressDisplayName(call.getRemoteAddress());
            ContactAvatar.displayAvatar(
                    displayName, conferenceCallView.findViewById(R.id.avatar_layout), true);
            contactNameView.setText(displayName);
        }

        Chronometer timer = conferenceCallView.findViewById(R.id.call_timer);
        timer.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
        timer.start();

        ImageView removeFromConference =
                conferenceCallView.findViewById(R.id.remove_from_conference);
        removeFromConference.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Manager.getCallManager().removeCallFromConference(call);
                    }
                });

        mConferenceList.addView(conferenceCallView);
    }

    private void displayPausedConference() {
        LinearLayout pausedConferenceView =
                (LinearLayout)
                        LayoutInflater.from(this)
                                .inflate(R.layout.call_conference_paused_cell, null, false);

        ImageView conferenceResume = pausedConferenceView.findViewById(R.id.conference_resume);
        conferenceResume.setSelected(true);
        conferenceResume.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Manager.getCallManager().resumeConference();
                        updateCallsList();
                    }
                });

        mCallsList.addView(pausedConferenceView);
    }

    // OTHER

    private void updateCallsList() {
        Call currentCall = mCore.getCurrentCall();
        if (currentCall != null) {
            setCurrentCallContactInformation();
        }

        boolean callThatIsNotCurrentFound = false;
        boolean pausedConferenceDisplayed = false;
        boolean conferenceDisplayed = false;
        mCallsList.removeAllViews();
        mConferenceList.removeAllViews();

        for (Call call : mCore.getCalls()) {
            if (call != null && call.getConference() != null) {
                if (mCore.isInConference()) {
                    displayConferenceCall(call);
                    conferenceDisplayed = true;
                } else if (!pausedConferenceDisplayed) {
                    displayPausedConference();
                    pausedConferenceDisplayed = true;
                }
            } else if (call != null && call != currentCall) {
                Call.State state = call.getState();
                if (state == Call.State.Paused
                        || state == Call.State.PausedByRemote
                        || state == Call.State.Pausing) {
                    displayPausedCall(call);
                    callThatIsNotCurrentFound = true;
                }
            }
        }

        mCallsList.setVisibility(
                pausedConferenceDisplayed || callThatIsNotCurrentFound ? View.VISIBLE : View.GONE);
        mActiveCallHeader.setVisibility(
                currentCall != null && !conferenceDisplayed ? View.VISIBLE : View.GONE);
        mConferenceHeader.setVisibility(conferenceDisplayed ? View.VISIBLE : View.GONE);
        mConferenceList.setVisibility(mConferenceHeader.getVisibility());
    }

    private void displayPausedCall(final Call call) {
        LinearLayout callView =
                (LinearLayout)
                        LayoutInflater.from(this).inflate(R.layout.call_inactive_row, null, false);

        TextView contactName = callView.findViewById(R.id.contact_name);
        Address address = call.getRemoteAddress();
        LinphoneContact contact = ContactsManager.getInstance().findContactFromAddress(address);
        if (contact == null) {
            String displayName = LinphoneUtils.getAddressDisplayName(address);
            contactName.setText(displayName);
            ContactAvatar.displayAvatar(displayName, callView.findViewById(R.id.avatar_layout));
        } else {
            contactName.setText(contact.getFullName());
            ContactAvatar.displayAvatar(contact, callView.findViewById(R.id.avatar_layout));
        }

        Chronometer timer = callView.findViewById(R.id.call_timer);
        timer.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
        timer.start();

        ImageView resumeCall = callView.findViewById(R.id.call_pause);
        resumeCall.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        togglePause(call);
                    }
                });

        mCallsList.addView(callView);
    }

    private void updateCurrentCallTimer() {
        Call call = mCore.getCurrentCall();
        if (call == null) return;

        mCallTimer.setBase(SystemClock.elapsedRealtime() - 1000 * call.getDuration());
        mCallTimer.start();
    }

    private void setCurrentCallContactInformation() {
        updateCurrentCallTimer();

        Call call = mCore.getCurrentCall();
        if (call == null) return;

        LinphoneContact contact =
                ContactsManager.getInstance().findContactFromAddress(call.getRemoteAddress());
        if (contact != null) {
            ContactAvatar.displayAvatar(contact, mContactAvatar, true);
            mContactName.setText(contact.getFullName());
        } else {
            String displayName = LinphoneUtils.getAddressDisplayName(call.getRemoteAddress());
            ContactAvatar.displayAvatar(displayName, mContactAvatar, true);
            mContactName.setText(displayName);
        }
    }

    private void runTimer()
    {

        // Get the text view.
        final TextView timeView
                = (TextView)findViewById(
                R.id.textView);

        // Creates a new Handler
        final Handler handler
                = new Handler();

        // Call the post() method,
        // passing in a new Runnable.
        // The post() method processes
        // code without a delay,
        // so the code in the Runnable
        // will run almost immediately.
        handler.post(new Runnable() {
            @Override

            public void run()
            {
                int hours = seconds / 3600;
                int minutes = (seconds % 3600) / 60;
                int secs = seconds % 60;

                // Format the seconds into hours, minutes,
                // and seconds.
                String time
                        = String
                        .format(Locale.getDefault(),
                                "%d:%02d:%02d", hours,
                                minutes, secs);

                // Set the text view text.
                timeView.setText(time);

                // If running is true, increment the
                // seconds variable.
                if (running) {
                    seconds++;
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }
}
