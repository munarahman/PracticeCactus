package com.practicecactus.practicecactus.Cactus;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import com.practicecactus.practicecactus.R;

/**
 * Created by matthew on 2016-03-05.
 */
public class CactusMoodView extends View {
    private static final float BACKGROUND_RECT_R = 20;
    private static final int BACKGROUND_STROKE_WIDTH = 10;

    private float mood;
    private int height, width;
    private int barHeight, barWidth, barX, barY;

    private int NEG_COLOR_RES = R.color.negative_indicator;
    private int POS_COLOR_RES = R.color.positive_indicator;
    private Paint mBackgroundPaint;
    private Paint mOutlinePaint;
    private Paint mBarPaint;

    public CactusMoodView(Context context, AttributeSet attrs) {
        super(context, attrs);
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CactusMoodView,
                0, 0);
        try {
            mood = a.getFloat(R.styleable.CactusMoodView_mood_value, 0);
        } finally {
            a.recycle();
        }
        
        this.initialize();
    }

    private void initialize() {
        mBackgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBarPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setColor(getResources().getColor(R.color.colorSecondary));
        mOutlinePaint.setStrokeWidth(BACKGROUND_STROKE_WIDTH);

        mBackgroundPaint.setStyle(Paint.Style.FILL);
        mBackgroundPaint.setColor(getResources().getColor(R.color.colorSecondaryDark));

        mBarPaint.setStyle(Paint.Style.FILL);
        mBarPaint.setColor(this.getBarColor());
    }

    private int getBarColor() {
        int pos =  getResources().getColor(POS_COLOR_RES);
        int neg = getResources().getColor(NEG_COLOR_RES);


        int r_pos = Color.red(pos);
        int g_pos = Color.green(pos);
        int b_pos = Color.blue(pos);
        int r_neg = Color.red(neg);
        int g_neg = Color.green(neg);
        int b_neg = Color.blue(neg);

        int r = this.interpolateComp(r_pos, r_neg);
        int g = this.interpolateComp(g_pos, g_neg);
        int b = this.interpolateComp(b_pos, b_neg);
        return Color.rgb(r,g,b);
    }

    private int interpolateComp(int pos, int neg) {
        float alpha = (float) Math.sqrt((double) this.mood);
        return Math.round(alpha * pos + (1-alpha) * neg);
    }

    public float getMood() {
        return mood;
    }

    public void setMood(float value) {
        if (value <= 1 && value >= 0) {
            this.mood = value;
            mBarPaint.setColor(this.getBarColor());
            int barOff = BACKGROUND_STROKE_WIDTH;
            barWidth = Math.round((width - barOff) * mood);

            invalidate();
            requestLayout();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = w;
        height = h;

        int barOff = BACKGROUND_STROKE_WIDTH;
        barHeight = h - barOff;
        barWidth = Math.round((w - barOff) * mood);
        barX = BACKGROUND_STROKE_WIDTH;
        barY = BACKGROUND_STROKE_WIDTH;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // Draw the background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect((float) 0, (float) 0,
                    (float) width, (float) height,
                    BACKGROUND_RECT_R, BACKGROUND_RECT_R,
                    mBackgroundPaint);

        } else {
            canvas.drawRect((float) 0, (float) 0,
                    (float) width, (float) height,
                    mBackgroundPaint);
        }

        //Draw the progress bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(barX, barY,
                    barWidth, barHeight,
                    BACKGROUND_RECT_R, BACKGROUND_RECT_R,
                    mBarPaint);
        } else {
            canvas.drawRect(barX, barY,
                    barWidth, barHeight,
                    mBarPaint);
        }

        //Draw the outline
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            canvas.drawRoundRect(0, 0,
                    width, height,
                    BACKGROUND_RECT_R, BACKGROUND_RECT_R,
                    mOutlinePaint);
        } else {
            canvas.drawRect(0, 0,
                    width, height,
                    mOutlinePaint);
        }

    }
}
