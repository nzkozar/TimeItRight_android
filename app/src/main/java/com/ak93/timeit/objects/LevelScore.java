package com.ak93.timeit.objects;

import android.content.Context;
import android.content.SharedPreferences;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.R;
import java.io.Serializable;

/**
 * Created by Anže Kožar on 18.11.2016.
 * A class representing the score of a single level.
 * It contains the information of the best player achieved score, max possible level score and the
 * current level score, if used in the Play activity.
 */

public class LevelScore implements Serializable, AppConstants {

    private int level, score = 0, maxScore, bestScore = 0;

    /**
     * Creates a new LevelScore object from saved score preferences
     * @param context The Context to use for SharedPreferences retrieval
     * @param level Number of the level to create
     */
    public LevelScore(Context context, int level){
        this.level=level;
        SharedPreferences preferences =
                context.getSharedPreferences(context.getString(R.string.score_preferences),
                        Context.MODE_PRIVATE);
        bestScore = preferences.getInt(context.getString(R.string.KEY_LEVEL_SCORE)+level,0);
        maxScore = (TIMER_DURATIONS[level].length-1) * 100;
    }

    /**
     * Increments this LevelScore's score
     * @param increment How much to increment this score by
     * @return Returns the new total level score value
     */
    public int incrementScore(int increment){
        score+=increment;
        if(score>bestScore)bestScore=score;
        return score;
    }

    public int getLevel() {
        return level;
    }

    public int getScore(){
        return score;
    }

    public int getBestScore(){
        return bestScore;
    }

    public int getMaxScore() {
        return maxScore;
    }
}
