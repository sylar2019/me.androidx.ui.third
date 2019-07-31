package com.komodo.tagin;
/**
 * Komodo Lab: Tagin! Project: 3D Tag Cloud
 * Google Summer of Code 2011
 *
 * @authors Reza Shiftehfar, Sara Khosravinasr and Jorge Silva
 */

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TagCloudView extends RelativeLayout {

    public abstract interface OnTagClickCallback {
        void onTagClick(Tag tag);
    }

    public static float[] convertColorArray(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);

        float[] f = {r / 255f, g / 255f, b / 255f, a / 255f};
        return f;
    }

    final float TOUCH_SCALE_FACTOR = 0.8f;  //0.8f
    final float TRACKBALL_SCALE_FACTOR = 10; //10
    final float[] tempColor1 = {0.9412f, 0.7686f, 0.2f, 1};
    //rgb Alpha
    //{1f,0f,0f,1}  red
    //{0.3882f,0.21568f,0.0f,1} orange
    //{0.9412f,0.7686f,0.2f,1} light orange
    final float[] tempColor2 = {1f, 0f, 0f, 1};
    //rgb Alpha
    //{0f,0f,1f,1}  blue
    //{0.1294f,0.1294f,0.1294f,1} grey
    //{0.9412f,0.7686f,0.2f,1} light orange

    float mAngleX = 0;
    float mAngleY = 0;
    float centerX, centerY;
    float radius;
    int shiftLeft;
    int textSizeMin = 6;
    int textSizeMax = 34;
    float tspeed = 1;

    float dx = 100;
    float dy = 100;
//    boolean isAuto = true;
//    float oldX, oldY;

    Context mContext;
    TagCloud mTagCloud;
    List<TextView> mTextView;
    List<RelativeLayout.LayoutParams> mParams;
    OnTagClickCallback tagClickCallback;


    public TagCloudView(Context context) {
        super(context);
        init(context, null);
    }

    public TagCloudView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TagCloudView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = measure(widthMeasureSpec);
        int height = measure(heightMeasureSpec);


        //set the center of the sphere on center of our screen:
        centerX = width / 2;
        centerY = height / 2;
        radius = Math.min(centerX * 0.95f, centerY * 0.95f); //use 95% of screen
        //since we set tag margins from left of screen, we shift the whole tags to left so that
        //it looks more realistic and symmetric relative to center of screen in X direction
        shiftLeft = (int) (Math.min(centerX * 0.15f, centerY * 0.15f));

        mTagCloud.setRadius((int) radius);
    }

    private int measure(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);
        if (specMode == MeasureSpec.UNSPECIFIED) {
            result = 200;
        } else {
            result = specSize;
        }
        return result;
    }


    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    void init(Context cx, AttributeSet attrs) {
        this.mContext = cx;
        mTagCloud = new TagCloud();
        mTagCloud.setTextSize(textSizeMin, textSizeMax);
        mTagCloud.setTagColor1(tempColor1);
        mTagCloud.setTagColor2(tempColor2);

        /** 用来自动播放的*/
        autoPlay();
    }

    public void loadTags(List<Tag> tagList) {
        loadTags(tagList, this.textSizeMin, this.textSizeMax, this.tempColor1, this.tempColor2);
    }

    public void loadTags(List<Tag> tagList, int textSizeMin, int textSizeMax, float[] color1, float[] color2) {
        mTagCloud.setTextSize(textSizeMin, textSizeMax);
        mTagCloud.setTagColor1(color1);
        mTagCloud.setTagColor2(color2);

        removeAllViews();
        mTextView = new ArrayList<TextView>();
        mParams = new ArrayList<RelativeLayout.LayoutParams>();

        tagList = filter(tagList);
        mTagCloud.setTags(filter(tagList));


        //update the transparency/scale of tags
        mTagCloud.setAngleX(mAngleX);
        mTagCloud.setAngleY(mAngleY);
        mTagCloud.update();

        //Now Draw the 3D objects: for all the tags in the TagCloud
        Iterator it = mTagCloud.iterator();
        Tag tag;
        int i = 0;

        while (it.hasNext()) {
            tag = (Tag) it.next();
            tag.setParamNo(i);

            //LayoutParams
            RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
            lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            mParams.add(lp);

            //TextView
            TextView txtView = new TextView(mContext);
            txtView.setLayoutParams(lp);
            txtView.setOnClickListener(OnTagClickListener(tag));
            mTextView.add(txtView);

            //
            setTextViewByTag(txtView, tag);
            addView(txtView);

            i++;
        }


    }

    public void setOnTagClickCallback(OnTagClickCallback callback) {
        this.tagClickCallback = callback;
    }

    public void addTag(final Tag tag) {
        mTagCloud.add(tag);
        int i = mTextView.size();
        tag.setParamNo(i);

        //LayoutParams
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
        lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mParams.add(lp);

        //TextView
        TextView txtView = new TextView(mContext);
        txtView.setLayoutParams(lp);
        txtView.setOnClickListener(OnTagClickListener(tag));

        mTextView.add(txtView);

        //
        setTextViewByTag(txtView, tag);
        addView(txtView);
    }

    public boolean Replace(Tag newTag, String oldTagText) {
        boolean result = false;
        int j = mTagCloud.Replace(newTag, oldTagText);
        if (j >= 0) { //then oldTagText was found and replaced with newTag data
            refresh();
            result = true;
        }
        return result;
    }

    public void reset() {
        mTagCloud.reset();
        refresh();
    }

    @Override
    public boolean onTrackballEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        mAngleX = (y) * tspeed * TRACKBALL_SCALE_FACTOR;
        mAngleY = (-x) * tspeed * TRACKBALL_SCALE_FACTOR;
        refresh();

        return true;
    }

//    @Override
//    public boolean dispatchTouchEvent(MotionEvent e) {
//
//        float x = e.getX();
//        float y = e.getY();
//        boolean result = true;
//        if (e.getAction() == MotionEvent.ACTION_MOVE) {
//            onTouchEvent(e);
//        } else {
//            oldX = x;
//            oldY = y;
//        }
//
//        result = super.dispatchTouchEvent(e);
//        return result;
//    }
//
//    @Override
//    public boolean onTouchEvent(MotionEvent e) {
//
//        float x = e.getX();
//        float y = e.getY();
//
//        switch (e.getAction()) {
//            case MotionEvent.ACTION_DOWN:
//                isAuto = false;
//                oldX = x;
//                oldY = y;
//                break;
//
//            case MotionEvent.ACTION_UP:
//
//
//                dx = oldX - x;
//                dy = oldY - y;
//                oldX = x;
//                oldY = y;
//                isAuto = true;
//                break;
//
//            case MotionEvent.ACTION_MOVE:
//                dx = oldX - x;
//                dy = oldY - y;
//                oldX = x;
//                oldY = y;
//                mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
//                mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;
//                refresh();
//
//                break;
//
//        }
//
//        return true;
//    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {

            case MotionEvent.ACTION_DOWN:
                dx = 0;
                dy = 0;
                break;
            case MotionEvent.ACTION_UP:
                dx = x - centerX;
                dy = y - centerY;
                mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
                mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;
                break;
            case MotionEvent.ACTION_MOVE:
                dx = x - centerX;
                dy = y - centerY;
                mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
                mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;
                refresh();

                break;

        }

        return true;
    }

    void autoPlay() {
        postDelayed(new Runnable() {
            @Override
            public void run() {

//                if (isAuto)
                {
                    mAngleX = (dy / radius) * tspeed * TOUCH_SCALE_FACTOR;
                    mAngleY = (-dx / radius) * tspeed * TOUCH_SCALE_FACTOR;
                    refresh();
                }

                postDelayed(this, 50);
            }
        }, 50);
    }

    void refresh() {
        if (mTagCloud == null || mTextView == null) return;

        mTagCloud.setAngleX(mAngleX);
        mTagCloud.setAngleY(mAngleY);
        mTagCloud.update();

        Iterator<?> it = mTagCloud.iterator();
        Tag tempTag;
        TextView txtView;

        while (it.hasNext()) {
            tempTag = (Tag) it.next();
            txtView = mTextView.get(tempTag.getParamNo());
            refreshByTag(txtView, tempTag);
        }
    }

    void refreshByTag(TextView txtView, Tag tag) {
        setTextViewByTag(txtView, tag);
        txtView.bringToFront();
    }

    void setTextViewByTag(TextView txtView, Tag tag) {
        RelativeLayout.LayoutParams lp = mParams.get(tag.getParamNo());
        lp.setMargins(
                (int) (centerX - shiftLeft + tag.getLoc2DX()),
                (int) (centerY + tag.getLoc2DY()),
                0,
                0);

        int mergedColor = Color.argb((int) (tag.getAlpha() * 255),
                (int) (tag.getColorR() * 255),
                (int) (tag.getColorG() * 255),
                (int) (tag.getColorB() * 255));

        txtView.setTextColor(mergedColor);
        txtView.setTextSize((int) (tag.getTextSize() * tag.getScale()));
        txtView.setText(tag.getText());
        txtView.setSingleLine(true);

    }


    //the filter function makes sure that there all elements are having unique Text field:
    List<Tag> filter(List<Tag> tagList) {
        //current implementation is O(n^2) but since the number of tags are not that many,
        //it is acceptable.
        List<Tag> tempTagList = new ArrayList();
        Iterator itr = tagList.iterator();
        Iterator itrInternal;
        Tag tempTag1, tempTag2;
        //for all elements of TagList
        while (itr.hasNext()) {
            tempTag1 = (Tag) (itr.next());
            boolean found = false;
            //go over all elements of temoTagList
            itrInternal = tempTagList.iterator();
            while (itrInternal.hasNext()) {
                tempTag2 = (Tag) (itrInternal.next());
                if (tempTag2.getText().equalsIgnoreCase(tempTag1.getText())) {
                    found = true;
                    break;
                }
            }
            if (found == false)
                tempTagList.add(tempTag1);
        }
        return tempTagList;
    }


    OnClickListener OnTagClickListener(final Tag tag) {
        return new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (tagClickCallback != null) {
                    tagClickCallback.onTagClick(tag);
                }
            }
        };
    }

}
