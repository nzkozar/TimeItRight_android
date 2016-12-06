package com.ak93.timeit.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.ak93.timeit.AchievementListener;
import com.ak93.timeit.AppConstants;
import com.ak93.timeit.R;
import java.util.ArrayList;

/**
 * Created by Anže Kožar on 18.11.2016.
 * A class to hold the score of a game, as well as level scores for display in ActivityLevels.
 * Individual level scores are held in an ArrayList of LevelScore objects.
 */
public class Score implements AppConstants{

    private int totalScore=0,topRowScore=0,topLvl=0;
    //List of individual level scores
    private ArrayList<LevelScore> levelScores = new ArrayList<>();;

    //Achievement listener to report achievement changes to
    private AchievementListener mAchievementListener = null;

    private String TAG = "Score";

    /**
     * Construct a new Score object
     * @param context Context to use when retrieving LevelScore objects
     */
    public Score(Context context){
        //Prepare LevelScore objects
        for(int i = 0;i<TIMER_DURATIONS.length;i++){
            LevelScore levelScore = new LevelScore(context,i);
            levelScores.add(levelScore);
        }
    }

    /**
     * Sets an AchievementListener to this Score
     * @param listener AchievementListener
     */
    public void setAchievementListener(AchievementListener listener){
        mAchievementListener = listener;
    }

    /**
     * @return The total score of this game
     */
    public int getTotalScore() {
        return totalScore;
    }

    /**
     * Gets LevelScore.
     * @param lvl The levels whose LevelScore to get
     * @return LevelScore object from this Score dataset
     */
    public LevelScore getLevelScore(int lvl){
        return levelScores.get(lvl);
    }

    /**
     * @return ArrayList<LevelScore>
     */
    public ArrayList<LevelScore> getLevelScores(){return levelScores;}

    /**
     * Updates this Score objects score fields for the current game
     * @param lvl the level to which the score increment belongs
     * @param lvlScoreIncrement the score increment to add to this Score object
     */
    public void updateScore(int lvl, int lvlScoreIncrement){
        //increment top lvl
        if(topLvl<lvl)topLvl=lvl;
        //Increment total lvl score
        this.totalScore += lvlScoreIncrement;
        //Save the best row score for later highscore comparison
        if(topRowScore<lvlScoreIncrement)topRowScore=lvlScoreIncrement;
        //Save level scores
        levelScores.get(lvl).incrementScore(lvlScoreIncrement);
    }

    /**
     * Saves the highscore fields and checks for any achievements
     * @param context Context to use for SharedPreferences access
     */
    public void saveScore(Context context){
        SharedPreferences preferences =
                context.getSharedPreferences(context.getString(R.string.score_preferences),
                        Context.MODE_PRIVATE);

        SharedPreferences.Editor editor= preferences.edit();
        //Save score values
        int curentBestTotal = preferences.getInt(context.getString(R.string.KEY_BEST_TOTAL_SCORE),-1);
        if(curentBestTotal<totalScore) {
            editor.putInt(context.getString(R.string.KEY_BEST_TOTAL_SCORE), totalScore);
            //if score is more than some best score achievement, notify of achievement unlock
            if (totalScore > 9000){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_over_9000);
            }
            if(totalScore >= 5000){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_5000);
            }
            if(totalScore >= 4000){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_4000);
            }
            if(totalScore >= 2000){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_2000);
            }
            if(totalScore >= 1000){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_1000);
            }
            if(totalScore >= 500){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_500);
            }
            if(totalScore >= 250){
                mAchievementListener.onAchievementUnlocked(R.string.achievement_best_game_250);
            }
        }

        int curentBestRow = preferences.getInt(context.getString(R.string.KEY_BEST_ROW_SCORE),-1);
        if(curentBestRow<=topRowScore){
            editor.putInt(context.getString(R.string.KEY_BEST_ROW_SCORE),topRowScore);
            //if best row score is 100, notify of achievement unlock
            if(topRowScore==100) {
                mAchievementListener.onAchievementUnlocked(R.string.achievement_perfect_timing);
            }
        }

        int curentBestLvl = preferences.getInt(context.getString(R.string.KEY_BEST_LVL),-1);
        if(curentBestLvl<topLvl){
            editor.putInt(context.getString(R.string.KEY_BEST_LVL),topLvl);
            //Log.i(TAG,"New best level: "+String.valueOf(topLvl));
            //Notify the achievement listener of level achievement changes
            if(mAchievementListener!=null) {
                switch (topLvl) {
                    case 0:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_first_timer);
                        break;
                    case 1:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_2);
                        break;
                    case 5:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_6);
                        break;
                    case 8:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_9);
                        break;
                    case 11:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_12);
                        break;
                    case 14:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_15);
                        break;
                    case 17:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_18);
                        break;
                    case 20:
                        mAchievementListener.onAchievementUnlocked(R.string.achievement_time_level_21);
                        break;
                }
            }
        }

        //Save best level scores
        for (LevelScore ls: levelScores) {
            editor.putInt(context.getString(R.string.KEY_LEVEL_SCORE) + ls.getLevel(), ls.getBestScore());
        }

        editor.apply();
    }

    /**
     * Compares this Score to the provided Score.
     * @param levelScores Level score list to check for improvements
     * @return Returns a Map of level scores that are better in the provided score than in this Score
     */
    public ArrayList<LevelScore> compareLevelScores(ArrayList<LevelScore> levelScores){
        ArrayList<LevelScore> diffList = new ArrayList<>();
        for(LevelScore lsNew:levelScores){
            if(lsNew.getBestScore()> this.levelScores.get(lsNew.getLevel()).getBestScore())diffList.add(lsNew);
        }
        return diffList;
    }

    /**
     * Checks if a certain level is unlocked
     * @param i the level to check
     * @return True if the level in question is unlocked
     */
    public boolean isLevelUnlocked(int i){
        if(i==0)return true; //First level is always unlocked
        if(i<TIMER_DURATIONS.length) {
            int bestScore = getLevelScore(i - 1).getBestScore();
            int maxScore = getLevelScore(i - 1).getMaxScore();
            return bestScore >= (maxScore * 0.3);
        }
        return false;
    }

    /**
     * Get maximum possible score from level A to level B
     * @param lvlFrom The level where the player started
     * @param lvlTo The current level
     * @return Number of points a player can theoretically achieve if he gets perfect score on each timer
     */
    public int getMaxGameScore(int lvlFrom, int lvlTo){
        int maxScore = 0;
        for (int i = lvlFrom;i<=lvlTo;i++){
            maxScore+= levelScores.get(i).getMaxScore();
        }
        return maxScore;
    }
}
