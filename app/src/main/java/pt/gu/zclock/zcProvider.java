package pt.gu.zclock;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;
import android.util.Log;
import android.util.TypedValue;
import android.widget.RemoteViews;
import android.widget.Toast;

import net.sourceforge.zmanim.AstronomicalCalendar;
import net.sourceforge.zmanim.ComplexZmanimCalendar;
import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;
import net.sourceforge.zmanim.util.GeoLocation;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import pt.gu.zclock.zcService.gpsInfo;


/**
 * Implementation of App Widget functionality.
 * App Widget Configuration implemented in {@link zcPreferences}
 */
public class zcProvider extends AppWidgetProvider {

    static final int    HASHEM_72       =0;
    static final int    HASHEM_72_SOF   =1;
    static final int    HASHEM_42       =2;
    static final String[] romanNumb     ={"I","II","III","IV","V","VI","VII","VIII","IX","X"};
    static final String[] hebMonthsTransl = {"Nissan", "Iyar", "Sivan", "Tamuz", "Av", "Elul", "Tishri", "Mar Cheshvan",
            "Kislev", "Tevet", "Shevat", "Adar", "Adar II", "Adar I" };
    static final String[] parshiotTransl = {
            "Bereshit", "Noach", "Lech-Lecha", "Vayera", "Chaye-Sarah", "Toldot",
            "Vayetzei", "Vayishlach", "Vayeshev", "Miketz", "Vayigash", "Vayechi",
            "Shemot", "Vaera", "Bo", "Beshalach", "Yitro", "Mishpatim", "Terumah", "Tetzaveh", "Ki Tisa", "Vayakchel", "Pekude",
            "Vayikra", "Tzav", "Shemini", "Tazria", "Metzora", "Achre Mot", "Kedoshim", "Emor", "Behar", "Bechukotai",
            "Bamidbar", "Naso", "Behaalotcha", "Shelach", "Korach", "Chukat", "Balak", "Pinchas", "Matot", "Masei",
            "Devarim", "Vaetchanan", "Ekev", "Reeh", "Shoftim", "Ki Tetze", "Ki Tavo", "Nitzavim", "Vayelech", "HaAzinu",
            "Vayakchel Pekude", "Tazria Metzora", "Achre Mot Kedoshim", "Behar Bechukotai", "Chukat Balak",
            "Matot Masei", "Nitzavim Vayelech"};

    static final GeoLocation HarHabait = new GeoLocation("Har Habait", 31.777972f, 35.235806f, 743, TimeZone.getTimeZone("Asia/Jerusalem"));


    public static zClock Clock;
    static RemoteViews remoteViews;

    private static Calendar sysCalendar = Calendar.getInstance();
    private static ComplexZmanimCalendar zmanimCalendar;
    private static JewishCalendar jewishCalendar = new JewishCalendar();
    private static HebrewDateFormatter hebrewFormat = new HebrewDateFormatter();
    private static Date alotHarHabait;

    private static boolean clockUpdated=false;

    static void updateWidgets(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            updateAppWidget(context, appWidgetManager, appWidgetId);

        }
    }

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        remoteViews = new RemoteViews(context.getPackageName(), R.layout.main);

        updateWidgetSize(context, appWidgetId);

        //Settings Intent
        Intent settingsIntent = new Intent(context, zcPreferences.class).setAction(zcPreferences.ACTION_PREFS);
        settingsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, appWidgetId, settingsIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setOnClickPendingIntent(R.id.imageView, pendingIntent);

        Log.e("updateAppWidget", String.format("id:%s ", appWidgetId));
        updateWidgetSize(context, appWidgetId);

        int cMode = getIntPref(context, "clockMode", appWidgetId);
        if (zmanimCalendar == null || !zcService.gps_info.act) updateLocation(context);
        long newday = getNewDayTime(context, appWidgetId);

        sysCalendar = Calendar.getInstance();
        boolean nday = (sysCalendar.getTime().after(new Date(newday)));
        sysCalendar.add(Calendar.MINUTE, (int) ((86400000 - newday % 86400000) / 60000));
        jewishCalendar.setDate(sysCalendar);

        boolean bkgDark = getBoolPref(context, "bWhiteOnBlack", appWidgetId);

        if (cMode < 3) {

            if (Clock == null || nday || !clockUpdated) setupClockPrefs(context, appWidgetId);

            if (getBoolPref(context, "show72Hashem", appWidgetId))
                Clock.setBackgroundPicture(
                        getHashemNames(context, Clock.getPxClock(), appWidgetId, HASHEM_72, bkgDark ? 0x08ffffff : 0x08000000, 0));

            remoteViews.setImageViewBitmap(R.id.imageView, Clock.draw());
        }

        if (cMode == 3) remoteViews.setImageViewBitmap(
                R.id.imageView, getHashemNames(
                        context, getWidgetSizePrefs(
                                context, appWidgetId, true),
                        appWidgetId, HASHEM_72, bkgDark ? 0xffffffff : 0xff000000, bkgDark ? 0x80000000 : 0x80ffffff));

        if (cMode == 4) remoteViews.setImageViewBitmap(
                R.id.imageView, getHashemNames(
                        context, getWidgetSizePrefs(
                                context, appWidgetId, true),
                        appWidgetId, HASHEM_42, bkgDark ? 0xffffffff : 0xff000000, bkgDark ? 0x80000000 : 0x80ffffff));

        if (cMode == 5)
            remoteViews.setImageViewBitmap(
                    R.id.imageView, renderPasuk(
                            context, getWidgetSizePrefs(
                                    context, appWidgetId, true),
                            appWidgetId, 0, bkgDark ? 0xffffffff : 0xff000000, bkgDark ? 0x80000000 : 0x80ffffff));

        if (cMode ==6)


        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);

    }

    static Calendar updateCalendar(Calendar c, long newday) {
        c.add(Calendar.MINUTE, (int) ((86400000 - newday % 86400000) / 60000));
        return c;
    }

    static Bitmap getHashemNames(Context context,PointF size,int appWidgetId,int type, int colorForeground, int colorBackground){

        PointF wdgtSize = getWidgetSizePrefs(context, appWidgetId, true);
        int index;
        int glowSteps = (colorBackground == 0) ? 0 : 3;
        float f = 0;
        String s1 = "", s2[] = null;
        if (type < 2) {
            index = (int) (sysCalendar.getTime().getTime() / 60000 / getIntPref(context, "nShemot", appWidgetId)) % 72;
            try {
                s1 = new String(
                        Base64.decode(
                                context.getResources().getStringArray(R.array.short_shemot)[type], Base64.DEFAULT), "UTF-8")
                        .split("\\r?\\n")[index];
                /*if (wdgtSize.x > 2 * wdgtSize.y) {
                    s2 = new String[]{String.valueOf(index)};
                    f = 10f;
                } else */
                {
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
    }

    static Bitmap renderPasuk(Context context, PointF size, int appWidgetId, int type, int fColor, int bColor) {

        boolean bkgDark = getBoolPref(context, "bWhiteOnBlack", appWidgetId);
        Bitmap bitmap = Bitmap.createBitmap((int) size.x, (int) size.y, Bitmap.Config.ARGB_8888);
        bitmap = renderBackground(bitmap, bkgDark ? 0x80000000 : 0x80ffffff, 13);

        final Typeface tfStam = Typeface.createFromAsset(context.getAssets(), "fonts/sefstm.ttf");

        String[] currentPasuk = getCurrentPasuk(context,1);

        renderTextBlock(bitmap, tfStam, currentPasuk[0], bkgDark ? 0xa0ffffff : 0xa0000000, 28f, bitmap.getHeight() * 0.92f - 26f);

        return renderTextBlock(bitmap, tfStam, currentPasuk[1], bkgDark ? 0xffffffff : 0xff000000, 42f, 0);

    }

    private static String[] getCurrentPasuk(Context context,int mode) {
        int l = 0, index;
        int d = jewishCalendar.getDayOfWeek() - 1;
        int dm = (int) ((zmanimCalendar.getSunset().getTime() - zmanimCalendar.getSunrise().getTime()) / 60000);
        int m = (int) (sysCalendar.getTime().getTime() / 60000) % 1440;
        String pasuk, ref;
        try {
            int i = getParshaHashavuaIndex(sysCalendar);
            Log.e("Parsha index",String.valueOf(i));
            String[] source = context.getResources().getStringArray(R.array.torah)[i].split("#");
            String[] yom = new String(Base64.decode(source[0], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            String[] parsha;
            if (i>52) {
                String[] p = source[1].split("\\r?\\n");
                String[] p1 = new String(Base64.decode(p[0], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
                String[] p2 = new String(Base64.decode(p[1], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
                parsha = new String[p1.length+p2.length];
                System.arraycopy(p1,0,parsha,0,p1.length);
                System.arraycopy(p2,0,parsha,p1.length,p2.length);
            } else {
                parsha = new String(Base64.decode(source[1], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            }

            int v = 0;
            for (int n = 0; n < d; n++) {
                v += Integer.valueOf(yom[n]);
            }
            index = 1 + v + (m * Integer.valueOf(yom[d]) / 1440);
            Log.e("Parashat", String.format("parsha %d day %d line %d/%d", i, d + 1, v, index));
            pasuk = parsha[index];
            int iref = pasuk.indexOf(" ");
            ref = (iref > 0) ? String.format("%s %s", getParshaHashavua(sysCalendar, true, true), pasuk.substring(0, iref)) : "error";
            pasuk = (iref > 0) ? pasuk.substring(iref + 1) : String.format("ERRO %d/%d", index, l);
            if (mode ==1) pasuk = toNiqqud(pasuk);
            if (mode >1) pasuk = toOtiot(pasuk);
        } catch (UnsupportedEncodingException ignored) {
            Log.e("Encoding Error", "");
            return null;
        } catch (ArrayIndexOutOfBoundsException ignored) {
            Toast.makeText(context, "Parashat Hashavua resource is missing", Toast.LENGTH_LONG).show();
            return null;
        } catch (StringIndexOutOfBoundsException ignored) {
            Log.e("Index out of Bounds", "");
            return null;
        }
        return new String[]{ref,pasuk};
    }

    static String getParshaHashavua(Calendar c, boolean inHebrew, boolean parshaNameOnly) {
        int day = c.get(Calendar.DAY_OF_WEEK);
        String result = (parshaNameOnly) ? "" : (inHebrew) ? ((day % 7 == 0) ? "שבת " : "פרשת השבוע ") : ((day % 7 == 0) ? "Shabbat " : "Parashat Hashavua ");
        c.add(Calendar.DATE, 7 - day);
        hebrewFormat.setHebrewFormat(inHebrew);
        return result + hebrewFormat.formatParsha(new JewishCalendar(c.getTime()));
    }

    static int getParshaHashavuaIndex(Calendar cal) {
        Calendar c = (Calendar) cal.clone();
        int day = c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DATE, 7 - day);
        return new JewishCalendar(c).getParshaIndex();
    }

    private static long getNewDayTime(Context context, int appWidgetId) {

        switch (getIntPref(context, "zmanimMode", appWidgetId)) {
            case 1:
                return zmanimCalendar.getTzais60().getTime();
            case 2:
                return zmanimCalendar.getTzais72().getTime();
            case 3:
                return zmanimCalendar.getTzais90().getTime();
            case 4:
                return zmanimCalendar.getTzais120().getTime();
            case 5:
                return zmanimCalendar.getTzais16Point1Degrees().getTime();
            case 6:
                return zmanimCalendar.getTzais18Degrees().getTime();
            case 7:
                return zmanimCalendar.getTzais19Point8Degrees().getTime();
            case 8:
                return zmanimCalendar.getTzais26Degrees().getTime();
            default:
                return zmanimCalendar.getSunset().getTime();
        }
    }

    //preferences
    static int getIntPref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "integer", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key + appWidgetId, context.getResources().getInteger(ResId));
    }

    static float getDimensPref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "dimen", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key + appWidgetId, 100) / 100f * context.getResources().getDimension(ResId);
    }
    
    //region methods updated to zcZmanim

    static float getSizePref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "dimen", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getFloat(key + appWidgetId, context.getResources().getDimension(ResId));
    }

    static String getStringPref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "string", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getString(key + appWidgetId, context.getResources().getString(ResId));
    }

    static boolean getBoolPref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "bool", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(key + appWidgetId, context.getResources().getBoolean(ResId));
    }

    static int getColorPref(Context context, String key, int appWidgetId) {
        int ResId = context.getResources().getIdentifier(key, "color", context.getPackageName());
        return PreferenceManager.getDefaultSharedPreferences(context).getInt(key + appWidgetId, context.getResources().getColor(ResId));
    }

    //draw methods
    //updated to zcZmanim
    static Bitmap renderText(PointF size,
                             Typeface typeface,
                             String title, String[] subtitle,
                             int title_color, float title_size,
                             int subtitle_color, float subtitle_size,
                             int glowSteps,
                             int bkgColor,
                             float corners) {

        Bitmap bitmap = Bitmap.createBitmap((int) size.x, (int) size.y, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        PointF centro = new PointF(size.x / 2, size.y / 2);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(bkgColor);
        canvas.drawRoundRect(new RectF(0, 0, size.x, size.y), corners, corners, p);
        float y_pos = centro.y;
        p.setTypeface(typeface);
        p.setTextAlign(Paint.Align.CENTER);

        //Draw title
        if (title != null) {
            Rect b = new Rect();
            if (title_size == 0) {
                p.setTextSize(100);
                p.getTextBounds(title, 0, title.length(), b);
                p.setTextSize(Math.min(90 * size.x / b.width(), 50 * size.y / b.height()));
            } else p.setTextSize(title_size);
            p.setColor(title_color);
            p.getTextBounds(title, 0, title.length(), b);
            y_pos = centro.y + b.centerY()/4;
            if (glowSteps > 0) {
                float blur_rad = Clock.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 22 / glowSteps);
                p.setAlpha(128);
                for (int i = 0; i < glowSteps; i++) {
                    canvas.drawText(title, centro.x, y_pos, p);
                    blur_rad = blur_rad / 2;
                }
                p.setMaskFilter(null);
            }
            p.setColor(title_color);
            canvas.drawText(title, centro.x, y_pos, p);
        }


        //Draw subtitle
        if (subtitle != null) {
            Rect b = new Rect();
            if (subtitle_size == 0) {
                p.setTextSize(100);
                p.getTextBounds(subtitle[0], 0, subtitle[0].length(), b);
                subtitle_size = Math.min(80 * size.x / b.width(), 50 * size.y / b.height() / subtitle.length);
            }
            p.setTextSize(subtitle_size);
            p.getTextBounds(subtitle[0], 0, subtitle[0].length(), b);
            y_pos = size.y*0.56f;
            for (String st : subtitle) {
                p.setColor(subtitle_color);
                canvas.drawText(st, centro.x, y_pos - 2.5f * (p.descent() + p.ascent()), p);
                y_pos += b.height();
            }
        }

        return bitmap;
    }

    //updated to zcZmanim
    static Bitmap renderBackground(Bitmap bitmap, int bkgColor, float corners) {
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bkgColor);
        canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), corners, corners, p);
        return bitmap;
    }

    //updated to zcZmanim
    static Bitmap renderTextBlock(Bitmap bitmap,
                                  Typeface typeface,
                                  String text,
                                  int color, float size, float yPos) {

        Canvas canvas = new Canvas(bitmap);
        float margin=0.9f;

        Log.e("Draw Parsha", text);
        float slmargin = bitmap.getWidth() * (1-margin);
        float slwidth = bitmap.getWidth() * margin;
        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(color);
        p.setTypeface(typeface);
        p.setTextSize(size);
        canvas.translate(slmargin/2, yPos);
        canvas.save();
        //p.setMaskFilter(new BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL));
        StaticLayout sl;
        do {
            sl = new StaticLayout("" + text, p, (int) slwidth, Layout.Alignment.ALIGN_NORMAL, margin, 0.0f, false);
            size -= 1;
            p.setTextSize(size);
        } while ((sl.getHeight() > bitmap.getHeight() * margin));

        sl.draw(canvas);
        //p.setMaskFilter(null);
        //sl = new StaticLayout(""+text,p,(int)(bitmap.getWidth()*0.95f), Layout.Alignment.ALIGN_NORMAL,1.0f,0.0f,false);
        //sl.draw(canvas);
        canvas.restore();
        return bitmap;
    }

    //updated to zcZmanim
    static Bitmap renderTextLine(Bitmap bitmap,
                                 Typeface typeface,
                                 String text, int color, float size, PointF position) {

        Canvas canvas = new Canvas(bitmap);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.LINEAR_TEXT_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(color);
        p.setTypeface(typeface);
        p.setTextSize(size);
        canvas.drawText(text, position.x, position.y, p);
        return bitmap;
    }

    static void updateWidgetSize(Context context, int appWidgetId) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Bundle newOptions = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
        int w, h, wCells, hCells;
        if (Resources.getSystem().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        } else {
            w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            wCells = (int) Math.ceil((w + 2) / 72);
            hCells = (int) Math.ceil((h + 2) / 72);
        } else {
            wCells = (int) Math.ceil((w + 30) / 70);
            hCells = (int) Math.ceil((h + 30) / 70);
        }

        SharedPreferences.Editor ed = sharedPreferences.edit();
        ed.putFloat("widgetWidth" + appWidgetId, (float) w);
        ed.putFloat("widgetHeight" + appWidgetId, (float) h);
        ed.putInt("widgetCellWidth" + appWidgetId, wCells);
        ed.putInt("widgetCellHeight" + appWidgetId, hCells);
        ed.commit();
    }

    static PointF getWidgetSizePrefs(Context context, int appWidgetId, boolean applyDimension) {
        float w = applyDimension ?
                Clock.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getSizePref(context, "widgetWidth", appWidgetId)) :
                getSizePref(context, "widgetWidth", appWidgetId);
        float h = applyDimension ?
                Clock.applyDimension(TypedValue.COMPLEX_UNIT_DIP, getSizePref(context, "widgetHeight", appWidgetId)) :
                getSizePref(context, "widgetWidth", appWidgetId);
        return new PointF(w, h);
    }

    static Point getWidgetNCells(Context context, int appWidgetId) {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        Bundle newOptions = AppWidgetManager.getInstance(context).getAppWidgetOptions(appWidgetId);
        int w, h;
        if (Resources.getSystem().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
            h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT);
        } else {
            w = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
            h = newOptions.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            return new Point((int) Math.ceil((w + 2) / 72), (int) Math.ceil((h + 2) / 72));
        } else return new Point((int) Math.ceil((w + 30) / 70), (int) Math.ceil((h + 30) / 70));
    }

    static void updateClock() {
        clockUpdated = false;
    }

    static void setupClockPrefs(Context context, int appWidgetId) {
        if (zmanimCalendar == null) updateLocation(context);

        final Typeface tfThin = Typeface.create(context.getString(R.string.font_thin), Typeface.NORMAL);
        final Typeface tfCond = Typeface.create(context.getString(R.string.font_condensed), Typeface.NORMAL);
        final Typeface tfSTAM = Typeface.createFromAsset(context.getAssets(), "fonts/sefstm.ttf");

        boolean bHeb = getBoolPref(context, "bLangHebrew", appWidgetId);

        Clock = new zClock(
                getIntPref(context, "clockMode", appWidgetId),
                getNewDayTime(context, appWidgetId),
                getWidgetSizePrefs(context, appWidgetId, false));

        Clock.setClockFrame(
                getDimensPref(context, "wClockFrame", appWidgetId),
                getDimensPref(context, "wClockPointer", appWidgetId),
                getIntPref(context, "szPtrHeight", appWidgetId),
                getIntPref(context, "resTimeMins", appWidgetId),
                getIntPref(context, "szTimeMins", appWidgetId),
                getColorPref(context, "cClockFrameOn", appWidgetId),
                getColorPref(context, "cClockFrameOff", appWidgetId),
                getColorPref(context, "cTime", appWidgetId),
                getDimensPref(context, "szTime", appWidgetId),
                tfThin);


        if (getBoolPref(context, "showHebDate", appWidgetId)) {
            hebrewFormat.setHebrewFormat(bHeb);
            hebrewFormat.setTransliteratedMonthList(hebMonthsTransl);
            hebrewFormat.setTransliteratedParshiosList(parshiotTransl);
            Clock.addLabel(hebrewFormat.format(jewishCalendar),
                    getDimensPref(context, "szDate", appWidgetId),
                    getColorPref(context, "cDate", appWidgetId),
                    bHeb ? tfSTAM : tfCond,
                    getDimensPref(context, "wClockMargin", appWidgetId));
            if (jewishCalendar.isChanukah()) {
                Clock.addLabel(
                        bHeb ? ("חנוכה " + jewishCalendar.getDayOfChanukah()) :
                                "Chanukah " + romanNumb[jewishCalendar.getDayOfChanukah() - 1],
                        getDimensPref(context, "szParshat", appWidgetId),
                        getColorPref(context, "cParshat", appWidgetId),
                        bHeb ? tfSTAM : tfThin,
                        getDimensPref(context, "wClockMargin", appWidgetId));
            }
            if (jewishCalendar.isRoshChodesh()) {
                Clock.addLabel(
                        bHeb ? "ראש חודש" : "Rosh Chodesh",
                        getDimensPref(context, "szParshat", appWidgetId),
                        getColorPref(context, "cParshat", appWidgetId),
                        bHeb ? tfSTAM : tfThin,
                        getDimensPref(context, "wClockMargin", appWidgetId));
            }
        }

        if (getBoolPref(context, "showParashat", appWidgetId)) {
            String s = getParshaHashavua((Calendar) sysCalendar.clone(), bHeb, false);
            if (s.length() != 0) {
                Clock.addLabel(s,
                        getDimensPref(context, "szParshat", appWidgetId),
                        getColorPref(context, "cParshat", appWidgetId),
                        bHeb ? tfSTAM : tfThin,
                        getDimensPref(context, "wClockMargin", appWidgetId));
            }
        }
        if (zcService.gps_info.act) {
            setZmanimMarks(context, appWidgetId);
            zcService.LastZmanimUpdate = Calendar.getInstance().getTime();
        }
        clockUpdated = true;
    }

    static void updateLocation(Context context) {
        zcService.gps_info = new gpsInfo(context);
        zcService.gps_info.update();
        Log.e("zcProvider GPS:", String.format("loc:%s lat:%f long:%f alt:%f tz:%s",
                zcService.gps_info.getGeolocationName(),
                zcService.gps_info.lat,
                zcService.gps_info.lng,
                zcService.gps_info.alt,
                sysCalendar.getTimeZone().toString()
        ));

        GeoLocation geoLocation = new GeoLocation(sysCalendar.getTimeZone().toString(),
                zcService.gps_info.lat, zcService.gps_info.lng, zcService.gps_info.alt, sysCalendar.getTimeZone());

        alotHarHabait = new ComplexZmanimCalendar(HarHabait).getAlos72();
        zmanimCalendar = new ComplexZmanimCalendar(geoLocation);

    }
    //endregion

    static void setZmanimMarks(Context context, int appWidgetId) {

        final Typeface tfCondN = Typeface.create(context.getString(R.string.font_condensed), Typeface.NORMAL);
        final Typeface tfLight = Typeface.create(context.getString(R.string.font_light), Typeface.NORMAL);
        final Typeface tfRegularB = Typeface.create(context.getString(R.string.font_regular), Typeface.BOLD);

        Clock.resetTimeMarks();

        //Clock Hours 0-23h
        if (getBoolPref(context, "showTimeMarks", appWidgetId)) {
            Date[] timeHours = new Date[24];
            for (int i = 0; i < 24; i++) {
                timeHours[i] = new Date(3600000 * i);
            }
            Clock.addMarks(tfCondN,
                    getColorPref(context, "cTimemarks", appWidgetId),
                    getDimensPref(context, "szTimemarks", appWidgetId),
                    getStringPref(context, "tsTimemarks", appWidgetId),
                    getBoolPref(context, "iTimemarks", appWidgetId),
                    timeHours);
        }

        int zMode = getIntPref(context, "zmanimMode", appWidgetId);
        Date[] sunsr;

        switch (zMode) {
            case 1:
                sunsr = new Date[]{zmanimCalendar.getTzais60(), zmanimCalendar.getAlos60()};
                break;
            case 2:
                sunsr = new Date[]{zmanimCalendar.getTzais72(), zmanimCalendar.getAlos72()};
                break;
            case 3:
                sunsr = new Date[]{zmanimCalendar.getTzais90(), zmanimCalendar.getAlos90()};
                break;
            case 4:
                sunsr = new Date[]{zmanimCalendar.getTzais120(), zmanimCalendar.getAlos120()};
                break;
            case 5:
                sunsr = new Date[]{zmanimCalendar.getTzais16Point1Degrees(), zmanimCalendar.getAlos16Point1Degrees()};
                break;
            case 6:
                sunsr = new Date[]{zmanimCalendar.getTzais18Degrees(), zmanimCalendar.getAlos18Degrees()};
                break;
            case 7:
                sunsr = new Date[]{zmanimCalendar.getTzais19Point8Degrees(), zmanimCalendar.getAlos19Point8Degrees()};
                break;
            case 8:
                sunsr = new Date[]{zmanimCalendar.getTzais26Degrees(), zmanimCalendar.getAlos26Degrees()};
                break;
            default:
                sunsr = new Date[]{zmanimCalendar.getSunset(), zmanimCalendar.getSunrise()};
        }

        Clock.changeNewDay(sunsr[0].getTime());
        long chatzot = zmanimCalendar.getChatzos().getTime();

        Date d1 = (zMode == 0) ? zmanimCalendar.getTzais() : zmanimCalendar.getSunset();
        Date d2 = (zMode == 0) ? zmanimCalendar.getAlosHashachar() : zmanimCalendar.getSunrise();

        Clock.addMarks(tfRegularB,
                getColorPref(context, "cZmanim_sun", appWidgetId),
                getDimensPref(context, "szZmanim_sun", appWidgetId),
                getStringPref(context, "tsZmanim_sun", appWidgetId),
                getBoolPref(context, "iZmanim_sun", appWidgetId),
                new Date[]{
                        sunsr[0],
                        sunsr[1],
                        d1,
                        d2,
                        new Date(chatzot),
                        new Date(chatzot + 43200000)});

        if (getBoolPref(context, "showZmanim", appWidgetId)) {

            Clock.addMarks(tfLight,
                    getColorPref(context, "cZmanim_main", appWidgetId),
                    getDimensPref(context, "szZmanim_main", appWidgetId),
                    getStringPref(context, "tsZmanim_main", appWidgetId),
                    getBoolPref(context, "iZmanim_main", appWidgetId),
                    new Date[]{
                            alotHarHabait,
                            zmanimCalendar.getSunriseOffsetByDegrees(AstronomicalCalendar.ASTRONOMICAL_ZENITH - 11),
                            zmanimCalendar.getSofZmanShma(sunsr[1], sunsr[0]),
                            zmanimCalendar.getSofZmanTfila(sunsr[1], sunsr[0]),
                            zmanimCalendar.getMinchaKetana(sunsr[1], sunsr[0]),
                            zmanimCalendar.getMinchaGedola(sunsr[1], sunsr[0]),
                            zmanimCalendar.getPlagHamincha(sunsr[1], sunsr[0])
                    });

        }

        Clock.updateTimeMarks();
    }

    static String toNiqqud(String txt) {
        String res = "";
        for (char c : txt.toCharArray())
            if (c < 0x041 || c == 0x05be || (c > 0x05af && c < 0x05eb && (c < 0x05bd || c > 0x05bf) && c != 0x05c3)) {
                res += c;
            }
        return res;
    }

    static String toOtiot(String txt) {
        String res = "";
        for (char c : txt.toCharArray()) {
            if (c > 0x05ef && c < 0x05eb) res += c;
        }
        return res;
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Log.e("zcProvider.onEnabled", "");
        // Enter relevant functionality for when the first widget is created
        context.startService(new Intent(zcService.ACTION_UPDATE));
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
        context.stopService(new Intent(context, zcService.class));
        Log.e("zcProvider.onDisabled", "");
        Toast.makeText(context, "zClock removed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {


        context.startService(new Intent(zcService.ACTION_UPDATE));

        //Update widgets
        updateWidgets(context, appWidgetManager, appWidgetIds);
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        // When the user deletes the widget, delete the preference associated with it.
        Log.e("zcProvider.onDeleted", String.valueOf(appWidgetIds.length));
        for (int appWidgetId : appWidgetIds) {
            removeWidgetPreferences(context, appWidgetId);
        }
        context.stopService(new Intent(context, zcService.class));
        super.onDeleted(context, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String iAction = intent.getAction();
        Log.e("zcProvider.onReceive", iAction);
        super.onReceive(context, intent);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        //workaround for kitkat issue on START_STICKY
        context.startService(new Intent(zcService.ACTION_UPDATE));

        //update widgets
        updateWidgetSize(context, appWidgetId);
        setupClockPrefs(context, appWidgetId);
        updateAppWidget(context, appWidgetManager, appWidgetId);

    }

    public void removeWidgetPreferences(Context context,int appWidgetId){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor ed = sp.edit();

        ed.remove("clockMode"+appWidgetId);

        ed.remove("zmanimMode"+appWidgetId);

        ed.remove("szZmanim_sun"+appWidgetId);
        ed.remove("szZmanim_main"+appWidgetId);
        ed.remove("szZmanimAlotTzet"+appWidgetId);
        ed.remove("szTimemarks"+appWidgetId);
        ed.remove("szTime"+appWidgetId);
        ed.remove("szDate"+appWidgetId);
        ed.remove("szParshat"+appWidgetId);
        ed.remove("szShemot"+appWidgetId);

        ed.remove("cClockFrameOn"+appWidgetId);
        ed.remove("cClockFrameOff"+appWidgetId);
        ed.remove("cZmanim_sun"+appWidgetId);
        ed.remove("cZmanim_main"+appWidgetId);
        ed.remove("cZmanimAlotTzet"+appWidgetId);
        ed.remove("cTimemarks"+appWidgetId);
        ed.remove("cTime"+appWidgetId);
        ed.remove("cDate"+appWidgetId);
        ed.remove("cParshat"+appWidgetId);
        ed.remove("cShemot"+appWidgetId);

        ed.remove("wClockMargin"+appWidgetId);
        ed.remove("wClockFrame"+appWidgetId);
        ed.remove("wClockPoer"+appWidgetId);
        ed.remove("resTimeMins "+appWidgetId);
        ed.remove("szTimeMins"+appWidgetId);

        ed.remove("tsZmanim_sun"+appWidgetId);
        ed.remove("tsZmanim_main"+appWidgetId);
        ed.remove("tsZmanimAlotTzet"+appWidgetId);
        ed.remove("tsTimemarks"+appWidgetId);

        ed.remove("iZmanim_sun"+appWidgetId);
        ed.remove("iZmanim_main"+appWidgetId);
        ed.remove("iZmanimAlotTzet"+appWidgetId);
        ed.remove("iTimemarks"+appWidgetId);

        //ed.remove("bAlotTzet72"+appWidgetId);
        ed.remove("showHebDate"+appWidgetId);
        ed.remove("showParashat"+appWidgetId);
        ed.remove("showAnaBekoach"+appWidgetId);
        ed.remove("show72Hashem"+appWidgetId);
        ed.remove("showZmanim"+appWidgetId);
        ed.remove("showTimeMarks"+appWidgetId);
        ed.remove("nShemot"+appWidgetId);

        ed.commit();

    }

}