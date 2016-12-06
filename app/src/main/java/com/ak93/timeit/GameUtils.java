package com.ak93.timeit;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;

/**
 * Created by Anže Kožar on 26.11.2016.
 * A class containing some static methods used throughout the app
 */

public class GameUtils implements AppConstants {

    private static final String TAG = "GameUtils";

    /**
     * Unlocks the achievement with stringId
     * @param client GoogleApiClient to connect with
     * @param stringId String resource id of the achievement
     */
    public static void unlockAchievement(GoogleApiClient client, int stringId){
        Games.Achievements.unlock(client, client.getContext().getString(stringId));
        //Log.i(TAG, "Achievement unlocked!");
    }

    /**
     * Increments the achievement with stringId, increment.
     * @param client GoogleApiClient to connect with
     * @param stringId String resource id of the achievement
     * @param increment The amount this achievement should be incremented by
     */
    public static void incrementAchievement(GoogleApiClient client, int stringId, int increment){
        Games.Achievements.increment(client,client.getContext().getString(stringId),increment);
        //Log.i(TAG, "Achievement incremented!");
    }

    /**
     * @param context The context to use to check the connection
     * @return True if this device is currently connected to the internet
     */
    public static boolean hasNetworkConnection(Context context){
        ConnectivityManager cm = (ConnectivityManager)context.
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if(activeNetwork.getType() == ConnectivityManager.TYPE_WIFI ||
                    activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE)
                return true;
        }
        return false;
    }

}
