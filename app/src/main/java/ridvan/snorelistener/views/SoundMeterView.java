package ridvan.snorelistener.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;

public class SoundMeterView extends View {
    private int                 minLevel;
    private int                 currentLevel;
    private Paint               paint;
    private LinkedList<Integer> radiuses;
    private int visibleLevelCount = 5;
    private int oldLevel;

    public SoundMeterView(Context context) {
        super(context);
        radiuses = new LinkedList<>();
    }

    public SoundMeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        radiuses = new LinkedList<>();
    }

    public SoundMeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        radiuses = new LinkedList<>();
    }

    public int getVisibleLevelCount() {
        return visibleLevelCount;
    }

    public void setVisibleLevelCount(int visibleLevelCount) {
        this.visibleLevelCount = visibleLevelCount;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public void setMinLevel(int minLevel) {
        this.minLevel = minLevel;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        if (oldLevel == currentLevel) return;
        this.currentLevel = currentLevel;

        radiuses.add(currentLevel);
        if (radiuses.size() > visibleLevelCount) radiuses.removeFirst();

        oldLevel = currentLevel;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = getOrCreatePaint();

        for (int i = 0, limit = radiuses.size(), remaining = limit; i < limit; i++, remaining--) {
            int radius = radiuses.get(i) + 8 * remaining;
            int alpha  = (255 * i / 4);
            paint.setAlpha(alpha);

            float rad = radius * getRate();
            if (rad < minLevel / 2) continue;

            canvas.drawCircle(centerX(), centerY(), radius * getRate(), paint);
        }
    }

    private float getRate() {
        return Math.min(getWidth(), getHeight()) / 170;
    }

    private float centerX() {
        return getWidth() / 2;
    }

    private float centerY() {
        return getHeight() / 2;
    }

    private Paint getOrCreatePaint() {
        if (paint != null) return paint;

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setAntiAlias(true);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setColor(Color.GRAY);
        paint.setStrokeWidth(2);
        paint.setStyle(Paint.Style.STROKE);

        setLayerType(2, paint);

        return paint;
    }
}
