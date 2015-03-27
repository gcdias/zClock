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
import android.util.Log;
import android.util.TypedValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by GU on 03-11-2014.
 */
public class zClock {

    private final int CLOCK_MODE_DAY_UP=0;
    private final int CLOCK_MODE_DAY_DOWN=1;
    public int cFrameOff,// = 0x1000c3ff,
            cFrameOn,//=0xff00c3ff,
            cPointer;// = 0xff00c3ff;
    public boolean bClockElapsedTime = true;
    private float   dp = Resources.getSystem().getDisplayMetrics().density;
    //private int     resx =Resources.getSystem().getDisplayMetrics().widthPixels;
    //private int     resy=Resources.getSystem().getDisplayMetrics().heightPixels;
    private float pad = 3 * dp;
    private float[] textMarksMaxWidth = new float[]{22f * dp, 0};
    private PointF  szClock,//= new PointF(294*dp,294*dp),
                    centro;// = new PointF(szClock.x / 2f, szClock.y / 2f);
    private Point   pxClock;
    private int     raio;// = Math.min(szClock.x / 2f, szClock.y / 2f) - pad;
    private float   szFrame,// = 0.5f * dp,
                    szPointer,// = 0.17f * dp,
                    szTimeMins,//=10f,
                    resTimeMins,//=2f;
                    szPtrHeight=50;
    private PathEffect patFrame;

    private int     cTime;// = 0xc3ffffff;
    private float   szTime;// = 45f * dp;
    private Typeface typeTime;//=Typeface.create("sans-serif-light", Typeface.NORMAL);

    private int     clockMode;// = 0;
    private float   angle_offset;// = 90f;
    private float   newdaytime_angle;
    private float   szTimeLabPad=10f;
    private long    newDayTimeMilis;
    private Bitmap  backgroundBitmap=null;
    private float lineFeed = 22f;
    private List<timeLabel> timeMarks = new ArrayList();
    private List<dateLabel> Labels = new ArrayList();

    public zClock(int clockMode, long NewDayTime, PointF dpClock) {

        this.clockMode = clockMode;
        this.newDayTimeMilis = NewDayTime;
        this.szClock = dpClock;
        this.pxClock = new Point(
                applyDimension(TypedValue.COMPLEX_UNIT_DIP, szClock.x),
                applyDimension(TypedValue.COMPLEX_UNIT_DIP, szClock.y));
        this.newdaytime_angle = ((newDayTimeMilis / 60000f) % 1440) / 4;
        this.raio = Math.min(pxClock.x / 2, pxClock.y / 2);
        if (clockMode < 3) {
            this.pxClock.x = raio * 2;
            this.pxClock.y = raio * 2;
        }
        this.raio -= pad;
        this.centro = new PointF(this.pxClock.x / 2, this.pxClock.y / 2);
    }

    public static int applyDimension(int typedValyeUnit, float value) {
        return (int) TypedValue.applyDimension(typedValyeUnit, value, Resources.getSystem().getDisplayMetrics());
    }

    public void setClockFrame(float szFrame, float szPointer, float szPtrHeight, int resTimeMins, int szTimeMins, int cFrameOn, int cFrameOff, int cTime, float szTime, Typeface tfTime) {
        this.szFrame = szFrame;
        this.szPointer = szPointer;
        this.szPtrHeight = szPtrHeight / 100f;
        this.resTimeMins = resTimeMins;
        this.szTimeMins = szTimeMins / 100f;
        this.patFrame = renderDashPathEffect(this.raio);
        this.cTime = cTime;
        this.szTime = szTime;
        this.cFrameOn = cFrameOn;
        this.cFrameOff = cFrameOff;
        this.typeTime = tfTime;
    }

    private DashPathEffect renderDashPathEffect(float raio) {
        float f = (float) (Math.PI * raio * this.resTimeMins / 720);
        return new DashPathEffect(new float[]{f * this.szTimeMins, f * (1 - this.szTimeMins)}, 0);
    }

    public PointF getPxClock() {
        return new PointF(pxClock.x, pxClock.y);
    }

    public void changeNewDay(long NewDayTimeMilis) {
        this.newDayTimeMilis = NewDayTimeMilis;
        newdaytime_angle = ((newDayTimeMilis / 60000f) % 1440) / 4;
    }

    public void resetTimeMarks() {
        timeMarks.clear();
    }

    public void addMarks(Typeface typeface, int color, float size, String format, boolean inside, Date[] timearray) {
        for (Date t : timearray) {
            timeMarks.add(new timeLabel(t, size, color, typeface, format, inside));
        }
    }

    public void addLabel(String text, float size, int color, Typeface typeface, float pad) {
        Labels.add(new dateLabel(text, size, color, typeface, pad));
    }

    public void setBackgroundPicture(Bitmap b) {
        backgroundBitmap = b;
    }

    public Bitmap draw() {

        Bitmap bitmap = Bitmap.createBitmap(pxClock.x, pxClock.y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        //Draw background
        if (backgroundBitmap != null) {
            p.setColor(Color.WHITE);
            canvas.drawBitmap(backgroundBitmap, 0, 0, p);
        }

        //Draw Clock background Frame
        p.setColor(cFrameOff);
        p.setStrokeWidth(szFrame);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(centro.x, centro.y, raio, p);

        float time = getTimeMins();
        float time_angle = (time / 4) % 360;

        switch (clockMode) {
            case CLOCK_MODE_DAY_UP:
                angle_offset = -90;
                break;
            case CLOCK_MODE_DAY_DOWN:
                angle_offset = 90;
                break;
            default:
                angle_offset = -time_angle;
        }

        //Elapsed Time
        p.setColor(cFrameOn);
        p.setPathEffect(patFrame);
        Log.e("Clock.draw", String.format("ta:%f ndta:%f", time_angle, newdaytime_angle));
        float angStart = bClockElapsedTime ? newdaytime_angle : time_angle;
        float angLenght = bClockElapsedTime ? time_angle - newdaytime_angle : newdaytime_angle - time_angle;
        if (angLenght < 0) angLenght += 360;
        canvas.drawArc(new RectF(centro.x - raio, centro.y - raio, centro.x + raio, centro.y + raio),
                angStart + angle_offset, angLenght, false, p);

        //Clock Pointer
        p.reset();
        p.setAntiAlias(true);
        float fp = raio * (1 - this.szPtrHeight / 2);
        p.setStrokeWidth(raio * this.szPtrHeight);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(cFrameOn);
        canvas.drawArc(new RectF(centro.x - fp, centro.y - fp, centro.x + fp, centro.y + fp), time_angle + angle_offset - 0.5f, 0.5f, false, p);

        Paint tp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        float line;
        Rect bounds = new Rect();
        String s;

        //Draw timemarks
        timeMarkPaths();
        for (timeLabel z : timeMarks) {
            lineFeed = applyDimension(TypedValue.COMPLEX_UNIT_PX, z.size);
            tp.setColor(z.color);
            tp.setTextSize(lineFeed);
            tp.setTypeface(z.type);
            tp.setTextAlign((z.alignRight) ? Paint.Align.RIGHT : Paint.Align.LEFT);
            canvas.drawTextOnPath(z.toString(), z.path, 0, 0, tp);
        }


        //Draw Current Time
        tp.setTypeface(typeTime);
        tp.setTextAlign(Paint.Align.CENTER);
        tp.setTextSize(applyDimension(TypedValue.COMPLEX_UNIT_PX, szTime));
        tp.setColor(cTime);
        s = new SimpleDateFormat("HH:mm").format(new Date());
        tp.getTextBounds(s, 0, s.length(), bounds);
        line = centro.y + bounds.height() / 2;  // -(tp.ascent()+tp.descent())/2;
        canvas.drawText(s, centro.x, line, tp);

        //Draw Labels
        for (dateLabel t : Labels) {
            lineFeed = applyDimension(TypedValue.COMPLEX_UNIT_PX, t.size);
            tp.setTypeface(t.type);
            tp.setColor(t.color);
            tp.setTextSize(lineFeed);
            tp.getTextBounds(t.label, 0, t.label.length(), bounds);
            line += 1.5f * bounds.height();// -(tp.ascent()+tp.descent())/2;
            canvas.drawText(t.label, centro.x, line, tp);
        }

        return bitmap;

    }

    public Bitmap renderBackground(Bitmap bitmap, int bkgColor, float corners) {
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bkgColor);
        canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), corners, corners, p);
        return bitmap;
    }

    public float getTimeMins() {
        return (System.currentTimeMillis() / 60000f) % 1440;
    }

    public void updateTimeMarks() {
        textMarksMaxWidth = getMaxTextWidth();
        raio = (int) (Math.min(pxClock.x / 2, pxClock.y / 2) - textMarksMaxWidth[0] - szFrame - szTimeLabPad);
        this.patFrame = renderDashPathEffect(this.raio);
        timeMarkPaths();
    }

    private float[] getMaxTextWidth() {
        float res_out = 0f, res_in = 0f;
        Paint tp = new Paint();
        for (timeLabel z : timeMarks) {
            lineFeed = applyDimension(TypedValue.COMPLEX_UNIT_DIP, z.size);
            tp.setTypeface(z.type);
            tp.setTextSize(lineFeed);
            if (z.insideFrame)
                res_in = Math.max(res_in, tp.measureText(z.toString()));
            else
                res_out = Math.max(res_out, tp.measureText(z.toString()));
        }
        return new float[]{res_out, res_in};
    }

    private void timeMarkPaths() {

        float r1a = szFrame + szTimeLabPad, r1, r2;
        final double Pi2Deg = Math.PI / 180;
        double angle, cos, sin;
        for (timeLabel z : timeMarks) {
            if (z.insideFrame) {
                r2 = raio - r1a;
                r1 = r2 - textMarksMaxWidth[1];
            } else {
                r1 = raio + r1a;
                r2 = r1 + textMarksMaxWidth[0];
            }
            Path p = new Path();
            angle = (z.toRadianAngle() + angle_offset) * Pi2Deg;
            cos = Math.cos(angle);
            sin = Math.sin(angle);
            if (cos < 0) {
                p.moveTo((float) (r2 * cos) + centro.x, (float) (r2 * sin) + centro.y);
                p.lineTo((float) (r1 * cos) + centro.x, (float) (r1 * sin) + centro.y);
                z.alignRight = true ^ z.insideFrame;
            } else {
                p.moveTo((float) (r1 * cos) + centro.x, (float) (r1 * sin) + centro.y);
                p.lineTo((float) (r2 * cos) + centro.x, (float) (r2 * sin) + centro.y);
                z.alignRight = false ^ z.insideFrame;
            }
            z.path = p;
        }
    }

    class timeLabel {
        private Date date;
        private float size;
        private int color;
        private Typeface type;
        private boolean alignRight;
        private Path path;
        private String format;
        private String label;
        private boolean insideFrame;

        timeLabel(Date date, float size, int color, Typeface t, String format, boolean insideFrame) {
            this.date = date;
            this.size = size;
            this.color = color;
            this.type = t;
            this.format = format;
            this.insideFrame = insideFrame;
        }

        @Override
        public String toString() {
            SimpleDateFormat s = new SimpleDateFormat(this.format);
            return s.format(this.date);
        }

        public float toRadianAngle() {
            float f = (this.date.getTime() / 60000f) % 1440;
            return f * 360f / 1440f;
        }

        public timeLabel addHours(int hours) {
            this.date = new Date(this.date.getTime() + 3600000 * hours);
            return this;
        }

        public timeLabel addMinutes(int minutes) {
            this.date = new Date(this.date.getTime() + 60000 * minutes);
            return this;
        }
    }

    class dateLabel {
        private String label;
        private float size;
        private int color;
        private Typeface type;
        private float pad;

        dateLabel(String text, float size, int color, Typeface t, float pad) {
            this.label = text;
            this.size = size;
            this.color = color;
            this.type = t;
            this.pad = pad;
        }
    }
}
