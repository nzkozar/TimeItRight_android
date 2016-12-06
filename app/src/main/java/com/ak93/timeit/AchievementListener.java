package com.ak93.timeit;

/**
 * Created by Anže Kožar on 28.11.2016.
 * An interface to report Achievement Unlocked events.
 */

public interface AchievementListener {
    /**
     * onAchievementUnlocked
     * @param stringId The string resource of the unlocked achievement
     */
    void onAchievementUnlocked(int stringId);
}
