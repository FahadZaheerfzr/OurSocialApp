package com.nust.socialapp.linphone;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;


import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;

import java.util.Locale;


import com.nust.socialapp.R;
import com.nust.socialapp.linphone.call.AndroidAudioManager;
import com.nust.socialapp.linphone.compatibility.Compatibility;

public class CallInterfaceActivity extends Activity {
    private int seconds = 0;

    private boolean executing;
    private boolean voiceaudioselected;
    private boolean micaudioisselected;

    private AndroidAudioManager mAudioManager;
    private Call mCall;
    private TextView CallStatus;
    private Core mCore;
    private ImageButton Speaker;
    boolean mIsSpeakerEnabled = false;
    private CoreListenerStub mListener;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Compatibility.setShowWhenLocked(this, true);
        Compatibility.setTurnScreenOn(this, true);
        setContentView(R.layout.call_screen_layout);
        mCore = Manager.getCore();
        init();
        executing = true;
        voiceaudioselected = true;
        micaudioisselected = true;

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.Error) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotFound) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_user_not_found),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotAcceptable) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_incompatible_media),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.Busy) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_user_busy),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (message != null) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_unknown) + " - " + message,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else if (state == Call.State.End) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        CallInterfaceActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else if (state == Call.State.Connected) {
                            runTimer();
                        }

                        if (state == Call.State.End || state == Call.State.Released) {
                            finish();
                        }
                    }
                };
    }


    @Override
    protected void onResume() {
        super.onResume();
        Core core = Manager.getCore();
        if (core != null) {
            core.addListener(mListener);
        }

        mCall = null;

        // Only one caller ringing at a time is allowed
        if (Manager.getCore() != null) {
            for (Call call : Manager.getCore().getCalls()) {
                Call.State cstate = call.getState();
                if (Call.State.OutgoingInit == cstate
                        || Call.State.OutgoingProgress == cstate
                        || Call.State.OutgoingRinging == cstate
                        || Call.State.OutgoingEarlyMedia == cstate) {
                    mCall = call;
                    break;
                }
            }
        }
        if (mCall == null) {
            Log.e("[Call Outgoing Activity] Couldn't find outgoing caller");
            finish();
            return;
        }
    }

    public void init(){
        CallStatus = (TextView) findViewById(R.id.textView);
        ImageButton Mutemic = (ImageButton) findViewById(R.id.imageButton2);
        Speaker = (ImageButton)findViewById(R.id.imageButton);
        ImageButton Hangup = (ImageButton)findViewById(R.id.imageButton3);

        Hangup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Manager.getCallManager().terminateCurrentCallOrConferenceOrAll();
                onDestroy();

            }
        });

        Mutemic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Mutemic.setSelected(!micaudioisselected);
                micaudioisselected = !micaudioisselected;
                mCore.enableMic(!mCore.micEnabled());
            }
        });

        Speaker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Speaker.setSelected(!voiceaudioselected);
                voiceaudioselected = !voiceaudioselected;
                toggleSpeaker();
            }
        });
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
                if (executing) {
                    seconds++;
                }

                // Post the code again
                // with a delay of 1 second.
                handler.postDelayed(this, 1000);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
