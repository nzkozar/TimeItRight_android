package com.ak93.timeit.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.Log;

import com.ak93.timeit.R;

import timber.log.Timber;

/**
 * Created by Anže Kožar on 26.9.2016.
 */

public class FontView extends android.support.v7.widget.AppCompatTextView {

    public FontView(Context context) {
        super(context);
    }

    public FontView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setCustomFont(context, attrs);
    }

    public FontView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setCustomFont(context, attrs);
    }

    private void setCustomFont(Context ctx, AttributeSet attrs) {
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.FontView);
        String customFont = a.getString(R.styleable.FontView_customFont);
        setCustomFont(ctx, customFont);
        a.recycle();
    }

    public boolean setCustomFont(Context ctx, String asset) {
        Typeface typeface = null;
        if(asset!=null && !asset.equals("")) {
            try {
                typeface = Typeface.createFromAsset(ctx.getAssets(), asset);
            } catch (Exception e) {
                Timber.e("Unable to load typeface: " + e.getMessage());
                return false;
            }
        }
        if(typeface!=null)setTypeface(typeface);
        return true;
    }
}
