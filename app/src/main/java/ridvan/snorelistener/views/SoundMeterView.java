package ridvan.snorelistener.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.LinkedList;

/**
 * SoundMeterView, is just a normal empty view with circles of sound levels, handled by functions
 */
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
        // If old level is the same with current, return
        if (oldLevel == currentLevel) return;
        this.currentLevel = currentLevel;

        // Add this level as radius into the radiuses
        radiuses.add(currentLevel);

        // If we have more than visible levels, remove firstly added level
        if (radiuses.size() > visibleLevelCount) radiuses.removeFirst();

        // Set old level as newly given level
        oldLevel = currentLevel;

        // Force redraw this view
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Paint paint = getOrCreatePaint();

        // For each index i up-to radiuses size as limit, with remaining index that comes from
        // the end of the list, by increasing index i and decreasing remaining index
        for (int i = 0, limit = radiuses.size(), remaining = limit; i < limit; i++, remaining--) {
            // Get radius and push 8 * remaining pixel back to indicate the order
            int radius = radiuses.get(i) + 8 * remaining;

            // Transparency of the circle
            int alpha = (255 * i / 4);
            paint.setAlpha(alpha);

            // Balance size as visible size of the view
            float rad = radius * getRate();

            // If radius is smaller than the view from the center, since it will be drawn
            // back of the view, we may pass it
            if (rad < minLevel / 2) continue;

            // Draw circle from center, by given radius, by paint
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

    /**
     * Creates a anti-aliased, join-rounded, gray color initialized, 2-width stroke having paint
     *
     * @return Paint object
     */
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
