package com.ak93.timeit.activities;

import android.app.Activity;
import android.os.Bundle;

import com.ak93.timeit.AppConstants;

/**
 * Created by Anže Kožar on 28/02/2016.
 * hard mode of the game. Disappearing timer texts and shieeeeet! Serious stuff, really!
 * To be added sometime in the future. Maybe. Who knows...
 */
public class PlayHard extends Activity implements AppConstants {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setResult(RESULT_CODE_CANCELED);
    }
}
