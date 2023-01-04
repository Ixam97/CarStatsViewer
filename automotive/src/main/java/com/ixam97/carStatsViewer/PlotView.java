package com.ixam97.carStatsViewer;

import static com.ixam97.carStatsViewer.PlotPaint.*;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlotView extends View {
    public PlotView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setFocusable(true);
        setFocusableInTouchMode(true);
        setupPaint();
    }

    private final float TextSize = 26f;

    private final int XMargin = 150;
    private int XLineCount = 6;

    private final int YMargin = 60;
    private int YLineCount = 4;

    private Integer DisplayItemCount = null;

    private Date StartDate;
    private Date CurrentDate;

    private final ArrayList<PlotLine> PlotLines = new ArrayList<>();
    private final ArrayList<PlotPaint> PlotPaint = new ArrayList<>();

    private Paint labelPaint;
    private Paint labelLinePaint;

    // Setup paint with color and stroke styles
    private void setupPaint() {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.colorControlActivated, typedValue, true);

        // defines paint and canvas
        Paint basePaint = Companion.basePaint();

        labelLinePaint = new Paint(basePaint);
        labelLinePaint.setColor(Color.DKGRAY);

        labelPaint = new Paint(labelLinePaint);
        labelPaint.setStyle(Paint.Style.FILL);

        List<Integer> PlotColors = Arrays.asList(
                Color.BLACK,
                Color.GREEN,
                Color.CYAN,
                Color.BLUE,
                Color.RED
        );

        for (Integer color : PlotColors) {
            if (color == Color.BLACK) {
                color = typedValue.data;
            }

            PlotPaint.add(Companion.byColor(color));
        }
    }

    public void reset() {
        for (PlotLine item : PlotLines) {
            item.reset();
        }

        StartDate = new Date();
        CurrentDate = null;

        invalidate();
    }

    public void addPlotLine(PlotLine plotLine) {
        if (plotLine.getPlotPaint() == null) {
            plotLine.setPlotPaint(PlotPaint.get(PlotLines.size()));
        }

        PlotLines.add(plotLine);

        invalidate();
    }

    public void removePlotLine(PlotLine plotLine) {
        PlotLines.remove(plotLine);
        invalidate();
    }

    public void setDataPoints(ArrayList<Float> primaryDataPoints) {
        setDataPoints(primaryDataPoints, null);
    }

    public void setDataPoints(ArrayList<Float> primaryDataPoints, ArrayList<Float> secondaryDataPoints) {
        if (PlotLines.size() >= 1) PlotLines.get(0).setDataPoints(primaryDataPoints);
        if (PlotLines.size() >= 2) PlotLines.get(1).setDataPoints(secondaryDataPoints);

        CurrentDate = new Date();
        invalidate();
    }

    public void addDataPoints(ArrayList<Float> primaryDataPoints) {
        addDataPoints(primaryDataPoints, null);
    }

    public void addDataPoints(ArrayList<Float> primaryDataPoints, ArrayList<Float> secondaryDataPoints) {
        if (PlotLines.size() >= 1) PlotLines.get(0).addDataPoints(primaryDataPoints);
        if (PlotLines.size() >= 2) PlotLines.get(1).addDataPoints(secondaryDataPoints);

        CurrentDate = new Date();
        invalidate();
    }

    public void addDataPoint(Float primaryDataPoint) {
        addDataPoint(primaryDataPoint, null);
    }

    public void addDataPoint(Float primaryDataPoint, Float secondaryDataPoint) {
        if (PlotLines.size() >= 1) PlotLines.get(0).addDataPoint(primaryDataPoint);
        if (PlotLines.size() >= 2) PlotLines.get(1).addDataPoint(secondaryDataPoint);

        CurrentDate = new Date();
        invalidate();
    }

    public void setDisplayItemCount(Integer displayItemCount) {
        for (PlotLine line : PlotLines) {
            line.setDisplayItemCount(displayItemCount);
        }
        DisplayItemCount = displayItemCount;
        invalidate();
    }

    public void setYLineCount(Integer lineCount) {
        if (lineCount < 2) return;
        YLineCount = lineCount;
        invalidate();
    }

    public void setXLineCount(Integer lineCount) {
        if (lineCount < 2) return;
        XLineCount = lineCount;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        DrawXLines(canvas);
        DrawYLines(canvas);
        DrawPlot(canvas);
    }

    private void DrawPlot(Canvas canvas) {
        float maxX = canvas.getWidth();
        float maxY = canvas.getHeight();

        for (PlotLine line : PlotLines) {
            if (line.isEmpty()) continue;

            List<Float> items = line.getDataPoints();

            if (items.size() <= 0) {
                return;
            }

            ArrayList<PlotPoint> plotPoints = new ArrayList<>();

            if (DisplayItemCount != null) {
                for (int i = 0; i < DisplayItemCount - items.size(); i++) {
                    plotPoints.add(null);
                }
            }

            int index = plotPoints.size();
            for (Float item : items) {
                plotPoints.add(new PlotPoint(line.x(index++, XMargin, maxX), line.y(item, YMargin, maxY)));
            }

            Path path = new Path();
            PlotPoint prevPoint = null;

            int virtualIndex = 0;
            for (int i = 0; i < plotPoints.size(); i++) {
                PlotPoint point = plotPoints.get(i);

                if (point == null) continue;

                if (virtualIndex == 0) {
                    path.moveTo(point.getX(), point.getY());
                    if (items.size() == 1) {
                        path.lineTo(point.getX(), point.getY());
                    }
                } else {
                    float midX = (prevPoint.getX() + point.getX()) / 2;
                    float midY = (prevPoint.getY() + point.getY()) / 2;

                    if (virtualIndex == 1) {
                        path.lineTo(midX, midY);
                    } else {
                        path.quadTo(prevPoint.getX(), prevPoint.getY(), midX, midY);
                    }
                }

                prevPoint = point;
                virtualIndex++;
            }

            path.lineTo(prevPoint.getX(), prevPoint.getY());

            canvas.drawPath(path, line.getPlotPaint().getPlot());
        }
    }

    private void DrawXLines(Canvas canvas) {
        float maxX = canvas.getWidth();
        float maxY = canvas.getHeight();

        for (int i = 0; i <= XLineCount - 1; i++) {
            String label = "";
            if (DisplayItemCount != null) {
                label = String.format("%dkm", Math.abs(((DisplayItemCount - 1) / 10) - ((i * (DisplayItemCount - 1) / (XLineCount - 1)) / 10)));
            } else if (StartDate != null && CurrentDate != null) {
                long diff = TimeUnit.SECONDS.convert(Math.abs(StartDate.getTime() - CurrentDate.getTime()), TimeUnit.MILLISECONDS);
                float x = ((float) diff / (XLineCount - 1)) * i;
                label = String.format("%02d:%02d", (int) (x / 60), (int) (x % 60));
            }

            float xCord = PlotLine.Companion.x(i, XLineCount, XMargin, maxX);
            float yCord = maxY - YMargin;

            Rect bounds = new Rect();
            labelPaint.getTextBounds(label, 0, label.length(), bounds);

            canvas.drawText(label, xCord - (bounds.width() / 2), yCord + (YMargin / 2) + (bounds.height() / 2), labelPaint);

            Path path = new Path();
            path.moveTo(xCord, YMargin);
            path.lineTo(xCord, yCord);

            canvas.drawPath(path, labelLinePaint);
        }
    }

    private void DrawYLines(Canvas canvas) {
        float maxX = canvas.getWidth();
        float maxY = canvas.getHeight();

        for (int i = 0; i <= YLineCount - 1; i++) {
            float cordY = PlotLine.Companion.x(i, YLineCount, YMargin, maxY);

            Path path = new Path();
            path.moveTo(XMargin, cordY);
            path.lineTo(maxX - XMargin, cordY);
            canvas.drawPath(path, labelLinePaint);
        }

        for (PlotLine line : PlotLines) {
            if (line.isEmpty()) continue;

            Rect bounds = new Rect();
            labelPaint.getTextBounds("Dummy", 0, "Dummy".length(), bounds);

            float labelShiftY = bounds.height() / 2;
            float valueShiftY = (line.max() - line.min()) / (YLineCount - 1);

            Float labelCordX = null;
            if (line.getLabelPosition() == PlotLabelPosition.LEFT) labelCordX = TextSize;
            if (line.getLabelPosition() == PlotLabelPosition.RIGHT) labelCordX = maxX - XMargin + TextSize;

            Float highlightCordY = line.y(line.highlight(), YMargin, maxY);
            Float baseCordY = line.y(0f, YMargin, maxY);

            if (line.getLabelPosition() != PlotLabelPosition.NONE) {
                for (int i = 0; i <= YLineCount - 1; i++) {
                    float valueY = line.max() - (i * valueShiftY);
                    float cordY = line.y(valueY, YMargin, maxY);

                    String label = String.format(line.getLabelFormat(), valueY / line.getDivider());

                    if (highlightCordY == null || Math.abs(cordY - highlightCordY) > TextSize) {
                        canvas.drawText(label, labelCordX, cordY + labelShiftY, labelPaint);
                    }
                }
            }

            if (labelCordX != null) {
                canvas.drawText(String.format(line.getHighlightFormat(), line.highlight() / line.getDivider()), labelCordX, highlightCordY + labelShiftY, line.getPlotPaint().getHighlightLabel());
            }

            if (highlightCordY != null && line.getHighlightMethod() == PlotHighlightMethod.AVG) {
                Path highlightPath = new Path();
                highlightPath.moveTo(XMargin, highlightCordY);
                highlightPath.lineTo(maxX - XMargin, highlightCordY);

                canvas.drawPath(highlightPath, line.getPlotPaint().getHighlightLabelLine());
            }

            if (baseCordY != null) {
                Path basePath = new Path();
                basePath.moveTo(XMargin,  baseCordY);
                basePath.lineTo(maxX - XMargin, baseCordY);
                canvas.drawPath(basePath, labelLinePaint);
            }
        }
    }
}