package com.ak93.timeit;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

/**
 * Created by Anže Kožar on 19.11.2016.
 * A LayoutManager to use for timer play list. This Layout manager has its vertical scroll disabled
 * so that players can not view the timers in advance or accidentally scroll the list during play.
 */

public class PlayListLayoutManager extends LinearLayoutManager {

    public PlayListLayoutManager(Context context) {
        super(context, VERTICAL, false);
    }

    @Override
    public boolean canScrollVertically() {
        //Prevent user from scrolling the play list
        return false;
    }
}
