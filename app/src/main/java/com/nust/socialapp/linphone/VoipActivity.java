package com.nust.socialapp.linphone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;



import org.linphone.core.Call;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.tools.Log;

import com.nust.socialapp.R;
import com.nust.socialapp.linphone.notifications.NotificationsManager;

/**
 * Name: VoipActivity
 * Description: Enables an App to App calling between two phones
 */
public class VoipActivity extends AppCompatActivity {
    int Request_voice = 1;
    private String s1 = "";
    static TextView status;
    public static VoipActivity instance = null;
    private ImageView LED;
    private TextView tryAgain;
    private ImageButton TryAgain;
    private CoreListenerStub mListener;
    private EditText T;

    NotificationsManager mNotificationManager;
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intent = getIntent();
        String value = intent.getStringExtra("key");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.voip);
        initComponents();
        TryAgain = (ImageButton) findViewById(R.id.imageButton);
        tryAgain = (TextView) findViewById(R.id.TryAgain);
        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onRegistrationStateChanged(
                            final Core core,
                            final ProxyConfig proxy,
                            final RegistrationState state,
                            String smessage) {
                        if (core.getProxyConfigList() == null) {
                            showNoAccountConfigured();
                            return;
                        }

                        if ((core.getDefaultProxyConfig() != null
                                && core.getDefaultProxyConfig().equals(proxy))
                                || core.getDefaultProxyConfig() == null) {
                            LED.setImageResource(getStatusIconResource(state));
                            status.setText(getStatusIconText(state));
                        }

                        try {
                            status.setOnClickListener(
                                    new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Core core = Manager.getCore();
                                            if (core != null) {
                                                core.refreshRegisters();
                                            }
                                        }
                                    });
                        } catch (IllegalStateException ise) {
                            Log.e(ise);
                        }}

                    };

    }

    @Override
    public void onResume() {
        super.onResume();
        Core core = Manager.getCore();
        if (core != null) {
            core.addListener(mListener);
            ProxyConfig lpc = core.getDefaultProxyConfig();
            if (lpc != null) {
                mListener.onRegistrationStateChanged(core, lpc, lpc.getState(), null);
            }

            Call call = core.getCurrentCall();
            if (call != null || core.getConferenceSize() > 1 || core.getCallsNb() > 0) {
                if (call != null) {
                // We are obviously connected
                if (core.getDefaultProxyConfig() == null) {
                    LED.setImageResource(R.drawable.led_light);
                    status.setText(getString(R.string.no_account));
                } else {
                    LED.setImageResource(
                            getStatusIconResource(core.getDefaultProxyConfig().getState()));
                    status.setText(getStatusIconText(core.getDefaultProxyConfig().getState()));
                }
                }
            }
        }
    }

    public void initComponents() {


        LED = (ImageView) findViewById(R.id.status_led);
        status = (TextView) findViewById(R.id.status_text);
        T = (EditText) findViewById(R.id.textView3);
        Button B1 = (Button) findViewById(R.id.button1);
        Button B2 = (Button) findViewById(R.id.button2);

        Button B3 = (Button) findViewById(R.id.button3);

        Button B4 = (Button) findViewById(R.id.button4);
        Button B5 = (Button) findViewById(R.id.button5);
        Button B6 = (Button) findViewById(R.id.button6);
        Button B7 = (Button) findViewById(R.id.button7);
        Button B8 = (Button) findViewById(R.id.button8);
        Button B9 = (Button) findViewById(R.id.button9);
        Button B0 = (Button) findViewById(R.id.button0);
        Button B_hash = (Button) findViewById(R.id.button_hash);
        Button B_star = (Button) findViewById(R.id.button_star);
        ImageButton Call = (ImageButton) findViewById(R.id.imageButton4);

        final FloatingActionButton Back = (FloatingActionButton) findViewById(R.id.floatingActionButton2);


        B1.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "1";
                T.setText(s1);
            }
        });
        B2.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "2";
                T.setText(s1);
            }
        });

        B3.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "3";
                T.setText(s1);
            }
        });
        B4.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "4";
                T.setText(s1);
            }
        });
        B5.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "5";
                T.setText(s1);
            }
        });
        B6.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "6";
                T.setText(s1);
            }
        });
        B7.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "7";
                T.setText(s1);
            }
        });
        B8.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "8";
                T.setText(s1);
            }
        });
        B9.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "9";
                T.setText(s1);
            }
        });
        B0.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "0";
                T.setText(s1);
            }
        });
        B_hash.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "#";
                T.setText(s1);
            }
        });
        B_star.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                Back.setVisibility(View.VISIBLE);
                s1 += "*";
                T.setText(s1);
            }
        });
        Back.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                s1 = s1.substring(0, s1.length() - 1);
                T.setText(s1);
                if (s1.isEmpty())
                    Back.setVisibility(View.INVISIBLE);
            }
        });

        Call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String a = String.valueOf(T.getText());
                if(a.isEmpty()){
                    Toast.makeText(getApplicationContext(), "Enter a valid SIP address", Toast.LENGTH_LONG).show();
                }
                else {
                    if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                        Core core = Manager.getCore();
                        if (core != null) {
                            core.enableMic(true);
                        }
                        Manager.getCallManager().newOutgoingCall("sip:" + T.getText() + "@sip.linphone.org", null);

                        Toast.makeText(getApplicationContext(), "Calling", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(VoipActivity.this, RingerActivity.class));
                    } else {
                        requestAudioPermission();
                    }
                }
            }
        });
    }

private void requestAudioPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)){
            new AlertDialog.Builder(this)
                    .setTitle("Make Call")
                    .setMessage("Allow " + getString(R.string.app_name) + " to access your phone microphone.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ActivityCompat.requestPermissions(VoipActivity.this,new String[]{Manifest.permission.RECORD_AUDIO}, Request_voice);
                        }
                    }).setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    dialogInterface.dismiss();
                }
            }).create().show();
        }else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, Request_voice);
        }
}

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == Request_voice){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Core core = Manager.getCore();
                if (core != null) {
                    core.enableMic(true);
                }
                Manager.getCallManager().newOutgoingCall("sip:" + T.getText() + "@sip.linphone.org", null);
                Toast.makeText(getApplicationContext(), "Calling", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(VoipActivity.this, CallInterfaceActivity.class));
            }
            else {
                Toast.makeText(getApplicationContext(),"Call Cant be forwarded without accessing microphone", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private int getStatusIconResource(RegistrationState state) {
        try {
            if (state == RegistrationState.Ok) {
                return R.drawable.led_connected;
            } else if (state == RegistrationState.Progress) {
                return R.drawable.led_connecting;
            } else if (state == RegistrationState.Failed) {
                return R.drawable.led_light;
            } else {
                return R.drawable.led_light;
            }
        } catch (Exception e) {
            Log.e(e);
        }

        return R.drawable.led_light;
    }

    private String getStatusIconText(RegistrationState state) {
        android.content.Context context = this.getApplicationContext();
        try {
            if (state == RegistrationState.Ok) {
                return context.getString(R.string.status_connected);
            } else if (state == RegistrationState.Progress) {
                return context.getString(R.string.status_in_progress);
            } else if (state == RegistrationState.Failed) {
                return context.getString(R.string.status_error);
            } else {
                return context.getString(R.string.status_not_connected);
            }
        } catch (Exception e) {
            Log.e(e);
        }
        return context.getString(R.string.status_not_connected);
    }
        private void showNoAccountConfigured() {
            LED.setImageResource(R.drawable.led_light);
            status.setText(getString(R.string.no_account));
        }

}
