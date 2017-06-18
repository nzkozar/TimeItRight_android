package com.ak93.timeit;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.ak93.timeit.views.FontView;
import java.util.ArrayList;

/**
 * Created by Anže Kožar on 19.11.2016.
 * A RecyiclerView Adapter used to populate the play list with timer views
 */

public class PlayListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    //Dataset
    private ArrayList<Long> mDataset = new ArrayList<>();
    //Timer text colors
    private int colorFirst, colorMissed;

    private static final String TAG = "PlayListAdapter";

    /** Provide a reference to the views for each data item
     *  Complex data items may need more than one view per item, and
     *  you provide access to all the views for a data item in a view holder
     */
    private static class ViewHolder extends RecyclerView.ViewHolder {
        FontView mView;
        ViewHolder(View v) {
            super(v);
            mView = (FontView) v;
        }
    }

    /**
     * Construct a new adapter
     * @param context Context for resource retrieval
     * @param dataset ArrayList of timer values in milliseconds
     */
    public PlayListAdapter(Context context, ArrayList<Long> dataset) {
        mDataset = dataset;
        colorFirst = ContextCompat.getColor(context,R.color.color_play_row_text_first);
        colorMissed = ContextCompat.getColor(context,R.color.color_play_row_text_misssed);
    }

    /**
     * Sets this adapters data
     * @param data the dataset to use
     */
    public void setData(ArrayList<Long> data){
        mDataset = data;
        notifyDataSetChanged();
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        //Log.i(TAG,"CREATE VIEW!!!!");
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.play_row, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
        //Log.i(TAG,"BIND VIEW "+String.valueOf(position)+" !!!!");
        final ViewHolder vh = (ViewHolder)holder;
        long millis = mDataset.get(position);
        int sec = (int)millis/1000;
        int hundreds = (int)Math.floor((millis-(sec*1000))/10);

        //Construct the timer text
        StringBuilder timestamp = new StringBuilder();
        if(sec<10)timestamp.append(" ");
        timestamp.append(String.valueOf(sec));
        timestamp.append(":");
        if(hundreds<10)timestamp.append("0");
        timestamp.append(hundreds);

        vh.mView.setText(timestamp);
        //Set timer color
        if(position == 0){
            if(mDataset.size()>1 && millis>=mDataset.get(1)) {
                vh.mView.setTextColor(colorFirst);
            }else{
                vh.mView.setTextColor(colorMissed);
            }
        }
    }

    @Override
    public int getItemCount() {
        //Return the size of our dataset
        return mDataset.size();
    }
}
