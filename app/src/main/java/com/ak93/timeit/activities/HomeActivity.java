package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.GameUtils;
import com.ak93.timeit.R;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * Created by Anže Kožar on 30.2.2016.
 * Home activity serves as a main menu, containing the navigation buttons to other app screens
 */
public class HomeActivity extends Activity implements View.OnClickListener,AppConstants,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    private SharedPreferences preferences;

    private static final String TAG = "HomeActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        setContentView(R.layout.activity_home);

        init();

        //Get shared preferences
        preferences = getSharedPreferences(getString(R.string.main_preferences),MODE_PRIVATE);
        if(!preferences.getBoolean(getString(R.string.main_pref_tut_shown),false)){
            Intent intentTut = new Intent(getApplicationContext(),TutorialActivity.class);
            startActivityForResult(intentTut,REQUEST_CODE_TUTORIAL);
        }
    }

    /**
     * Initialize UI
     */
    private void init(){
        //Request ad
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        findViewById(R.id.but_play).setOnClickListener(this);
        findViewById(R.id.but_play_hard).setOnClickListener(this);
        findViewById(R.id.but_time_hall).setOnClickListener(this);
        findViewById(R.id.but_tutorial).setOnClickListener(this);
        findViewById(R.id.but_about).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Auto login to Google Play Games if auto login is enabled else show sign in button
        if(preferences.getBoolean(getString(R.string.main_pref_autologin),true)) {
            //Log.i(TAG,"Auto connect google API");
            mGoogleApiClient.connect(); //Auto connect Play Games services
        }else{
            showSignInBar(true);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Log.i(TAG,"onActivityResult with requestCode= "+requestCode+" resultCode= "+resultCode);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN: //Handle Google Play Games login
                //Log.i(TAG, "onActivityResult with requestCode == REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN, responseCode="
                        //+ resultCode + ", intent=" + data);
                if (resultCode == RESULT_OK) {
                    //Log.i(TAG,"Result CODE_OK");
                    //User has successfully logged in. From now on, log him in automatically
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
                    mGoogleApiClient.connect();
                } else {
                    //sign in failed, don't autologin again
                    //Log.i(TAG,"Result code NOT_OK");
                    if(!GameUtils.hasNetworkConnection(this)){
                        Toast.makeText(this,"Active internet connection needed to log in!",Toast.LENGTH_LONG).show();
                    }
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),false).apply();
                    showSignInBar(true);
                }
                break;
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER: //Handle Google Play Games login
                //Log.i(TAG, "onActivityResult with requestCode == REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER, responseCode="
                        //+ resultCode + ", intent=" + data);
                if(resultCode == RESULT_OK){

                }else {
                    //Log.i(TAG,"Result code NOT_OK");
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),false).apply();
                }
                break;
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.but_play:
                Intent intentPlay = new Intent(getApplicationContext(),ActivityLevels.class);
                startActivityForResult(intentPlay,REQUEST_CODE_PLAY);
                break;
            case R.id.but_play_hard:
                Intent intentPlayHard = new Intent(getApplicationContext(),ActivityLevels.class);
                startActivityForResult(intentPlayHard,REQUEST_CODE_PLAY_HARD);
                break;
            case R.id.but_time_hall:
                Intent intentScoreboard = new Intent(getApplicationContext(),HallOfTime.class);
                startActivityForResult(intentScoreboard,REQUEST_CODE_SCOREBOARD);
                break;
            case R.id.but_tutorial:
                Intent intentTut = new Intent(getApplicationContext(),TutorialActivity.class);
                startActivityForResult(intentTut,REQUEST_CODE_TUTORIAL);
                break;
            case R.id.but_about:
                Intent intentAbout = new Intent(getApplicationContext(),AboutActivity.class);
                startActivityForResult(intentAbout,REQUEST_CODE_ABOUT);
                break;
            case R.id.button_sign_in:
                // start the sign-in flow
                mGoogleApiClient.connect();
                break;
        }
    }

    /**
     * Shows the Google sign in button
     */
    private void showSignInBar(boolean show) {
        Log.d(TAG, "Showing sign in bar");
        if(show) {
            findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.sign_in_bar).setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Log.i(TAG, "onConnected() called. Sign in successful!");
        showSignInBar(false);
        //Enable auto login in the future
        preferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.i(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.d(TAG,"onConnectionFailed(), result: " + connectionResult);
        //Log.i(TAG,"onConnectionFailed() error_code: "+
                //String.valueOf(connectionResult.getErrorCode()+" message: "+
                        //String.valueOf(connectionResult.getErrorMessage())));
        if (connectionResult.hasResolution()) {
            //Log.i(TAG,"onConnectionFailed(), hasResolution");
            try {
                switch (connectionResult.getErrorCode()){
                    case ConnectionResult.SIGN_IN_REQUIRED:
                        //Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_SIGN_IN_REQUIRED");
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN);
                        break;
                    case ConnectionResult.RESOLUTION_REQUIRED:
                        //Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_NOT_INSTALLED");
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER);
                        break;
                    case ConnectionResult.SIGN_IN_FAILED:
                        //Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_SIGN_IN_FAILED");
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