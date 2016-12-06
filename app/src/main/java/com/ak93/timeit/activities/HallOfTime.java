package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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
import com.ak93.timeit.objects.Score;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * Created by Anže Kožar on 26.9.2016.
 * This activity displays game statistics and score
 */

public class HallOfTime extends Activity implements AppConstants,
        View.OnClickListener, GoogleApiClient.OnConnectionFailedListener,
        GoogleApiClient.ConnectionCallbacks {

    private TextView bestScoreText,bestRowScoreText,bestLvlText,averageTimerScoreText;

    private SharedPreferences scorePreferences,mainPreferences;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    private static final String TAG = "HallOfTime";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        //Retrieve shared preferences
        scorePreferences =
                getSharedPreferences(getString(R.string.score_preferences),Context.MODE_PRIVATE);
        mainPreferences =
                getSharedPreferences(getString(R.string.main_preferences),Context.MODE_PRIVATE);

        setContentView(R.layout.activity_scoreboard);

        init();
    }

    /**
     * Initialize activity UI
     */
    private void init(){
        //Request ad
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        bestScoreText = (TextView)findViewById(R.id.bestScoreText);
        bestRowScoreText = (TextView)findViewById(R.id.bestTimerScoreText);
        bestLvlText = (TextView)findViewById(R.id.bestLevelText);
        averageTimerScoreText = (TextView)findViewById(R.id.averageTimerScoreText);

        findViewById(R.id.backButton).setOnClickListener(this);
        findViewById(R.id.achievements_button).setOnClickListener(this);
        findViewById(R.id.resetScoreButton).setOnClickListener(this);

        populateHighscore();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Auto login to Google Play Games if auto login is enabled
        if(mainPreferences.getBoolean(getString(R.string.main_pref_autologin),true)) {
            //Log.i(TAG,"Auto connect google API");
            mGoogleApiClient.connect(); //Auto connect Play Games services
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //Stop google API client
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * Populate highscore fields
     */
    private void populateHighscore(){
        int bestScore = scorePreferences.getInt(getString(R.string.KEY_BEST_TOTAL_SCORE),0);
        int bestTimerScore = scorePreferences.getInt(getString(R.string.KEY_BEST_ROW_SCORE),0);
        int bestLevel = scorePreferences.getInt(getString(R.string.KEY_BEST_LVL),0)+1;

        long averageTimerTotalScore = scorePreferences.getLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_SCORE),0);
        long averageTimerTotalTimers = scorePreferences.getLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_TIMERS),1);
        int averageTimerScore = (int)(averageTimerTotalScore/averageTimerTotalTimers);

        bestScoreText.setText(String.valueOf(bestScore));
        bestRowScoreText.setText(String.valueOf(bestTimerScore));
        averageTimerScoreText.setText(String.valueOf(averageTimerScore));

        bestLvlText.setText(String.valueOf(bestLevel));
    }

    /**
     * Handle reset score button click
     */
    private void resetScore(){
        new AlertDialog.Builder(this)
                .setTitle("Reset score:")
                .setMessage(R.string.string_reset_score_alert_message)
                .setPositiveButton(R.string.string_reset_score_positive_button,
                        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        scorePreferences.edit().clear().apply();

                        // reload highscore
                        populateHighscore();
                    }
                })
                .setNegativeButton(R.string.string_reset_score_negative_button,null)
                .show();
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
                    mainPreferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
                    mGoogleApiClient.connect();
                } else {
                    //sign in failed, don't autologin again
                    //Log.i(TAG, "Result code NOT_OK");
                    if (!GameUtils.hasNetworkConnection(this)) {
                        Toast.makeText(this, "Active internet connection needed to log in!", Toast.LENGTH_LONG).show();
                    }
                    mainPreferences.edit().putBoolean(getString(R.string.main_pref_autologin), false).apply();
                }
                break;
            }
        }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.backButton:
                finish();
                break;
            case R.id.resetScoreButton:
                resetScore();
                break;
            case R.id.achievements_button:
                if(mGoogleApiClient.isConnected()) {
                    startActivityForResult(Games.Achievements.getAchievementsIntent(mGoogleApiClient), REQUEST_CODE_ACHIEVEMENTS);
                }else{
                    //notify the user that he needs to connect
                    Toast.makeText(getApplicationContext(),"Google Games is not connected!",Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.i(TAG, "onConnectionFailed() called, result: " + connectionResult);
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN);
            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //show achievements button after the client has successfully been connected
        findViewById(R.id.achievements_button).setVisibility(View.VISIBLE);
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }
}
