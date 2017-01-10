package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.GameUtils;
import com.ak93.timeit.R;
import com.ak93.timeit.views.FontView;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import java.util.ArrayList;

/**
 * Created by Anže Kožar on 6.11.2016.
 * This activity displays the Tutorial for the game
 */

public class TutorialActivity extends Activity implements AppConstants, View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private ViewPager mViewPager;
    private TutorialAdapter mViewPagerAdapter;

    // Client used to interact with Google APIs.
    private GoogleApiClient mGoogleApiClient;

    private SharedPreferences mainPreferences;

    private static final String TAG = "TutorialActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);


        // Create the Google Api Client with access to Games
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();

        setContentView(R.layout.activity_tutorial);

        init();

        //Mark tutorial shown in scorePreferences
        mainPreferences = getSharedPreferences(getString(R.string.main_preferences),MODE_PRIVATE);
        SharedPreferences.Editor editor = mainPreferences.edit();
        editor.putBoolean(getString(R.string.main_pref_tut_shown),true);
        editor.apply();
    }

    /**
     * Initialize the UI
     */
    private void init(){
        final FontView continueButton = (FontView) findViewById(R.id.continueButton);
        continueButton.setOnClickListener(this);

        final ImageView leftButton = (ImageView)findViewById(R.id.slide_left);
        leftButton.setOnClickListener(this);

        final ImageView rightButton = (ImageView)findViewById(R.id.slide_right);
        rightButton.setOnClickListener(this);

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPagerAdapter = new TutorialAdapter(this);
        mViewPager.setAdapter(mViewPagerAdapter);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabDots);
        tabLayout.setupWithViewPager(mViewPager, true);
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if(pos == mViewPagerAdapter.getCount()-1){ //Last tab
                    continueButton.setVisibility(View.VISIBLE);
                    rightButton.setVisibility(View.INVISIBLE);
                    //unlock achievement
                    if (mGoogleApiClient.isConnected()) {
                        // unlock the "Trivial Victory" achievement.
                        GameUtils.unlockAchievement(mGoogleApiClient,R.string.achievement_learn_to_time);
                        Log.i(TAG, "Tutorial achievement unlocked!");
                    }
                } else if(pos == 0){
                    leftButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                int pos = tab.getPosition();
                if(pos == 0){ //First tab
                    leftButton.setVisibility(View.VISIBLE);
                }else if(pos == mViewPagerAdapter.getCount()-1){
                    rightButton.setVisibility(View.VISIBLE);
                    continueButton.setVisibility(View.INVISIBLE);
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
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
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onClick(final View v) {
        switch (v.getId()){
            case R.id.continueButton:
                finish();
                break;
            case R.id.slide_left:
                //Slide left button
                mViewPager.setCurrentItem(mViewPager.getCurrentItem()-1,true);
                break;
            case R.id.slide_right:
                //Slide right button
                mViewPager.setCurrentItem(mViewPager.getCurrentItem()+1,true);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN:
                Log.i(TAG, "onActivityResult with requestCode == REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN, responseCode="
                        + resultCode + ", intent=" + data);
                if (resultCode == RESULT_OK) {
                    Log.i(TAG,"Result CODE_OK");
                    //User has successfully logged in. From now on, log him in automatically
                    mainPreferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
                    mGoogleApiClient.connect();
                } else {
                    //sign in failed, don't autologin again
                    Log.i(TAG,"Result code NOT_OK");
                    if(!GameUtils.hasNetworkConnection(this)){
                        Toast.makeText(this,"Active internet connection needed to log in!",Toast.LENGTH_LONG).show();
                    }
                    mainPreferences.edit().putBoolean(getString(R.string.main_pref_autologin),false).apply();
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
        //Log.i(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        mGoogleApiClient.connect();
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


    /**
     * ViewPager adapter. Displays the tutorial screens.
     */
    private class TutorialAdapter extends PagerAdapter {

        private LayoutInflater layoutInflater;
        private Context mContext;
        private ArrayList<Integer> drawableIds = new ArrayList<>();
        private String[] texts;

        private static final String TAG = "TutorialAdapter";

        TutorialAdapter(Context context){
            layoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mContext = context;

            //Add drawable ids to an array list, for use in populating the view pager
            drawableIds.add(R.drawable.tut_0);
            drawableIds.add(R.drawable.tut_0);
            drawableIds.add(R.drawable.tut_2);
            drawableIds.add(R.drawable.tut_3);
            drawableIds.add(R.drawable.tut_4);
            drawableIds.add(R.drawable.tut_5);
            drawableIds.add(R.drawable.tut_6);
            drawableIds.add(R.drawable.tut_7_1);

            //get tutorial slide texts
            texts = context.getResources().getStringArray(R.array.strings_tutorial_slide_texts);
        }

        @Override
        public int getCount() {
            return drawableIds.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            RelativeLayout v = (RelativeLayout)layoutInflater.inflate(R.layout.tutorial_page,null);
            //Log.i(TAG,"position: "+position);
            FontView slideText = (FontView)v.findViewById(R.id.slideText);
            ImageView slideImage = (ImageView)v.findViewById(R.id.slideImage);

            slideText.setText(texts[position]);
            slideImage.setImageResource(drawableIds.get(position));

            container.addView(v,0);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }
    }
}
