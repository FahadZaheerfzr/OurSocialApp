package com.nust.socialapp.linphone;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Toast;


import com.nust.socialapp.R;

import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Reason;
import org.linphone.core.tools.Log;


public class RingerActivity extends Activity {
    private Call calling;
    private CoreListenerStub mListener;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ringer_layout);
        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onCallStateChanged(
                            Core core, Call call, Call.State state, String message) {
                        if (state == Call.State.Error) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotFound) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_user_not_found),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.NotAcceptable) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_incompatible_media),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (call.getErrorInfo().getReason() == Reason.Busy) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_user_busy),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            } else if (message != null) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_unknown) + " - " + message,
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
                        } else if (state == Call.State.End) {
                            // Convert Core message for internalization
                            if (call.getErrorInfo().getReason() == Reason.Declined) {
                                Toast.makeText(
                                        RingerActivity.this,
                                        getString(R.string.error_call_declined),
                                        Toast.LENGTH_SHORT)
                                        .show();
                            }
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

        calling = null;

        // Only one caller ringing at a time is allowed
        if (Manager.getCore() != null) {
            for (Call call : Manager.getCore().getCalls()) {
                Call.State cstate = call.getState();
                if (Call.State.OutgoingInit == cstate
                        || Call.State.OutgoingProgress == cstate
                        || Call.State.OutgoingRinging == cstate
                        || Call.State.OutgoingEarlyMedia == cstate) {
                    calling = call;
                    break;
                }
            }
        }
        if (calling == null) {
            Log.e("[Call Outgoing Activity] Couldn't find outgoing caller");
            finish();
            return;
        }
    }
}
