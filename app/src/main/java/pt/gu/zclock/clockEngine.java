package pt.gu.zclock;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by GU on 25-12-2014.
 */
public class clockEngine {

    //region fields and variables
    //Constants
    static final int CLOCKMODE_DAYDOWN = 0;
    static final int CLOCKMODE_DAYUP = 1;

    //public fields
    public int clockMode = CLOCKMODE_DAYDOWN;
    public int szUnits = TypedValue.COMPLEX_UNIT_DIP;
    public PointF szClock = new PointF(250f, 250f);
    public float szRad = 125f;
    public PointF szCenter = new PointF(szRad, szRad);
    public float szPad = 3f;
    public float szFrame = 0.5f;
    public float szPtrWidth = 0.2f;
    public float frPtrHeight = 0.5f;
    public int resTimeMins = 2;
    public float frTimeMins = 0.1f;
    public float szMainTime = 54f;
    public float szTimeLabPad = 10f;

    public int cFrame = 0xff00c3ff;
    public int cFrameOff = 0x1000c3ff;
    public int cPtr = 0xff00c3ff;
    public int cMainTime = 0xffffffff;

    public Typeface tfMainTime = Typeface.create("sans-serif-light", Typeface.NORMAL);
    public long newDayTimeMilis = 0;
    public Bitmap backgroundBitmap = null;

    List<labTime> timeLabels = new ArrayList<>();
    List<labEvent> eventLabels = new ArrayList<>();

    //private fields
    private boolean bInnerFieldsUpdated = false;
    private Point pxClock, pxCenter;
    private int pxRad, pxPad, pxFrame, pxPtrWidth, pxTimeLabPad, pxMainTime;
    private float dp = Resources.getSystem().getDisplayMetrics().density;
    private PathEffect patFrame;
    private float angleModeOffset = 90f;
    private float newdaytime_angle = 0;
    private float[] textMarksMaxWidth = new float[]{22f * dp, 0};
    //endregion

    //region inner classes

    class labTime {
        private Path path;
        Date time;
        float size;
        int color;
        Typeface typeface;
        String format;
        String label;
        boolean alignRight;
        boolean placement;

        labTime(Date time, float fontSize, int fontColor, Typeface fontTypeface, String label, String timeFormat, boolean placement) {
            this.time = time;
            this.size = fontSize;
            this.color = fontColor;
            this.typeface = fontTypeface;
            this.format = timeFormat;
            this.label = label;
            this.placement = placement;
        }

        @Override
        public String toString() {
            SimpleDateFormat s = new SimpleDateFormat(this.format);
            return s.format(this.time);
        }

        public float toGradAngle() {
            float f = (this.time.getTime() / 60000f) % 1440;
            return f * 360f / 1440f;
        }

        labTime addHours(int hours) {
            this.time = new Date(this.time.getTime() + 3600000 * hours);
            return this;
        }

        labTime addMinutes(int minutes) {
            this.time = new Date(this.time.getTime() + 60000 * minutes);
            return this;
        }
    }

    class labEvent {
        String label;
        float size;
        int color;
        Typeface typeface;
        float pad;

        labEvent(String label, float fontSize, int fontColor, Typeface fontTypeface, float pad) {
            this.label = label;
            this.size = fontSize;
            this.color = fontColor;
            this.typeface = fontTypeface;
            this.pad = pad;
        }
    }

    //endregion

    //Constructor
    public clockEngine(PointF clockSize, int clockMode, int szUnits) {
        this.szClock = clockSize;
        this.clockMode = clockMode;
        this.szUnits = szUnits;
        if (!bInnerFieldsUpdated) updateSizeFields(this.szUnits);
    }

    public clockEngine(PointF clockSize, int clockMode) {
        this.szClock = clockSize;
        this.clockMode = clockMode;
        this.szUnits = TypedValue.COMPLEX_UNIT_DIP;
        if (!bInnerFieldsUpdated) updateSizeFields(this.szUnits);
    }

    //region timeLabels and eventLabels methods
    public void addTimeLabels(Date[] timearray, Typeface fontTypeface, int fontColor, float fontSize, String timeLabel, String timeFormat, boolean placement) {
        for (Date time : timearray) {
            timeLabels.add(new labTime(time, fontSize, fontColor, fontTypeface, timeLabel, timeFormat, placement));
        }
    }

    public void addTimeLabel(Date time, Typeface fontTypeface, int fontColor, float fontSize, String timeLabel, String timeFormat, boolean placement) {
        timeLabels.add(new labTime(time, fontSize, fontColor, fontTypeface, timeLabel, timeFormat, placement));
    }

    private void editTimeLabel(String keyLabel, labTime newLabel) {
        //TODO implement timeLabels HashMap for edition;
    }

    public void clearTimeLabels() {
        timeLabels.clear();
    }

    public void addLabel(String eventLabel, float fontSize, int fontColor, Typeface fontTypeface, float szPad) {
        eventLabels.add(new labEvent(eventLabel, fontSize, fontColor, fontTypeface, szPad));
    }

    public void clearEventLabels() {
        eventLabels.clear();
    }
    //endregion

    private void updateSizeFields(int szUnits) {
        DisplayMetrics dm = Resources.getSystem().getDisplayMetrics();
        pxRad = (int) TypedValue.applyDimension(szUnits, Math.min(szClock.x, szClock.y), dm);
        pxClock = new Point(pxRad, pxRad);
        pxRad /= 2;
        pxCenter = new Point(pxRad, pxRad);
        pxPad = (int) TypedValue.applyDimension(szUnits, szPad, dm);
        pxFrame = (int) TypedValue.applyDimension(szUnits, szFrame, dm);
        pxPtrWidth = (int) TypedValue.applyDimension(szUnits, pxPtrWidth, dm);
        pxTimeLabPad = (int) TypedValue.applyDimension(szUnits, pxTimeLabPad, dm);
        pxMainTime = (int) TypedValue.applyDimension(szUnits, pxMainTime, dm);
        patFrame = renderDashPathEffect(pxRad);
        bInnerFieldsUpdated = true;
    }

    private DashPathEffect renderDashPathEffect(float rad) {
        float f = (float) (Math.PI * rad * this.resTimeMins / 720);
        return new DashPathEffect(new float[]{f * this.frTimeMins, f * (1 - this.frTimeMins)}, 0);
    }

    public Bitmap draw() {

        if (!bInnerFieldsUpdated) updateSizeFields(szUnits);

        Bitmap bitmap = Bitmap.createBitmap(pxClock.x, pxClock.y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        //RectF area = ;
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        //Draw background Bitmap
        if (backgroundBitmap != null) {
            p.setColor(Color.WHITE);
            canvas.drawBitmap(backgroundBitmap, 0, 0, p);
        }

        //Draw Clock background Frame
        p.setColor(cFrameOff);
        p.setStrokeWidth(szFrame);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(pxCenter.x, pxCenter.y, pxRad, p);

        long lCurrTime = System.currentTimeMillis();
        String sCurrTime = new SimpleDateFormat("HH:mm").format(new Date());
        long newDayOffset = 43200000 + (newDayTimeMilis - 43200000) % 86400000;
        float gradTimeWithOffset = ((lCurrTime + newDayOffset) % 86400000 / 60000 / 4) % 360;

        switch (clockMode) {
            case CLOCKMODE_DAYUP:
                angleModeOffset = -90;
                break;
            case CLOCKMODE_DAYDOWN:
                angleModeOffset = 90;
                break;
            default:
                angleModeOffset = -gradTimeWithOffset;
        }

        //Elapsed Time
        p.setColor(cFrame);
        p.setPathEffect(patFrame);
        canvas.drawArc(
                new RectF(pxCenter.x - pxRad, pxCenter.y - pxRad, pxCenter.x + pxRad, pxCenter.y + pxRad),
                gradTimeWithOffset + angleModeOffset, gradTimeWithOffset,
                false, p);


        //Clock Pointer
        p.reset();
        p.setAntiAlias(true);
        float fp = pxRad * (1 - this.frPtrHeight / 2);
        p.setStrokeWidth(pxRad * this.frPtrHeight);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(cFrame);
        canvas.drawArc(
                new RectF(pxCenter.x - fp, pxCenter.y - fp, pxCenter.x + fp, pxCenter.y + fp),
                gradTimeWithOffset + angleModeOffset - 0.5f, 0.5f,
                false, p);

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        float line;
        Rect bounds = new Rect();

        //Draw timemarks
        setPaths();
        for (labTime times : timeLabels) {
            tp.setColor(times.color);
            tp.setTextSize(times.size);
            tp.setTypeface(times.typeface);
            tp.setTextAlign((times.alignRight) ? Paint.Align.RIGHT : Paint.Align.LEFT);
            canvas.drawTextOnPath(times.toString(), times.path, 0, 0, tp);
        }

        //Draw Current Time
        tp.setTypeface(tfMainTime);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(szMainTime);
        tp.setColor(cMainTime);
        tp.getTextBounds(sCurrTime, 0, sCurrTime.length(), bounds);
        line = pxCenter.y + bounds.height() / 2;  // -(tp.ascent()+tp.descent())/2;
        canvas.drawText(sCurrTime, pxCenter.x, line, tp);

        line += 22f;
        //Draw Labels
        for (labEvent event : eventLabels) {
            tp.setTypeface(event.typeface);
            tp.setColor(event.color);
            tp.setTextSize(event.size);
            tp.getTextBounds(event.label, 0, event.label.length(), bounds);
            line += 22f + bounds.height() / 2;// -(tp.ascent()+tp.descent())/2;
            canvas.drawText(event.label, pxCenter.x, line, tp);
        }

        return bitmap;

    }

    private void setPaths() {
        float r1a = szFrame + szTimeLabPad, r1, r2;
        final double Pi2Deg = Math.PI / 180;
        double angle, cos, sin;
        for (labTime z : timeLabels) {
            if (z.placement) {
                r2 = pxRad - r1a;
                r1 = r2 - textMarksMaxWidth[1];
            } else {
                r1 = pxRad + r1a;
                r2 = r1 + textMarksMaxWidth[0];
            }
            Path p = new Path();
            angle = (z.toGradAngle() + angleModeOffset) * Pi2Deg;
            cos = Math.cos(angle);
            sin = Math.sin(angle);
            if (cos < 0) {
                p.moveTo((float) (r2 * cos) + pxCenter.x, (float) (r2 * sin) + pxCenter.y);
                p.lineTo((float) (r1 * cos) + pxCenter.x, (float) (r1 * sin) + pxCenter.y);
                z.alignRight = true ^ z.placement;
            } else {
                p.moveTo((float) (r1 * cos) + pxCenter.x, (float) (r1 * sin) + pxCenter.y);
                p.lineTo((float) (r2 * cos) + pxCenter.x, (float) (r2 * sin) + pxCenter.y);
                z.alignRight = false ^ z.placement;
            }
            z.path = p;
        }

    }
}
