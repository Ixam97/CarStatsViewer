package com.ixam97.carStatsViewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;

public class PlotView extends View {

    public PlotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setupPaint();
    }

    private final ArrayList<Float> DataPoints = new ArrayList<>();

    // defines paint and canvas
    private Paint basePaint;
    private Paint drawPaint;
    private Paint averagePaint;

    private float averageValue = 0f;

    // Setup paint with color and stroke styles
    private void setupPaint() {
        basePaint = new Paint();
        basePaint.setColor(Color.LTGRAY);
        basePaint.setAntiAlias(true);
        basePaint.setStrokeWidth(1);
        basePaint.setStyle(Paint.Style.STROKE);
        basePaint.setStrokeJoin(Paint.Join.ROUND);
        basePaint.setStrokeCap(Paint.Cap.ROUND);

        drawPaint = new Paint();

        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);

        drawPaint.setColor(typedValue.data);
        drawPaint.setAntiAlias(true);
        drawPaint.setStrokeWidth(6);
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setStrokeJoin(Paint.Join.ROUND);
        drawPaint.setStrokeCap(Paint.Cap.ROUND);

        averagePaint = new Paint();

        averagePaint.setColor(typedValue.data);
        averagePaint.setAntiAlias(true);
        averagePaint.setStrokeWidth(3);
        averagePaint.setPathEffect(new DashPathEffect(new float[]{5, 10}, 0));
        averagePaint.setStyle(Paint.Style.STROKE);
        averagePaint.setStrokeJoin(Paint.Join.ROUND);
        averagePaint.setStrokeCap(Paint.Cap.ROUND);
    }

    public void addDataPoint(float dataPoint)
    {
        if (DataPoints.size() > 30)
        {
            DataPoints.remove(0);
        }

        DataPoints.add(dataPoint);
        invalidate();
    }

    public void updateAverage(float newAverageValue)
    {
        averageValue = newAverageValue;
        invalidate();
    }

    private float XCord(int position, int items, float maxX)
    {
        return maxX / (items -1) * position;
    }

    private float YCord(float value, float minP, float maxP, float maxY)
    {
        return Math.abs((maxY / (maxP - minP) * (value - minP) - maxY));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DataPoints.size() == 0) return;

        int maxX = getWidth();
        int maxY = getHeight();

        ArrayList<Float> clonePoints = (ArrayList<Float>) DataPoints.clone();

        float minP = Math.min(Math.min(Collections.min(clonePoints), averageValue), -20f);
        float maxP = Math.max(Math.max(Collections.max(clonePoints), averageValue), 50f);

        Path basePath = new Path();
        basePath.moveTo(0f, YCord(0, minP, maxP, maxY));
        basePath.lineTo(maxX, YCord(0, minP, maxP, maxY));

        Path fiftyPath = new Path();
        fiftyPath.moveTo(0f, YCord(500, minP, maxP, maxY));
        fiftyPath.lineTo(maxX, YCord(500, minP, maxP, maxY));

        Path averagePath = new Path();
        averagePath.moveTo(0f, YCord(averageValue, minP, maxP, maxY));
        averagePath.lineTo(maxX, YCord(averageValue, minP, maxP, maxY));

        canvas.drawPath(basePath, basePaint);
        canvas.drawPath(fiftyPath, basePaint);
        canvas.drawPath(averagePath, averagePaint);

        ArrayList<Point> points = new ArrayList<>();

        for (int i = 0; i < clonePoints.size(); i += 1)
        {
            points.add(new Point((int)XCord(i, clonePoints.size(), maxX), (int)YCord(clonePoints.get(i), minP, maxP, maxY)));
        }

        Path path = new Path();

        if (points.size() > 1) {
            Point prevPoint = null;
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);

                if (i == 0) {
                    path.moveTo(point.x, point.y);
                } else {
                    float midX = (prevPoint.x + point.x) / 2;
                    float midY = (prevPoint.y + point.y) / 2;

                    if (i == 1) {
                        path.lineTo(midX, midY);
                    } else {
                        path.quadTo(prevPoint.x, prevPoint.y, midX, midY);
                    }
                }
                prevPoint = point;
            }
            path.lineTo(prevPoint.x, prevPoint.y);
        }

        canvas.drawPath(path, drawPaint);
    }
}