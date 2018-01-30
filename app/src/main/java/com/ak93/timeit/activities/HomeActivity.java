package com.ak93.timeit.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.HashMap;
import java.util.function.BiConsumer;

import io.branch.indexing.BranchUniversalObject;
import io.branch.referral.Branch;
import io.branch.referral.BranchError;
import io.branch.referral.util.LinkProperties;
import timber.log.Timber;

/**
 * Created by Anže Kožar on 30.2.2016.
 * Home activity serves as a main menu, containing the navigation buttons to other app screens
 */
public class HomeActivity extends Activity implements View.OnClickListener,AppConstants{

    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient;

    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        MobileAds.initialize(getApplicationContext(), getString(R.string.admob_app_id));

        // Create the client used to sign in to Google services.
        mGoogleSignInClient = GoogleSignIn.getClient(this,
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());

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
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        findViewById(R.id.but_play).setOnClickListener(this);
        findViewById(R.id.but_play_hard).setOnClickListener(this);
        findViewById(R.id.but_time_hall).setOnClickListener(this);
        findViewById(R.id.but_tutorial).setOnClickListener(this);
        findViewById(R.id.but_about).setOnClickListener(this);
        findViewById(R.id.button_sign_in).setOnClickListener(this);
    }

    private boolean isSignedIn() {
        return GoogleSignIn.getLastSignedInAccount(this) != null;
    }

    private void signInSilently() {
        Timber.i("signInSilently()");

        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Timber.d("signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Timber.d("signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    private void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN);
    }

    private void signOut() {
        Timber.i("signOut()");

        if (!isSignedIn()) {
            Timber.w("signOut() called, but was not signed in!");
            return;
        }

        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        boolean successful = task.isSuccessful();
                        Timber.i("signOut(): " + (successful ? "success" : "failed"));

                        onDisconnected();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Since the state of the signed in user can change when the activity is not active
        // it is recommended to try and sign in silently from when the app resumes.

        //Auto login to Google Play Games if auto login is enabled else show sign in button
        signInSilently();
    }


    @Override
    protected void onStart() {
        super.onStart();
        /*
        //Auto login to Google Play Games if auto login is enabled else show sign in button
        if(preferences.getBoolean(getString(R.string.main_pref_autologin),true)) {
            //Log.i(TAG,"Auto connect google API");
            mGoogleApiClient.connect(); //Auto connect Play Games services
        }else{
            showSignInBar(true);
        }
        */

        Branch branch = Branch.getInstance();

        branch.initSession(new Branch.BranchUniversalReferralInitListener() {
            @Override
            public void onInitFinished(BranchUniversalObject branchUniversalObject, LinkProperties linkProperties, BranchError error) {
                if (error == null) {
                    // params are the deep linked params associated with the link that the user clicked -> was re-directed to this app
                    // params will be empty if no data found
                    // ... insert custom logic here ...
                    if(linkProperties!=null) {
                        HashMap<String,String> params = linkProperties.getControlParams();
                        if(params.containsKey("$deeplink_path")){
                            String deeplinkPath = params.get("$deeplink_path");
                            if(deeplinkPath.equals("levels")){
                                Intent intentPlay = new Intent(getApplicationContext(),ActivityLevels.class);
                                startActivityForResult(intentPlay,REQUEST_CODE_PLAY);
                            }
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            params.forEach(new BiConsumer<String, String>() {
                                @Override
                                public void accept(String s, String s2) {
                                    Timber.i("Branch control params: "+s+", "+s2);
                                }
                            });
                        }
                    }
                } else {
                    Timber.i(error.getMessage());
                }
            }
        }, this.getIntent().getData(), this);

    }

    @Override
    public void onNewIntent(Intent intent) {
        this.setIntent(intent);

        //Get deep app link data
        Uri data = this.getIntent().getData();
        if (data != null && data.isHierarchical()) {
            String uri = this.getIntent().getDataString();
            Timber.i("Deep link clicked " + uri);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Log.i(TAG,"onActivityResult with requestCode= "+requestCode+" resultCode= "+resultCode);
        switch (requestCode){
            case REQUEST_CODE_GOOGLE_PLAY_RESOLUTION_SIGN_IN: //Handle Google Play Games login
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(data);
                try {
                    GoogleSignInAccount account = task.getResult(ApiException.class);
                    onConnected(account);
                } catch (ApiException apiException) {
                    String message = apiException.getMessage();
                    int statusCode = apiException.getStatusCode();
                    if(statusCode== GoogleSignInStatusCodes.SIGN_IN_REQUIRED){
                        //TODO resolution!
                    }
                    message += "("+apiException.getStatusCode()+")";
                    if (message == null || message.isEmpty()) {
                        message = getString(R.string.signin_other_error);
                    }

                    onDisconnected();

                    new AlertDialog.Builder(this)
                            .setMessage(message)
                            .setNeutralButton(android.R.string.ok, null)
                            .show();
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
                startSignInIntent();
                break;
        }
    }

    /**
     * Shows the Google sign in button
     */
    private void showSignInBar(boolean show) {
        Timber.i("Showing sign in bar");
        if(show) {
            findViewById(R.id.sign_in_bar).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.sign_in_bar).setVisibility(View.INVISIBLE);
        }
    }

    public void onConnected(GoogleSignInAccount googleSignInAccount) {
        Timber.i("onConnected()");
        showSignInBar(false);
        //Enable auto login in the future
        preferences.edit().putBoolean(getString(R.string.main_pref_autologin),true).apply();
    }

    private void onDisconnected() {
        Timber.i("onDisconnected()");
        showSignInBar(true);
    }

}