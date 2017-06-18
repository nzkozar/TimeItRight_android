package com.ak93.timeit.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.GameUtils;
import com.ak93.timeit.objects.LevelScore;
import com.ak93.timeit.objects.Score;
import com.ak93.timeit.R;
import com.ak93.timeit.views.LevelCard;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import static android.content.pm.PackageManager.PERMISSION_GRANTED;

/**
 * Created by Anže Kožar on 30.9.2016.
 * This activity displays all available game levels with their current scores.
 * It enables the player to start a game from any unlocked level.
 */

public class ActivityLevels extends Activity implements AppConstants, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private LinearLayout levelsListView;
    private Score savedScores;
    private ArrayList<LevelCard> levelCards;
    private ProgressBar shareProgress;

    private SharedPreferences preferences;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;
    private ArrayList<Integer> achievementQueue = new ArrayList<>();

    private static final int MSG_LEVELS_CREATED = 0;
    public static final String TAG = "ActivityLevels";


    Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what){
                case MSG_LEVELS_CREATED:
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            populateLevels();
                        }
                    });
                    break;
            }
        }
    };

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

        setContentView(R.layout.activity_levels);

        preferences = getSharedPreferences(getString(R.string.main_preferences),MODE_PRIVATE);

        savedScores = new Score(this);
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
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    //Initialize UI
    private void init(){
        //Request ad
        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        ImageView backButton = (ImageView)findViewById(R.id.backButton);
        backButton.setOnClickListener(this);
        ImageView shareButton = (ImageView)findViewById(R.id.shareButton);
        shareButton.setOnClickListener(this);

        levelsListView = (LinearLayout)findViewById(R.id.levelListLayout);
        shareProgress = (ProgressBar)findViewById(R.id.progressBar);
        shareProgress.setIndeterminate(true);

        new Thread(new Runnable() {
            @Override
            public void run() {
                levelCards = createLevelCards();
                handler.obtainMessage(MSG_LEVELS_CREATED).sendToTarget();
            }
        }).start();
    }

    /**
     * Creates LevelCard Views for each level, to be displayed later on screen.
     * TODO Level cards should be asynchronously loaded onto screen as they are created, to avoid
     * TODO the lag between opening the activity and seeing the levels.
     * @return an ArrayList of LevelCard views
     */
    private ArrayList<LevelCard> createLevelCards(){
        ArrayList<LevelCard> arrayList = new ArrayList<>();
        boolean lvlUnlocked = true;
        for(LevelScore ls:savedScores.getLevelScores()){

            LevelCard levelCard = new LevelCard(this);
            levelCard.setLevelText(String.valueOf(ls.getLevel()+1));

            int levelScore = ls.getBestScore();
            int levelMaxScore = ls.getMaxScore();

            String scoreText = levelScore+"/"+levelMaxScore;
            levelCard.setScoreText(scoreText);

            if(lvlUnlocked) {
                if (levelScore > levelMaxScore * 0.9) {
                    levelCard.setStars(3);
                } else if (levelScore > levelMaxScore * 0.6) {
                    levelCard.setStars(2);
                } else if (levelScore > levelMaxScore * 0.3) {
                    levelCard.setStars(1);
                }
            }else{
                levelCard.setStars(-1);
            }

            arrayList.add(levelCard); //Adds a level box view to the Array list

            lvlUnlocked = levelScore > (levelMaxScore * 0.3);
            //Log.i(TAG,"Level "+i+" added!");
        }
        return arrayList;
    }

    /**
     * Populates the list of levels with previously created LevelCard Views.
     * TODO should display levels in a RecyclerView instead of a LinearLayout
     */
    private void populateLevels(){
        //Log.i(TAG,"Populating levels...");
        levelsListView.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        for(int i = 0;i<TIMER_DURATIONS.length;i++){
            //Log.i(TAG,"Adding level "+i);
            boolean newRow = i%3 == 0;
            if(newRow){
                row.setBackgroundColor(ContextCompat.getColor(this,R.color.color_bg));
                levelsListView.addView(row); //Adds a previous row to the level list
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lp =
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.setMargins(0,0,0,30);
                row.setLayoutParams(lp);
                //Log.i(TAG,"Added level row");
            }

            LevelCard levelCard = levelCards.get(i);
            row.addView(levelCard); //Adds a level box view to row
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) levelCard.getLayoutParams();
            lp.weight = 1f;
            lp.setMargins(10,0,10,0);
            levelCard.setLayoutParams(lp);
            levelCard.setId(i);
            levelCard.setOnClickListener(this);
        }
        row.setBackgroundColor(ContextCompat.getColor(this,R.color.color_bg));
        levelsListView.addView(row); //Adds the last row to the level list
        levelsListView.invalidate();
        //Log.i(TAG,"...Levels populated!");
    }

    /**
     * Updates the displayed level cards for the provided LevelScores
     * @param newHighscores A list of LevelScores to update
     */
    private void updateLevelCards(ArrayList<LevelScore> newHighscores){
        //Log.i(TAG,"Update level cards");
        boolean lvlUnlocked = true;
        for(int i = 0; i<levelCards.size(); i++){
            int score = savedScores.getLevelScore(i).getBestScore();
            int levelMaxScore = savedScores.getLevelScore(i).getMaxScore();

            int newHighscoreIndex = levelScoreIndex(newHighscores,i);
            if(newHighscoreIndex>-1){
                //UPDATE level score
                score = newHighscores.get(newHighscoreIndex).getBestScore();
                String scoreText = score+"/"+levelMaxScore;
                levelCards.get(i).setScoreText(scoreText);
            }

            //Log.i(TAG,"lvl: "+i+" levelScore: "+levelMaxScore+" isUnlocked: "+String.valueOf(lvlUnlocked));
            if(lvlUnlocked){
                //Log.i(TAG,"setStars");
                if (score >= levelMaxScore * 0.9) {
                    levelCards.get(i).setStars(3);
                    if(mGoogleApiClient.isConnected()) {
                        //unlock 3 star achievement
                        GameUtils.unlockAchievement(mGoogleApiClient, R.string.achievement_full_house);
                    }else{
                        //Put achievement into queue, to be unlocked upon client connection
                        achievementQueue.add(R.string.achievement_full_house);
                        //Log.i(TAG,"Achievement put in queue.");
                    }
                } else if (score >= levelMaxScore * 0.6) {
                    levelCards.get(i).setStars(2);
                    if(mGoogleApiClient.isConnected()) {
                        //unlock 2 star achievement
                        GameUtils.unlockAchievement(mGoogleApiClient,R.string.achievement_glass_half_full);
                    }else{
                        //Put achievement into queue, to be unlocked upon client connection
                        achievementQueue.add(R.string.achievement_glass_half_full);
                        //Log.i(TAG,"Achievement put in queue.");
                    }
                } else if (score >= levelMaxScore * 0.3) {
                    levelCards.get(i).setStars(1);
                    if(mGoogleApiClient.isConnected()) {
                        //unlock 2 star achievement
                        GameUtils.unlockAchievement(mGoogleApiClient,R.string.achievement_its_something);
                    }else{
                        //Put achievement into queue, to be unlocked upon client connection
                        achievementQueue.add(R.string.achievement_its_something);
                        //Log.i(TAG,"Achievement put in queue.");
                    }
                }else{
                    levelCards.get(i).setStars(0);
                }
            }else{
                levelCards.get(i).setStars(-1);
            }
            lvlUnlocked = score >= (levelMaxScore * 0.3);
        }
    }

    /**
     * Gets the index of a certain level score in an ArrayList of LevelScore objects
     * @param scores Haystack
     * @param level level number to find
     * @return Index of a LevelScore in an ArrayList, -1 if no such index
     */
    private int levelScoreIndex(ArrayList<LevelScore> scores, int level){
        for(LevelScore ls:scores){
            if(ls.getLevel()==level)return scores.indexOf(ls);
        }
        return -1;
    }

    /**
     * Creates a Bitmap of all level badges, to be used in sharing
     * @return
     */
    private Bitmap getLevelsScreenshot(){
        ScrollView v = (ScrollView)findViewById(R.id.levelsScroll);

        int width = v.getMeasuredWidth();
        int height = v.getChildAt(v.getChildCount()-1).getBottom();

        Bitmap b = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        v.layout(v.getLeft(), v.getTop(), v.getRight(), v.getBottom());
        v.draw(c);
        return b;
    }

    /**
     * Gets an Uri of a Bitmap
     * @param inContext Context
     * @param inImage Bitmap
     * @return Uri pointing to this Bitmap
     */
    public static Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(),
                inImage, "", "");
        return Uri.parse(path);
    }

    /**
     * Creates a bitmap of the levels screen and prepares ist for user to share
     */
    private void shareLevels(){
        //request for external storage permissions if needed
        boolean permissionCheckWriteStorage = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PERMISSION_GRANTED;
        if(permissionCheckWriteStorage) {
            shareProgress.setVisibility(View.VISIBLE);

            Log.i(TAG,"Capturing image...");
            Bitmap bitmap = getLevelsScreenshot();
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("image/jpeg");
            sendIntent.putExtra(Intent.EXTRA_STREAM, getImageUri(this, bitmap));
            startActivity(Intent.createChooser(sendIntent, "Share"));
            shareProgress.setVisibility(View.INVISIBLE);
            Log.i(TAG,"Share dialog opened!");
        }else{
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    APP_PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if(requestCode==APP_PERMISSIONS_REQUEST_CODE){
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG,"Permissions granted!");
                // permission was granted, yay! Do the shareLevels() where the permission was requested from
                shareLevels();
            } else {
                Log.i(TAG,"Storage permissions denied!");
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id){
            case R.id.backButton:
                finish();
                break;
            case R.id.shareButton:
                shareLevels();
                break;
            default: //Handle on LevelCard clicks
                //Log.i(TAG,"onClick default. id: "+id+"; Parent class: "+v.getClass().getName());
                if(v.getClass()==LevelCard.class) {
                    //check if level is unlocked
                    boolean isLevelUnlocked = true;
                    if(id>0) isLevelUnlocked = savedScores.isLevelUnlocked(id);
                    if(isLevelUnlocked) {
                        Intent intentPlay = new Intent(getApplicationContext(), Play.class);
                        intentPlay.putExtra(getString(R.string.KEY_START_FROM_LVL), id);
                        startActivityForResult(intentPlay, REQUEST_CODE_PLAY);
                    }else{
                        Log.i(TAG,"onLevelClick: Level locked!");
                    }
                }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN:
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
                }
                break;
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_OTHER:
                if(resultCode == RESULT_OK){

                }else {
                    //Log.i(TAG,"Result code NOT_OK");
                    preferences.edit().putBoolean(getString(R.string.main_pref_autologin),false).apply();
                }
                break;
            case REQUEST_CODE_PLAY: //Result of a game
                //Score newScore = (Score)data.getSerializableExtra("score");
                ArrayList<LevelScore> levelScores = (ArrayList<LevelScore>)data.getSerializableExtra("levelScores");
                final ArrayList<LevelScore> newHighscores = savedScores.compareLevelScores(levelScores);
                if(newHighscores.size()>0){
                    updateLevelCards(newHighscores);
                }
                savedScores = new Score(this);
                break;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //Log.i(TAG, "onConnected() called. Sign in successful!");
        //parse achievement queue
        //Log.i(TAG,"Parsing achievementQueue size:"+achievementQueue.size());
        for (int i = 0;i<achievementQueue.size();i++) {
            //Log.i(TAG,"Parsing achievement from queue..."+i);
            int r_id = achievementQueue.get(i);
            GameUtils.unlockAchievement(mGoogleApiClient,r_id);
        }
        achievementQueue.clear();
    }

    @Override
    public void onConnectionSuspended(int i) {
        //Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        //Log.i(TAG,"onConnectionFailed(), result: " + connectionResult);
        //Log.i(TAG,"onConnectionFailed() error_code: "+
                //String.valueOf(connectionResult.getErrorCode()+" message: "+
                        //String.valueOf(connectionResult.getErrorMessage())));
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
}
