package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.ak93.timeit.AchievementListener;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.GameUtils;
import com.ak93.timeit.PlayListAdapter;
import com.ak93.timeit.PlayListLayoutManager;
import com.ak93.timeit.R;
import com.ak93.timeit.objects.Score;
import com.ak93.timeit.views.FontView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import java.util.ArrayList;

/**
 * Created by Anže Kožar on 28/02/2016.
 * This activity handles main gameplay.
 */
public class Play extends Activity implements AppConstants, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        AchievementListener {

    //Widgets and stuff
    private TextView textLevel;
    private TextView buttonNextLevel;
    private RecyclerView listPlayFields;
    private ArrayList<Long> levelTimers = new ArrayList<>();
    private PlayListAdapter playListAdapter;

    //vars
    private short activeRow = -1, lvl = 0, startLvl;
    private CountDownTimer timer, nextLvlCountdown; //Game timer, Inter screen countdown till next level
    private long time; //Current time in millis
    private Score gameScore; //Score object for this game session

    //Achievement vars
    private boolean gamePlayed = false; //Has a game been played
    private long totalTimersScore, totalTimers; //For use of calculating the average timer score

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    private SharedPreferences preferences,scorePreferences;
    private Resources resources;

    private static final String TAG = "Play";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        //Retrieve resources and shared preferences
        resources = getResources();
        preferences = getSharedPreferences(getString(R.string.main_preferences),MODE_PRIVATE);
        scorePreferences = getSharedPreferences(getString(R.string.score_preferences),MODE_PRIVATE);

        //Retrieve average timer score fields
        totalTimersScore = preferences.getLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_SCORE),0);
        totalTimers = preferences.getLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_TIMERS),0);

        //Initialize AdMob adds api
        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        setResult(RESULT_CODE_CANCELED);

        //Retrieve the starting level number
        Bundle extras = getIntent().getExtras();
        if(extras!=null) {
            startLvl = (short)extras.getInt(getString(R.string.KEY_START_FROM_LVL), 0);
            lvl=startLvl;
        }

        init();
    }

    @Override
    protected void onStart() {
        super.onStart();
        //Auto login to Google Play Games if auto login is enabled
        if(preferences.getBoolean(getString(R.string.main_pref_autologin),true)) {
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

    @Override
    public void finish() {
        //save average timer score to preferences
        scorePreferences.edit().putLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_SCORE),
                totalTimersScore).apply();
        scorePreferences.edit().putLong(getString(R.string.KEY_AVERAGE_TIMER_TOTAL_TIMERS),
                totalTimers).apply();
        //drop the achievement listener
        gameScore.setAchievementListener(null);
        //Return this game's level scores, to be updated on ActivityLevels
        Intent intent = new Intent();
        intent.putExtra("levelScores",gameScore.getLevelScores());
        setResult(RESULT_CODE_CANCELED,intent);
        super.finish();
    }

    /**
     * Init Play activity
     */
    private void init(){
        gameScore = new Score(this);
        gameScore.setAchievementListener(this);
        createLvl(lvl);
    }

    /**
     * Changes the content view to play list of a level. Prepares all necessary game level parameters
     * @param lvl level to create
     */
    private void createLvl(int lvl){
        //Log.i(TAG,"NEW LEVEL "+lvl);
        activeRow = -1;
        gameScore.updateScore(lvl,0);
        setContentView(R.layout.activity_play);
        textLevel = (TextView)findViewById(R.id.play_level_text);
        TextView buttonTime = (TextView) findViewById(R.id.but_timeit);
        buttonTime.setOnClickListener(this);

        listPlayFields = (RecyclerView)findViewById(R.id.play_list);
        listPlayFields.setHasFixedSize(true);
        PlayListLayoutManager layoutManager = new PlayListLayoutManager(this);
        listPlayFields.setLayoutManager(layoutManager);

        setTimer(0);
        populateList(lvl);
        String levelText = "lvl "+(lvl+1)+" "+gameScore.getTotalScore();
        textLevel.setText(levelText);

        ImageView backButton = (ImageView) findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
    }

    /**
     * Create the next level
     */
    private void nextLvl(){
        lvl++;
        createLvl(lvl);
    }

    /**
     * Populate play list with timers for this level
     * @param lvl Index of game level to set up
     */
    private void populateList(int lvl){
        levelTimers.clear();
        for(int i = 0;i<TIMER_DURATIONS[lvl].length;i++){
            levelTimers.add(TIMER_DURATIONS[lvl][i]);
        }
        //populate recycler view with new data
        if(playListAdapter == null) {
            playListAdapter = new PlayListAdapter(this, levelTimers);
        }else {
            playListAdapter.setData(levelTimers);
        }
        listPlayFields.setAdapter(playListAdapter);

    }

    /**
     * Handle "Time!" button clicks
     */
    private void timeIt(){
        long timeDiff = time - TIMER_DURATIONS[lvl][activeRow + 1];
        if(activeRow>-1 && activeRow < TIMER_DURATIONS[lvl].length-2){
            midRow(timeDiff);
        }else if(activeRow==-1){
            firstRow();
        }else{
            lastRow(timeDiff);
        }
    }

    /**
     * Handle first timer "Time!" button click
     */
    private void firstRow(){
        //Log.i(TAG,"TimeIt! First row...");
        timer.start();
        activeRow++;
    }

    /**
     * Handle normal(mid) timer "Time!" button click
     */
    private boolean midRow(long timeDiff){
        //update recycler view
        levelTimers.remove(0);
        playListAdapter.notifyDataSetChanged();

        //Log.i(TAG,"TimeIt! Mid Row... "+activeRow);
        if(timeDiff>=0){
            setTimer(activeRow+1);
            time = TIMER_DURATIONS[lvl][activeRow+1];
            //Log.i("time", String.valueOf(time));
            //updateActiveTimerView(time);
            activeRow++;
            timer.start();
            long diff = 100 - timeDiff/10;
            Log.i(TAG,"Row score: "+String.valueOf(diff));
            int rowScore = (int)(diff);
            rowScore = (rowScore>0)?rowScore:0; //if player timed too early
            addToAverageTimerScore(rowScore);
            //Log.i(TAG,"Row score: "+String.valueOf(rowScore)+" Time diff: "+String.valueOf(timeDiff/10));
            updateScore(rowScore);
            return true;
        }else{
            //Log.i(TAG,"GAME OVER!!!!!!!!");
            finishLevel(MID_SCREEN_TYPE_GAMEOVER);
            return false;
        }
    }

    /**
     * Handle last timer "Time!" button click
     */
    private boolean lastRow(long timeDiff){
        //update recycler view
        levelTimers.remove(0);
        playListAdapter.notifyDataSetChanged();

        //Log.i(TAG,"TimeIt! Last Row!");
        if(timeDiff>=0){
            long diff = 100 - timeDiff/10;
            Log.i(TAG,"Row score: "+String.valueOf(diff));
            int rowScore = (int)(diff);
            rowScore = (rowScore>0)?rowScore:0; //if player timed too early
            addToAverageTimerScore(rowScore);
            //Log.i(TAG,"Row score: "+String.valueOf(rowScore)+" Time diff: "+String.valueOf(timeDiff/10));
            updateScore(rowScore);
        }else{
            //Log.i(TAG,"GAME OVER!!!!!!!!");
            finishLevel(MID_SCREEN_TYPE_GAMEOVER);
            return false;
        }
        finishLevel(MID_SCREEN_TYPE_NORMAL);
        return true;
    }

    /**
     * Increments the average timer score data fields
     * @param rowScore Timer score of the last timed timer
     */
    private void addToAverageTimerScore(int rowScore){
        totalTimers++;
        totalTimersScore+=rowScore;
    }

    /**
     * Update game score
     * @param score2add timer score
     */
    private void updateScore(int score2add){
        gameScore.updateScore(lvl,score2add);
        //Update game score display on screen
        String levelText = String.format(resources.getString(R.string.string_subtitle_score),
                lvl+1,
                gameScore.getTotalScore());
        textLevel.setText(levelText);
    }

    /**
     * Finish the level after the last timer was timed and display a inter level summary screen
     * @param type Type of level finish: GAMEOVER/NORMAL/FINISHED
     */
    private void finishLevel(short type){
        timer.cancel();
        if(type==MID_SCREEN_TYPE_GAMEOVER) {
            setInterScreen(MID_SCREEN_TYPE_GAMEOVER);
        }else if(lvl<TIMER_DURATIONS.length-1 && gameScore.isLevelUnlocked(lvl+1)){
            setInterScreen(MID_SCREEN_TYPE_NORMAL);
        }else{
            setInterScreen(MID_SCREEN_TYPE_FINISHED);
        }
    }

    /**
     * Prepares a new CountDownTimer for the next timer countdown
     * @param row the timer row to prepare for
     */
    private void setTimer(int row){
        if(timer!=null)timer.cancel();
        timer = new CountDownTimer(TIMER_DURATIONS[lvl][row],5) {
            @Override
            public void onTick(long millisUntilFinished) {
                updateActiveTimerView(millisUntilFinished);
                time = millisUntilFinished;
            }

            @Override
            public void onFinish(){
                if(activeRow == TIMER_DURATIONS[lvl].length-1){
                    finishLevel(MID_SCREEN_TYPE_NORMAL);
                }else {
                    finishLevel(MID_SCREEN_TYPE_GAMEOVER);
                }
            }
        };
    }

    /**
     * Changes the content view to inter level summary screen
     * @param type Type of level finish
     */
    private void setInterScreen(final short type){
        setContentView(R.layout.inter_level_screen);

        //Save score between levels
        gameScore.saveScore(this);

        //Request ad
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        TextView levelText = (TextView)findViewById(R.id.play_level_text);
        String text = "lvl "+(lvl+1)+" "+gameScore.getTotalScore();
        levelText.setText(text);

        TextView infoText = (TextView)findViewById(R.id.mid_level_info_text);
        switch(type){
            case MID_SCREEN_TYPE_GAMEOVER:
                infoText.setText(R.string.string_mid_gameover);
                break;
            case MID_SCREEN_TYPE_HIGHSCORE:
                infoText.setText(R.string.string_mid_highscore);
                break;
            case MID_SCREEN_TYPE_NORMAL:
                infoText.setVisibility(View.INVISIBLE);
                break;
        }
        TextView summaryLevel = (TextView)findViewById(R.id.mid_summary_level);
        text = String.format(resources.getString(R.string.string_mid_summary_level),lvl+1);
        summaryLevel.setText(text);

        int levelScore = gameScore.getLevelScore(lvl).getScore();
        int levelMaxScore = gameScore.getLevelScore(lvl).getMaxScore();
        int gameTotalScore = gameScore.getTotalScore();
        int gameTotalMaxScore = gameScore.getMaxGameScore(startLvl,lvl);

        TextView levelScoreText = (TextView)findViewById(R.id.mid_summary_score);
        levelScoreText.setText(String.valueOf(levelScore)+"/"+String.valueOf(levelMaxScore));

        TextView totalScoreText = (TextView)findViewById(R.id.mid_summary_total);
        totalScoreText.setText(String.valueOf(gameTotalScore)+"/"+String.valueOf(gameTotalMaxScore));

        TextView buttonMainMenu = (TextView) findViewById(R.id.but_main_menu);
        buttonMainMenu.setOnClickListener(this);

        buttonNextLevel = (TextView)findViewById(R.id.but_next_level);
        buttonNextLevel.setOnClickListener(this);
        text = String.format(resources.getString(R.string.string_mid_next_level),10);
        buttonNextLevel.setText(text);

        if(type==MID_SCREEN_TYPE_GAMEOVER || type==MID_SCREEN_TYPE_FINISHED){
            buttonNextLevel.setVisibility(View.INVISIBLE);
        }else{
            nextLvlCountdown = new CountDownTimer(10000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    String text = String.format(resources.getString(R.string.string_mid_next_level),
                            (int) Math.floor(millisUntilFinished / 1000));
                    buttonNextLevel.setText(text);
                }

                @Override
                public void onFinish() {
                    String text = String.format(resources.getString(R.string.string_mid_next_level), 0);
                    buttonNextLevel.setText(text);
                    nextLvl();
                }
            }.start();
        }

        //update "Levels played" achievements
        if(!gamePlayed && mGoogleApiClient.isConnected()){
            GameUtils.incrementAchievement(mGoogleApiClient,R.string.achievement_play_5_games,1);
            GameUtils.incrementAchievement(mGoogleApiClient,R.string.achievement_play_50_games,1);
            GameUtils.incrementAchievement(mGoogleApiClient,R.string.achievement_play_500_games,1);
            //Log.i(TAG,"Game played achievement updated!");
        }
        gamePlayed = true;
        //Log.i(TAG,"Show summary "+type);
    }

    /**
     * Updates the active timer row
     * @param time curent time in milliseconds to display on screen
     */
    private void updateActiveTimerView(long time){
        //Update only the active timer not all the timers in the list
        FontView activeTimerText = (FontView)listPlayFields.getChildAt(0);

        long millis = time-15;
        int sec = (int)millis/1000;
        int hundreds = (int)Math.floor((millis-(sec*1000))/10);

        StringBuilder timestamp = new StringBuilder();
        if(sec<10)timestamp.append(" ");
        timestamp.append(String.valueOf(sec));
        timestamp.append(":");
        if(hundreds<10)timestamp.append("0");
        timestamp.append(hundreds);
        activeTimerText.setText(timestamp);
        if(millis<levelTimers.get(1))activeTimerText.
                setTextColor(ContextCompat.getColor(this,R.color.color_play_row_text_misssed));

        levelTimers.set(0,time-15); //Subtract a few millis from the displayed time, to account for
        // timer text lagging behind the actual time

        //playListAdapter.notifyDataSetChanged();
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id){
            case R.id.but_timeit: //"Time!" button
                timeIt();
                if(mGoogleApiClient.isConnected()) {
                    //Update timers timed achievements
                    GameUtils.incrementAchievement(mGoogleApiClient, R.string.achievement_click_click_time_time, 1);
                    GameUtils.incrementAchievement(mGoogleApiClient, R.string.achievement_button_pusher, 1);
                    GameUtils.incrementAchievement(mGoogleApiClient, R.string.achievement_master_of_time, 1);
                    GameUtils.incrementAchievement(mGoogleApiClient, R.string.achievement_marty_mcfly, 1);
                }else{
                    //TODO add achievement queue for incrementing achievements
                }
                break;
            case R.id.but_main_menu: //Main menu button from inter level screen
                if(nextLvlCountdown!=null)nextLvlCountdown.cancel();
                finish();
                break;
            case R.id.but_next_level: //Next level button from inter level screen
                if(nextLvlCountdown!=null)nextLvlCountdown.cancel();
                nextLvl();
                break;
            case R.id.backButton: //Back button
                finish();
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN: //Handle Google Play Games login
                //Log.i(TAG, "onActivityResult with requestCode == REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN, responseCode="
                       // + resultCode + ", intent=" + data);
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
                }
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Log.i(TAG, "onConnected() called. Sign in successful!");
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.d(TAG,"onConnectionFailed(), result: " + connectionResult);
        //Log.i(TAG,"onConnectionFailed() error_code: "+
                //String.valueOf(connectionResult.getErrorCode()+" message: "+
                  //      String.valueOf(connectionResult.getErrorMessage())));
        if (connectionResult.hasResolution()) {
            //Log.i(TAG,"onConnectionFailed(), hasResolution");
            try {
                switch (connectionResult.getErrorCode()){
                    case ConnectionResult.SIGN_IN_REQUIRED:
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN);
                        break;
                    case ConnectionResult.RESOLUTION_REQUIRED:
                        //Log.i(TAG,"onConnectionFailed(), PLAY_GAMES_NOT_INSTALLED");
                        connectionResult.startResolutionForResult(this,
                                REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER);
                        break;
                }

            } catch (IntentSender.SendIntentException e) {
                // The intent was canceled before it was sent.  Return to the default
                // state and attempt to connect to get an updated ConnectionResult.
                mGoogleApiClient.connect();
            }
        }
    }

    @Override
    public void onAchievementUnlocked(int stringId) {
        //Log.i(TAG,"Achievement unlocked. Id: "+stringId);
        //Unlock achievement on Play Games
        if(mGoogleApiClient.isConnected()) {
            GameUtils.unlockAchievement(mGoogleApiClient, stringId);
        }else if(mGoogleApiClient.isConnecting()){
            //TODO add to achievement queue
        }
    }
}