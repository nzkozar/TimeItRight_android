package com.ak93.timeit.views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.ak93.timeit.R;

/**
 * Created by Anže Kožar on 30.9.2016.
 */

public class LevelCard extends RelativeLayout {
    private FontView levelText,scoreText;
    private ImageView starView;

    public LevelCard(Context context) {
        super(context);
        init();
    }

    public LevelCard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LevelCard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init(){
        inflate(getContext(), R.layout.level_box_view,this);
        LayoutParams lp = new LayoutParams(dpToPx(100),dpToPx(100));
        int margin = dpToPx(20);
        lp.setMargins(margin,margin,margin,margin); //TODO No effect

        setLayoutParams(lp);
        //setBackgroundColor(getResources().getColor(R.color.color_bg));
        setBackground(getResources().getDrawable(R.drawable.bg_home_but));
        int padding = dpToPx(5);
        setPadding(padding,padding,padding,padding);

        this.levelText = findViewById(R.id.levelText);
        this.scoreText = findViewById(R.id.scoreText);
        this.starView = findViewById(R.id.scoreStars);
    }

    public void setLevelText(String string){
        this.levelText.setText(string);
    }

    public void setScoreText(String string){
        this.scoreText.setText(string);
    }

    public void setStars(int i){
        Drawable d;
        switch (i){
            case -1:
                d = getContext().getResources().getDrawable(R.drawable.lock);
                break;
            case 0:
                d = getContext().getResources().getDrawable(R.drawable.star0);
                break;
            case 1:
                d = getContext().getResources().getDrawable(R.drawable.star1);
                break;
            case 2:
                d = getContext().getResources().getDrawable(R.drawable.star2);
                break;
            case 3:
                d = getContext().getResources().getDrawable(R.drawable.star3);
                break;
            default:
                d = getContext().getResources().getDrawable(R.drawable.star0);
        }
        starView.setImageDrawable(d);
        starView.invalidate();
    }

    /**
     * Converts dp to px, using the provided View's dpi density
     * @param dp dp to convert
     * @return px converted from dp
     */
    private int dpToPx(float dp) {
        float pixelsPerOneDp = getResources().getDisplayMetrics().densityDpi / 160f;
        return (int)(dp*pixelsPerOneDp);
    }
}
