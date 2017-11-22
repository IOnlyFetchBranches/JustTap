package com.justtap.comp;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.RelativeLayout;

import com.justtap.R;

public class Warp extends android.support.v7.widget.AppCompatImageView{

    //Type selection
    private static String[] Types = {"NORM", "BLKHOLE", "WRMHOLE"};
    //Graphics Prefs
    private static int[] Colors; //{TEXT COLOR, WARP COLOR}
    //Time of creation (Called to see how long it took to hit
    private final long createTime=System.currentTimeMillis();
    private String type;
    //Ref to parent graphics handler
    private GraphicsHandler parent;
    private Bitmap image;

    //Layout params of the final position of the warp
    //Only populated by graphics handler calls (gen warp)
    //THIS WILL RETURN NULL BY DEFAULT IF YOU CREATE YOUR OWN WARP;
    private RelativeLayout.LayoutParams position = null;
    //was this warp missed?
    private boolean missed = false; // Maybe default should be True? idk comeback to later!

    //CONTEXT CANNOT BE MADE STATIC
    private Context callingActivityContext;


    //Primary constructor
    public Warp(int diffLevel, Context context, GraphicsHandler parent) {
        super(context);

        this.parent=parent; //link to the graphics handler
        setType(diffLevel); //Set Type;


        //Graphics handling
        Colors = LogicEngine.getColorScheme(false);



    }

    //This funtion ensures the size of the item is kept valid
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int dwidth = 250;
        int dheight = 250;

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int mwidth = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int mheight = MeasureSpec.getSize(heightMeasureSpec);

        Log.i("WARP => ", "Measured W H" + mwidth + " " + mheight);


        int width, height;

        width = dwidth;
        height = dheight;

        setMeasuredDimension(width, height);

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        Log.e("WARP =>", "Pos L T R B:" + left + " " + top + " " + right + " " + bottom);

    }

    //This function is called when the user taps the created drawable, Returns the amount of milliseconds it took them.
    long pop() {
        return System.currentTimeMillis() - createTime;

    }

    RelativeLayout.LayoutParams getPosition() {
        return position;
    }

    void setPosition(@NonNull RelativeLayout.LayoutParams position) {
        this.position = position;
    }

    String getType() {
        return type;
    }

    //Type handling
    Warp setType(int level) {
        type = Types[(int) (Math.random() * level)];
        //Determine Drawable used for warp
        if (type.equals("NORM")) {
            this.setImageDrawable(getResources().getDrawable(R.mipmap.warp_norm));
            //Also update internal bitmap for canvas correction
            image = BitmapFactory.decodeResource(getResources(), R.mipmap.warp_norm);
        }
        Log.i("WARP =>", "Set type " + type);
        return this;
    }

    void markMissed() {
        missed = true; //Called by logic engine when user has missed ;}
    }

    boolean isMissed() {
        return missed;
    }





}
