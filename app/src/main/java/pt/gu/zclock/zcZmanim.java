package pt.gu.zclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Toast;

import net.sourceforge.zmanim.ComplexZmanimCalendar;
import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;
import net.sourceforge.zmanim.util.GeoLocation;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * Created by GU on 30-12-2014.
 */
public class zcZmanim {

    final int HASHEM_72 = 0;
    final int HASHEM_72_SOF = 1;
    final int HASHEM_42 = 2;
    final String[] romanNumb = {"I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    final String[] hebMonthsTrans = {"Nissan", "Iyar", "Sivan", "Tamuz", "Av", "Elul", "Tishri", "Mar Cheshvan",
            "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I"};

    final String[] parshiotTransl = {
            "Bereshit", "Noach", "Lech-Lecha", "Vayera", "Chaye-Sarah", "Toldot",
            "Vayetzei", "Vayishlach", "Vayeshev", "Miketz", "Vayigash", "Vayechi",
            "Shemot", "Vaera", "Bo", "Beshalach", "Yitro", "Mishpatim", "Terumah", "Tetzaveh", "Ki Tisa", "Vayakchel", "Pekude",
            "Vayikra", "Tzav", "Shmini", "Tazria", "Metzora", "Achrei Mos", "Kedoshim", "Emor", "Behar", "Bechukotai",
            "Bamidbar", "Naso", "Beha'alotcha", "Shelach", "Korach", "Chukat", "Balak", "Pinchas", "Matot", "Masei",
            "Devarim", "Vaetchanan", "Ekev", "Re'eh", "Shoftim", "Ki Tetze", "Ki Tavo", "Nitzavim", "Vayelech", "Ha'Azinu",
            "Vayakchel Pekudei", "Tazria Metzora", "Achre Mot Kedoshim", "Behar Bechukotai", "Chukat Balak",
            "Matot Masei", "Nitzavim Vayelech"};

    private int mAppWidgetId;
    private Context mContext;
    private SharedPreferences mSharedPreferences;
    private Calendar sysCalendar;
    private ComplexZmanimCalendar zCalendar;
    private JewishCalendar jCalendar;
    private HebrewDateFormatter hebFormat;
    private Date mSunrise;
    private Date mSunset;
    private int mMode = 0;
    private gpsInfo mGpsInfo;
    private clockLayout mClockLayout = new clockLayout();
    private timeLabels labHours;
    private timeLabels labZmanim;
    private timeLabels labSunEvents;

    private long _ndtShift;


    public zcZmanim(Context context, int appWidgetId) {

        //initilize vars
        this.mContext = context;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        this.hebFormat = new HebrewDateFormatter();
        this.hebFormat.setTransliteratedMonthList(hebMonthsTrans);
        this.hebFormat.setTransliteratedParshiosList(parshiotTransl);
        this.mGpsInfo = new gpsInfo();

        setWidgetId(appWidgetId);
    }

    protected void setWidgetId(int appWidgetId) {
        //calendar
        this.sysCalendar = Calendar.getInstance();
        this.zCalendar = new ComplexZmanimCalendar(mGpsInfo.geoLocation());
        this.mAppWidgetId = appWidgetId;
        this.mMode = getIntPref("clockMode");
        setCurrentSunTimes();
        sysCalendar.add(Calendar.MILLISECOND, (int) _ndtShift);
        this.jCalendar = new JewishCalendar(sysCalendar);
        this.mClockLayout.loadPreferences();
    }

    protected Bitmap getWidgetBitmap() {

        Bitmap bitmap = this.createWidgetBitmap((mMode < 3));
        final Typeface tfStam = Typeface.createFromAsset(mContext.getAssets(), "fonts/sefstm.ttf");

        if (mMode < 3) {

            if (getBoolPref("show72Hashem")) {
                int index = (int) (sysCalendar.getTime().getTime() / 60000 / getIntPref("nShemot")) % 72;
                String name = decodeResourceArray(R.array.short_shemot, HASHEM_72).split("\\r?\\n")[index];
                String verses = decodeResourceArray(R.array.long_shemot, 0);
                renderBackground(bitmap, 0x80000000, 3f);
                Rect bounds = new Rect();
                renderTextLine(bitmap,
                        new labelFormat(tfStam, 0x08ffffff, 0f),
                        name,
                        new PointF(bitmap.getWidth() / 2, bitmap.getHeight() * 0.4f),
                        bounds);
                for (String verse : verses.split("\\r?\\n")) {
                    renderTextLine(bitmap,
                            new labelFormat(tfStam, 0x08ffffff, 0f),
                            verse,
                            new PointF(bitmap.getWidth() / 2, bounds.bottom * 1.1f),
                            bounds);
                }
            }
            renderClock(bitmap, mClockLayout);
            
        }
        return bitmap;
    }


    private void setCurrentSunTimes() {

        switch (mMode) {
            case 1:
                mSunset = zCalendar.getTzais60();
                mSunrise = zCalendar.getAlos60();
                break;
            case 2:
                mSunset = zCalendar.getTzais72();
                mSunrise = zCalendar.getAlos72();
                break;
            case 3:
                mSunset = zCalendar.getTzais90();
                mSunrise = zCalendar.getAlos90();
                break;
            case 4:
                mSunset = zCalendar.getTzais120();
                mSunrise = zCalendar.getAlos120();
                break;
            case 5:
                mSunset = zCalendar.getTzais16Point1Degrees();
                mSunset = zCalendar.getAlos16Point1Degrees();
                break;
            case 6:
                mSunset = zCalendar.getTzais18Degrees();
                mSunset = zCalendar.getAlos18Degrees();
                break;
            case 7:
                mSunset = zCalendar.getTzais19Point8Degrees();
                mSunset = zCalendar.getAlos19Point8Degrees();
                break;
            case 8:
                mSunset = zCalendar.getTzais26Degrees();
                mSunset = zCalendar.getAlos26Degrees();
                break;
            default:
                mSunset = zCalendar.getSunset();
                mSunset = zCalendar.getSunrise();
                break;
        }
        _ndtShift = 86400000 - mSunset.getTime() % 86400000;
    }

    private int convertMilisToMinutes(long milis) {
        return (int) ((milis) / 60000 % 1440);
    }

    private void setZmanimMarks() {

        final Typeface tfCondN = Typeface.create(mContext.getString(R.string.font_condensed), Typeface.NORMAL);
        final Typeface tfRegularB = Typeface.create(mContext.getString(R.string.font_regular), Typeface.BOLD);

        boolean langHeb = getBoolPref("bLangHebrew");

        labHours.clear();

        //Clock Hours 0-23h
        if (getBoolPref("showTimeMarks")) {
            labHours.format = new labelFormat(tfCondN, getColorPref("cTimemarks"), getDimensPref("szTimemarks"));
            SimpleDateFormat df = new SimpleDateFormat(getStringPref("tsTimemarks"));
            for (int i = 0; i < 24; i++) {
                Date d = new Date(3600000 * i);
                labHours.add(d, df.format(d));
            }
        }

        if (getBoolPref("showZmanim")) {

            labSunEvents.clear();
            labSunEvents.format = new labelFormat(tfRegularB,
                    getColorPref("cZmanim_sun"),
                    getDimensPref("szZmanim_sun"));
            long chatzotMilis = zCalendar.getChatzos().getTime();
            labSunEvents.add(new Date(chatzotMilis), langHeb ? "חצות" : "Chatzot");
            labSunEvents.add(new Date(chatzotMilis + 43200000), langHeb ? "חצות" : "Chatzot");
            if (getIntPref("zmanimMode") == 0) {
                labSunEvents.add(mSunrise, langHeb ? "הנץ" : "Hanetz");
                labSunEvents.add(mSunset, langHeb ? "שקיעת" : "Shekiat");
                labSunEvents.add(zCalendar.getAlos72Zmanis(), langHeb ? "עלות ע׳ב ן׳" : "Alot 72\"z");
                labSunEvents.add(zCalendar.getTzais72Zmanis(), langHeb ? "צאת ע׳ב ן׳" : "Tzet 72\"z");
            } else {
                labSunEvents.add(zCalendar.getSunrise(), langHeb ? "הנץ" : "Hanetz");
                labSunEvents.add(zCalendar.getSunset(), langHeb ? "שקיעת" : "Shekiat");
                labSunEvents.add(mSunrise, langHeb ? "עלות" : "Alot");
                labSunEvents.add(mSunset, langHeb ? "צאת" : "Tzet");
            }
        }
        /*

        Clock.changeNewDay(sunsr[0].getTime());
        long chatzot = zCalendar.getChatzos().getTime();

        Date d1 = (zMode == 0) ? zCalendar.getTzais() : zCalendar.getSunset();
        Date d2 = (zMode == 0) ? zCalendar.getAlosHashachar() : zCalendar.getSunrise();

        Clock.addMarks(tfRegularB,
                getColorPref("cZmanim_sun"),
                getDimensPref("szZmanim_sun"),
                getStringPref("tsZmanim_sun"),
                getBoolPref("iZmanim_sun"),
                new Date[]{
                        sunsr[0],
                        sunsr[1],
                        d1,
                        d2,
                        new Date(chatzot),
                        new Date(chatzot + 43200000)});

        if (getBoolPref("showZmanim")) {

            Clock.addMarks(tfCondN,
                    getColorPref("cZmanim_main"),
                    getDimensPref("szZmanim_main"),
                    getStringPref("tsZmanim_main"),
                    getBoolPref("iZmanim_main"),
                    new Date[]{
                            zCalendar.getSunriseOffsetByDegrees(AstronomicalCalendar.ASTRONOMICAL_ZENITH - 11),
                            zCalendar.getSofZmanShma(sunsr[1], sunsr[0]),
                            zCalendar.getSofZmanTfila(sunsr[1], sunsr[0]),
                            zCalendar.getMinchaKetana(sunsr[1], sunsr[0]),
                            zCalendar.getMinchaGedola(sunsr[1], sunsr[0]),
                            zCalendar.getPlagHamincha(sunsr[1], sunsr[0])
                    });

        }

        Clock.updateTimeMarks();
        */
    }

    //region parsha methods

    private String getCurrentPasuk(boolean dayTimeOnly, String pasukReference) {
        long current = System.currentTimeMillis() + _ndtShift;
        long cSSet = mSunset.getTime();
        long cSRise = mSunrise.getTime();
        if (dayTimeOnly && (current > cSSet || current < cSRise)) return null;
        int dm = dayTimeOnly ? convertMilisToMinutes(cSSet - cSRise) : 1440;
        int m = (int) ((current - (dayTimeOnly ? cSRise : 0)) / 60000 % dm);
        int l = 0, index = 0;
        int d = jCalendar.getDayOfWeek() - 1;

        String pasuk = null;
        try {
            int i = getParshaHashavuaIndex(sysCalendar);
            String[] source = mContext.getResources().getStringArray(R.array.torah)[i].split("#");
            String[] parsha = new String(Base64.decode(source[1], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            String[] yom = new String(Base64.decode(source[0], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            int v = 0;
            for (int n = 0; n < d; n++) {
                v += Integer.valueOf(yom[n]);
            }
            index = 1 + v + (int) (m * Integer.valueOf(yom[d]) / 1440);
            pasuk = parsha[index];
            int iref = pasuk.indexOf(" ");
            pasukReference = (iref > 0) ? String.format("%s %s", getParshaHashavua(sysCalendar, true, true), pasuk.substring(0, iref)) : "error";
            pasuk = (iref > 0) ? pasuk.substring(iref + 1) : null;
        } catch (UnsupportedEncodingException ignored) {
            Log.e("Encoding Error", "");
            return null;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Log.e("Array Out of Range", "");
            return null;
        } catch (StringIndexOutOfBoundsException ignored) {
            Log.e("String Index out of Bounds", "");
        }
        return pasuk;
    }

    private Bitmap getCurrentPasuk(Context context, PointF size) {

        Bitmap bitmap = Bitmap.createBitmap((int) size.x, (int) size.y, Bitmap.Config.ARGB_8888);
        renderBackground(bitmap, 0x20000000, 13);

        final Typeface tfStam = Typeface.createFromAsset(context.getAssets(), "fonts/sefstm.ttf");
        int l = 0, index = 0;
        int d = jCalendar.getDayOfWeek() - 1;
        int dm = (int) ((zCalendar.getSunset().getTime() - zCalendar.getSunrise().getTime()) / 60000);
        int m = (int) (System.currentTimeMillis() / 60000 % 1440);
        String pasuk, ref;
        try {
            int i = getParshaHashavuaIndex(sysCalendar);
            String[] source = context.getResources().getStringArray(R.array.torah)[i].split("#");
            String[] parsha = new String(Base64.decode(source[1], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            String[] yom = new String(Base64.decode(source[0], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            int v = 0;
            for (int n = 0; n < d; n++) {
                v += Integer.valueOf(yom[n]);
            }
            index = 1 + v + (int) (m * Integer.valueOf(yom[d]) / 1440);
            //Log.e("Parashat", String.format("parsha %d day %d line %d/%d",i,d+1,v,index));
            pasuk = parsha[index];
            int iref = pasuk.indexOf(" ");
            ref = (iref > 0) ? String.format("%s %s", getParshaHashavua(sysCalendar, true, true), pasuk.substring(0, iref)) : "error";
            pasuk = (iref > 0) ? pasuk.substring(iref + 1) : String.format("L:%d SUM:%d I:%d", l, v, index);
        } catch (UnsupportedEncodingException ignored) {
            Log.e("Encoding Error", "");
            return bitmap;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Toast.makeText(context, "Parashat Hashavua resource is missing", Toast.LENGTH_LONG).show();
            return bitmap;
        } catch (StringIndexOutOfBoundsException ignored) {
            Log.e("String Index out of Bounds", "");
            return bitmap;
        }

        renderTextBlock(bitmap, new labelFormat(tfStam, 0x80ffffff, 32f), ref, bitmap.getHeight() * 0.9f - 26f, false);

        renderTextBlock(bitmap, new labelFormat(tfStam, 0xa0ffffff, 50f), pasuk, 0, false);

        return bitmap;
    }

    private String getParshaHashavua(Calendar c, boolean inHebrew, boolean parshaNameOnly) {
        int day = c.get(Calendar.DAY_OF_WEEK);
        String result = (parshaNameOnly) ? "" : (inHebrew) ? ((day % 7 == 0) ? "שבת " : "פרשת השבוע ") : ((day % 7 == 0) ? "Shabbat " : "Parashat Hashavua ");
        c.add(Calendar.DATE, 7 - day);
        hebFormat.setHebrewFormat(inHebrew);
        return result + hebFormat.formatParsha(new JewishCalendar(c.getTime()));
    }

    private int getParshaHashavuaIndex(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        int day = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DATE, 7 - day);
        return new JewishCalendar(c).getParshaIndex();
    }

    private String renderHashemName(int HashemName) {

        /*
        int index;
        if (type < 2) {
                index = (int) (sysCalendar.getTime().getTime() / 60000 / getIntPref("nShemot")) % 72;
                String name = decodeResourceArray(R.array.short_shemot, HashemName).split("\\r?\\n")[index];
                String verses = decodeResourceArray(R.array.long_shemot, 0);
                renderBackground(bitmap, 0x80000000, 3f);
                Rect bounds = new Rect();
                renderTextLine(bitmap,
                        new labelFormat(tfStam, 0x08ffffff, 0f),
                        name,
                        new PointF(bitmap.getWidth() / 2, bitmap.getHeight() * 0.4f),
                        bounds);
                for (String verse : verses.split("\\r?\\n")) {
                    renderTextLine(bitmap,
                            new labelFormat(tfStam,0x08ffffff,0f),
                            verse,
                            new PointF(bitmap.getWidth() / 2, bounds.bottom*1.1f),
                            bounds);
                }

            }

            index = (int) (sysCalendar.getTime().getTime() / 60000 / getIntPref("nShemot")) % 72;
            String name = decodeResourceArray(R.array.short_shemot, type).split("\\r?\\n")[index];
            try {
                s1 = new String(
                        Base64.decode(
                                mContext.getResources().getStringArray(R.array.short_shemot)[type], Base64.DEFAULT), "UTF-8")
                        .split("\\r?\\n")[index];
                if (wdgtSize.x > 2 * wdgtSize.y) {
                    s2 = new String[]{String.valueOf(index)};
                    f = 10f;
                } else {
                    s2 = new String(
                            Base64.decode(
                                    context.getResources().getStringArray(R.array.long_shemot)[0], Base64.DEFAULT), "UTF-8")
                            .split("\\r?\\n");
                    f = 0f;
                }
            } catch (UnsupportedEncodingException ignored) {
            }

        } else {
            index = jewishCalendar.getDayOfWeek();
            try {
                s1 = new String(
                        Base64.decode(
                                context.getResources().getStringArray(R.array.short_ab)[index - 1], Base64.DEFAULT), "UTF-8");
                s2 = new String[]{new String(
                        Base64.decode(
                                context.getResources().getStringArray(R.array.long_ab)[index - 1], Base64.DEFAULT), "UTF-8")};
            } catch (UnsupportedEncodingException ignored) {
            }
            f = 0;
        }

        return renderText(
                size,
                Typeface.createFromAsset(context.getAssets(), "fonts/sefstm.ttf"),
                s1, s2,
                colorForeground, 0, colorForeground, f,
                glowSteps, colorBackground, 13);
                */
        return null;
    }

    private String decodeResourceArray(int ResId, int index) {
        String result = "";
        try {
            result = new String(
                    Base64.decode(
                            mContext.getResources().getStringArray(ResId)[index], Base64.DEFAULT), "UTF-8");
        } catch (UnsupportedEncodingException ignored) {
        }
        return result;
    }

    //endregion

    //region Draw methods

    public Bitmap createWidgetBitmap(boolean square) {

        int width = applyDim(TypedValue.COMPLEX_UNIT_DIP, getSizePref("widgetWidth"));
        int height = applyDim(TypedValue.COMPLEX_UNIT_DIP, getSizePref("widgetHeight"));

        if (square) {
            width = Math.min(width, height);
            height = width;
        }

        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
    }

    public void renderBackground(Bitmap bitmap, int bkgColor, float corners) {
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bkgColor);
        canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), corners, corners, p);
    }

    public void renderTextBlock(Bitmap bitmap, labelFormat format, String text, float dy, boolean glowEffect) {

        Canvas canvas = new Canvas(bitmap);

        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(format.color);
        p.setTypeface(format.typeface);
        p.setTextSize(format.size);
        canvas.translate(-12, 12 + dy);
        canvas.save();
        if (glowEffect) p.setMaskFilter(new BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL));
        StaticLayout sl = new StaticLayout("" + text, p, (int) (bitmap.getWidth() * 0.95f), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        sl.draw(canvas);
        if (glowEffect) {
            p.setMaskFilter(null);
            sl = new StaticLayout("" + text, p, (int) (bitmap.getWidth() * 0.95f), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            sl.draw(canvas);
        }
        canvas.restore();
    }

    public void renderTextLine(Bitmap bitmap, labelFormat format, String text, PointF position, Rect bounds) {

        Canvas canvas = new Canvas(bitmap);
        PointF size = new PointF(bitmap.getWidth(), bitmap.getHeight());

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(format.color);
        p.setTypeface(format.typeface);

        //autosize if format.size=0
        if (format.size == 0) {
            p.setTextSize(100);
            p.getTextBounds(text, 0, text.length(), bounds);
            p.setTextSize(Math.min(90 * size.x / bounds.width(), 50 * size.y / bounds.height()));
        } else p.setTextSize(format.size);

        canvas.drawText(text, position.x, position.y, p);
    }

    public void renderClock(Bitmap backgroundBitmap, clockLayout layout) {

        int pxRad = Math.min(backgroundBitmap.getHeight(), backgroundBitmap.getWidth());
        if (layout.clockCenter == null)
            layout.clockCenter = new PointF(backgroundBitmap.getWidth() / 2, backgroundBitmap.getHeight() / 2);
        float newdaytime_angle = convertMilisToMinutes(mSunset.getTime()) / 4;
        float time = convertMilisToMinutes(System.currentTimeMillis());
        float time_angle = (time / 4) % 360;
        float angle_offset = layout.noRotation ? -time_angle : layout.rotationDeg;

        Canvas canvas = new Canvas(backgroundBitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        //Draw Clock background Frame
        p.setColor(layout.colorFrameBackground);
        p.setStrokeWidth(layout.sizeFrame);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(layout.clockCenter.x, layout.clockCenter.y, pxRad, p);

        //Elapsed Time
        p.setColor(layout.colorFrameForeground);
        p.setPathEffect(renderDashPathEffect(pxRad, layout.resTimeMins, layout.szTimeMins));
        float angStart = layout.renderElapsedTime ? newdaytime_angle : time_angle;
        float angLenght = layout.renderElapsedTime ? time_angle - newdaytime_angle : newdaytime_angle - time_angle;
        if (angLenght < 0) angLenght += 360;
        canvas.drawArc(new RectF(layout.clockCenter.x - pxRad, layout.clockCenter.y - pxRad, layout.clockCenter.x + pxRad, layout.clockCenter.y + pxRad),
                angStart + angle_offset, angLenght, false, p);

        //Clock Pointer
        p.reset();
        p.setAntiAlias(true);
        float fp = pxRad * (1 - layout.szPtrHeight / 2);
        p.setStrokeWidth(pxRad * layout.szPtrHeight);
        p.setStyle(Paint.Style.STROKE);
        p.setColor(layout.colorFrameForeground);
        canvas.drawArc(new RectF(layout.clockCenter.x - fp, layout.clockCenter.y - fp, layout.clockCenter.x + fp, layout.clockCenter.y + fp), time_angle + angle_offset - 0.5f, 0.5f, false, p);
    }

    public int applyDim(int typedValueUnit, float value) {
        return (int) TypedValue.applyDimension(typedValueUnit, value, Resources.getSystem().getDisplayMetrics());
    }

    private DashPathEffect renderDashPathEffect(float rad, int resTimeMins, float szTimeMins) {
        szTimeMins = szTimeMins % 1;
        float f = (float) (Math.PI * rad * resTimeMins / 720);
        return new DashPathEffect(new float[]{f * szTimeMins, f * (1 - szTimeMins)}, 0);
    }

    //endregion

    //region getPreferences Methods
    private int getIntPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "integer", mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, mContext.getResources().getInteger(ResId));
    }

    private float getDimensPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "dimen", mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, 100) / 100f * mContext.getResources().getDimension(ResId);
    }

    private float getSizePref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "dimen", mContext.getPackageName());
        return mSharedPreferences.getFloat(key + mAppWidgetId, mContext.getResources().getDimension(ResId));
    }

    private String getStringPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "string", mContext.getPackageName());
        return mSharedPreferences.getString(key + mAppWidgetId, mContext.getResources().getString(ResId));
    }

    private boolean getBoolPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "bool", mContext.getPackageName());
        return mSharedPreferences.getBoolean(key + mAppWidgetId, mContext.getResources().getBoolean(ResId));
    }

    private int getColorPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key, "color", mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, mContext.getResources().getColor(ResId));
    }

    //endregion

    //region classes

    public class gpsInfo {
        double lat;
        double lng;
        double alt;
        boolean act;
        long timestamp;
        String provider;

        public gpsInfo() {
            act = false;
        }

        public void update() {
            LocationManager lm = (LocationManager) mContext.getSystemService(
                    Context.LOCATION_SERVICE);
            List<String> providers = lm.getProviders(true);

            Location l = null;

            for (int i = providers.size() - 1; i >= 0; i--) {
                l = lm.getLastKnownLocation(providers.get(i));
                if (l != null) {
                    provider = providers.get(i);
                    break;
                }
            }

            if (l != null) {
                lat = l.getLatitude();
                lng = l.getLongitude();
                alt = l.getAltitude();
                act = true;
                timestamp = System.currentTimeMillis();
            } else act = false;
        }

        public String getGeolocationName() {
            String _Location = null;
            if (act) {
                Geocoder geocoder = new Geocoder(mContext, Locale.getDefault());
                try {
                    List<Address> listAddresses = geocoder.getFromLocation(lat, lng, 1);
                    if (null != listAddresses && listAddresses.size() > 0) {
                        _Location = listAddresses.get(0).getAddressLine(0);
                    }
                } catch (IOException ignored) {
                }
            }
            return _Location;
        }

        public GeoLocation geoLocation() {
            return new GeoLocation(this.getGeolocationName(),
                    this.lat,
                    this.lng,
                    this.alt,
                    Calendar.getInstance().getTimeZone());
        }
    }

    public class labelFormat {
        private float size;
        private int color;
        private Typeface typeface;

        public labelFormat(Typeface typeface, int color, float size) {
            this.typeface = typeface;
            this.color = color;
            this.size = size;
        }
    }

    public class timeLabels {
        private labelFormat format;
        private HashMap<Date, String> timeLabel;

        public timeLabels(labelFormat format) {
            this.format = format;
        }

        public void add(Date date, String label) {
            timeLabel.put(date, label);
        }

        public void remove(Date date) {
            timeLabel.remove(date);
        }

        public void clear() {
            timeLabel.clear();
        }

    }

    public class clockLayout {
        int colorFrameBackground;
        int colorFrameForeground;
        PointF clockCenter;
        float sizeFrame;
        float szPtrHeight;
        float szPrtWidth;
        int resTimeMins;
        float szTimeMins;
        float rotationDeg;
        boolean noRotation;
        boolean renderElapsedTime;

        public clockLayout() {
        }

        public void loadPreferences() {
            this.sizeFrame = getDimensPref("wClockFrame");
            this.szPrtWidth = getDimensPref("wClockPointer");
            this.szPtrHeight = getIntPref("szPtrHeight") / 100f;
            this.resTimeMins = getIntPref("resTimeMins");
            this.szTimeMins = getIntPref("szTimeMins") / 100f;
            this.colorFrameForeground = getColorPref("cClockFrameOn");
            this.colorFrameBackground = getColorPref("cClockFrameOff");
            this.renderElapsedTime = getBoolPref("bClockElapsedTime");
            int i = getIntPref("clockMode");
            this.noRotation = (i == 2);
            this.rotationDeg = (i == 1) ? 90f : -90f;
        }
    }
    //endregion
}
