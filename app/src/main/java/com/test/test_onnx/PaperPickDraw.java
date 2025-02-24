package com.test.test_onnx;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;



public class PaperPickDraw extends View implements Runnable{

    public PaperPickDraw(Context context) {
        super(context);
    }
    public PaperPickDraw(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }
    public PaperPickDraw(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    public PaperPickDraw(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }


    private OnnxResult[] papers;

    public void setResults(OnnxResult[] papers){
        this.papers = papers;
        postInvalidate();
    }
    public void clearPapers(){
        papers = null;
        postInvalidate();
    }


    private static final long focusShowTime = 1000;
    private float focusX = 0, focusY = 0;
    private long lastFocus = 0;
    public void setFocus(float x, float y){
        focusX = x;
        focusY = y;
        lastFocus = System.currentTimeMillis();
        postInvalidate();
        postDelayed(this, focusShowTime);
    }
    @Override
    public void run() {
        invalidate();
    }

    private final Paint paint = new Paint();
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        paint.setStyle(Paint.Style.STROKE);
        if(papers != null){
            paint.setColor(Color.BLUE);
            paint.setStrokeWidth(7);
            for(OnnxResult obj : papers){
                canvas.drawRoundRect(obj.getRectf(),20, 20, paint);
            }
        }

        if(System.currentTimeMillis() - lastFocus < focusShowTime){
            paint.setColor(getResources().getColor(R.color.white));
            paint.setStrokeWidth(5);
            canvas.drawRoundRect(focusX - 75, focusY - 75, focusX + 75, focusY + 75,20, 20, paint);
        }
    }


}
