package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ak93.timeit.AppConstants;
import com.ak93.timeit.GameUtils;
import com.ak93.timeit.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Game;
import com.google.android.gms.games.Games;

/**
 * Created by Anže Kožar on 3.11.2016.
 */

public class AboutActivity extends Activity implements AppConstants, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private SharedPreferences preferences;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "AboutActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        preferences = getSharedPreferences(getString(R.string.main_preferences),MODE_PRIVATE);

        setContentView(R.layout.activity_about);

        init();
    }

    private void init(){
        ImageView backButton = (ImageView)findViewById(R.id.backButton);
        backButton.setOnClickListener(this);

        ImageView fbButton = (ImageView)findViewById(R.id.fb_icon);
        fbButton.setOnClickListener(this);

        ImageView githubButton = (ImageView)findViewById(R.id.github_icon);
        githubButton.setOnClickListener(this);

        ImageView gplayButton = (ImageView)findViewById(R.id.gplay_icon);
        gplayButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(preferences.getBoolean(getString(R.string.main_pref_autologin),true)) {
            Log.i(TAG,"Auto connect google API");
            mGoogleApiClient.connect(); //Auto connect Play Games services
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.backButton:
                finish();
                break;
            case R.id.fb_icon:
                Intent fbIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.facebook.com/timeitgame/"));
                startActivity(fbIntent);
                break;
            case R.id.github_icon:
                Intent githubIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/nzkozar/TimeItRight_android"));
                startActivity(githubIntent);
                break;
            case R.id.gplay_icon:
                Intent gplayIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=com.ak93.timeit"));
                startActivity(gplayIntent);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i(TAG,"onActivityResult with requestCode= "+requestCode+" resultCode= "+resultCode);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN:
                Log.i(TAG, "onActivityResult with requestCode == REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN, responseCode="
                        + resultCode + ", intent=" + data);
                if (resultCode == RESULT_OK) {
                    Log.i(TAG,"Result CODE_OK");
                    //User has successfully logged in. From now on, log him in automatically
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
                    mGoogleApiClient.connect();
                } else {
                    //sign in failed, don't autologin again
                    Log.i(TAG,"Result code NOT_OK");
                    if(!GameUtils.hasNetworkConnection(this)){
                        Toast.makeText(this,"Active internet connection needed to log in!",Toast.LENGTH_LONG).show();
                    }
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),false).apply();
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected() called. Sign in successful!");
        preferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
        //Unlock about activity achievement
        GameUtils.unlockAchievement(mGoogleApiClient,R.string.achievement_sherlock_is_my_middle_name);

    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG,"onConnectionFailed(), result: " + connectionResult);
        Log.i(TAG,"onConnectionFailed() error_code: "+
                String.valueOf(connectionResult.getErrorCode()+" message: "+
                        String.valueOf(connectionResult.getErrorMessage())));
        if (connectionResult.hasResolution()) {
            Log.i(TAG,"onConnectionFailed(), hasResolution");
            try {
                switch (connectionResult.getErrorCode()){
                    case ConnectionResult.SIGN_IN_REQUIRED:
                        Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_SIGN_IN_REQUIRED");
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN);
                        break;
                    case ConnectionResult.RESOLUTION_REQUIRED:
                        Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_NOT_INSTALLED");
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER);
                        break;
                    case ConnectionResult.SIGN_IN_FAILED:
                        Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_SIGN_IN_FAILED");
                        break;
                }

            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleApiClient.connect();
            }
        }
    }
}

