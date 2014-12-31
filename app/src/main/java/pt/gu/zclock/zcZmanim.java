package pt.gu.zclock;

import android.app.WallpaperManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.Base64;
import android.util.Log;
import android.widget.Toast;

import net.sourceforge.zmanim.ComplexZmanimCalendar;
import net.sourceforge.zmanim.hebrewcalendar.HebrewDateFormatter;
import net.sourceforge.zmanim.hebrewcalendar.JewishCalendar;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by GU on 30-12-2014.
 */
public class zcZmanim {

    private int mAppWidgetId;
    private Context mContext;
    private SharedPreferences mSharedPreferences;

    private Calendar sysCalendar;
    private ComplexZmanimCalendar zCalendar;
    private JewishCalendar jCalendar;
    private HebrewDateFormatter hebFormat;
    private Date mNewDay;

    public zcZmanim(Context context,int appWidgetId){

        //initilize vars
        this.mContext = context;
        this.mAppWidgetId=appWidgetId;
        this.mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        //calendar
        this.sysCalendar = Calendar.getInstance();
        this.mNewDay = new Date(getNewDayTimeMilis());
        sysCalendar.add(Calendar.MINUTE,(int)((86400000-this.mNewDay.getTime()%86400000)/60000));
        this.jCalendar = new JewishCalendar(sysCalendar);
        
        //formatter
        this.hebFormat=new HebrewDateFormatter();
    }

    private long getNewDayTimeMilis(){

        switch(getIntPref("zmanimMode")){
            case 1: return zCalendar.getTzais60().getTime();
            case 2: return zCalendar.getTzais72().getTime();
            case 3: return zCalendar.getTzais90().getTime();
            case 4: return zCalendar.getTzais120().getTime();
            case 5: return zCalendar.getTzais16Point1Degrees().getTime();
            case 6: return zCalendar.getTzais18Degrees().getTime();
            case 7: return zCalendar.getTzais19Point8Degrees().getTime();
            case 8: return zCalendar.getTzais26Degrees().getTime();
            default: return zCalendar.getSunset().getTime();
        }
    }

    private int getAverageColorFromCurrentWallpaper(int samples){
        float[] hsv = new float[3];
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(mContext);
        Drawable drawable= wallpaperManager.getDrawable();
        Bitmap bitmap = ((BitmapDrawable)drawable).getBitmap();
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int n=0;
        float h=0f, s=0f, v=0f;
        for (int i=0;i<bitmap.getWidth();i +=width/samples){
            for (int j=0;j<bitmap.getHeight(); j+=height/samples){
                Color.colorToHSV(bitmap.getPixel(i,j),hsv);
                h+=hsv[0];
                s+=hsv[1];
                v+=hsv[2];
                n++;
            }
        }
        hsv = new float[]{h/n,s/n,v/n};
        //hsv = new float[]{h/n,1,1};
        return Color.HSVToColor(hsv);
    }

    public class labelFormat{
        private float size;
        private int color;
        private Typeface typeface;

        public labelFormat(Typeface typeface,int color, float size){
            this.typeface=typeface;
            this.color=color;
            this.size=size;
        }
    }

    public class timeLabels{
        private labelFormat format;
        private HashMap<Date,String> timeLabel;
        public timeLabels(labelFormat format){
            this.format=format;
        }
        public void add(Date date, String label){
            timeLabel.put(date,label);
        }

        public void remove(Date date){
            timeLabel.remove(date);
        }

    }

    public class clockLayout{
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

        public clockLayout(){}

        public void getFromPreferences(Context context, int appWidgetId){
            this.sizeFrame=getDimensPref("wClockFrame");
            this.szPrtWidth=getDimensPref("wClockPointer");
            this.szPtrHeight=getIntPref("szPtrHeight")/100f;
            this.resTimeMins=getIntPref("resTimeMins");
            this.szTimeMins=getIntPref("szTimeMins")/100f;
            this.colorFrameForeground=getColorPref("cClockFrameOn");
            this.colorFrameBackground=getColorPref("cClockFrameOff");
            this.renderElapsedTime=getBoolPref("bClockElapsedTime");
            int i =getIntPref("clockMode");
            this.noRotation = (i==2);
            this.rotationDeg = (i==1) ? 90f : -90f;
        }
    }

    //region parsha methods

    private Bitmap getCurrentPasuk(Context context, PointF size, int appWidgetId, int type, int fColor, int bColor){

        Bitmap bitmap = Bitmap.createBitmap((int)size.x, (int)size.y, Bitmap.Config.ARGB_8888);
        bitmap = renderBackground(bitmap,0x20000000,13);

        final Typeface tfStam = Typeface.createFromAsset(context.getAssets(), "fonts/sefstm.ttf");
        int l=0, index=0;
        int d = jCalendar.getDayOfWeek()-1;
        int dm = (int)((zCalendar.getSunset().getTime()-zCalendar.getSunrise().getTime())/60000);
        int m = (int)(System.currentTimeMillis()/60000%1440);
        String pasuk, ref;
        try{
            int i=getParshaHashavuaIndex(sysCalendar);
            String[] source = context.getResources().getStringArray(R.array.torah)[i].split("#");
            String[] parsha = new String(Base64.decode(source[1], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            String[] yom = new String(Base64.decode(source[0], Base64.DEFAULT), "UTF-8").split("\\r?\\n");
            int v=0;
            for(int n=0;n<d;n++){
                v +=Integer.valueOf(yom[n]);
            }
            index=1+v+(int)(m*Integer.valueOf(yom[d])/1440);
            //Log.e("Parashat", String.format("parsha %d day %d line %d/%d",i,d+1,v,index));
            pasuk = parsha[index];
            int iref = pasuk.indexOf(" ");
            ref = (iref>0) ? String.format ("%s %s",getParshaHashavua(sysCalendar,true,true),pasuk.substring(0,iref)) : "error";
            pasuk = (iref>0) ? pasuk.substring(iref + 1) : String.format("L:%d SUM:%d I:%d",l,v,index);
        }
        catch(UnsupportedEncodingException ignored){
            Log.e("Encoding Error", "");
            return bitmap;
        }
        catch(ArrayIndexOutOfBoundsException ignored){
            Toast.makeText(context, "Parashat Hashavua resource is missing", Toast.LENGTH_LONG).show();
            return bitmap;
        }
        catch(StringIndexOutOfBoundsException ignored){
            Log.e("String Index out of Bounds","");
            return bitmap;
        }

        renderTextBlock(bitmap,new labelFormat(tfStam,0x80ffffff,32f),ref,bitmap.getHeight()*0.9f-26f,false);

        return renderTextBlock(bitmap,new labelFormat(tfStam,0xa0ffffff, 50f),pasuk,0,false);

    }

    private String getParshaHashavua(Calendar c, boolean inHebrew, boolean parshaNameOnly) {
        int day=c.get(Calendar.DAY_OF_WEEK);
        String result = (parshaNameOnly) ? "" : (inHebrew) ? ((day%7==0) ? "שבת " : "פרשת השבוע ") : ((day%7==0) ? "Shabbat " : "Parashat Hashavua ");
        c.add(Calendar.DATE, 7 - day);
        hebFormat.setHebrewFormat(inHebrew);
        return result+hebFormat.formatParsha(new JewishCalendar(c.getTime()));
    }

    private int getParshaHashavuaIndex(Calendar cal){
        Calendar c = (Calendar)cal.clone();
        int day=c.get(Calendar.DAY_OF_WEEK);
        c.add(Calendar.DATE, 7 - day);
        return new JewishCalendar(c).getParshaIndex();
    }

    //endregion

    //region Draw methods

    public Bitmap renderBackground(Bitmap bitmap, int bkgColor, float corners) {
        Canvas canvas=new Canvas(bitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(bkgColor);
        canvas.drawRoundRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), corners, corners, p);
        return bitmap;
    }

    public Bitmap renderTextBlock(Bitmap bitmap, labelFormat format, String text, float dy, boolean glowEffect){

        Canvas canvas = new Canvas(bitmap);

        TextPaint p = new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.LINEAR_TEXT_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(format.color);
        p.setTypeface(format.typeface);
        p.setTextSize(format.size);
        canvas.translate(-12, 12 + dy);
        canvas.save();
        if (glowEffect) p.setMaskFilter(new BlurMaskFilter(7f, BlurMaskFilter.Blur.NORMAL));
        StaticLayout sl = new StaticLayout(""+text,p,(int)(bitmap.getWidth()*0.95f), Layout.Alignment.ALIGN_NORMAL,1.0f,0.0f,false);
        sl.draw(canvas);
        if (glowEffect) {
            p.setMaskFilter(null);
            sl = new StaticLayout("" + text, p, (int) (bitmap.getWidth() * 0.95f), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            sl.draw(canvas);
        }
        canvas.restore();
        return bitmap;
    }

    public Bitmap renderTextLine(Bitmap bitmap, labelFormat format, String text, PointF position){

        Canvas canvas = new Canvas(bitmap);

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG|Paint.LINEAR_TEXT_FLAG|Paint.SUBPIXEL_TEXT_FLAG);
        p.setColor(format.color);
        p.setTypeface(format.typeface);
        p.setTextSize(format.size);
        canvas.drawText(text,position.x,position.y,p);
        return bitmap;
    }

    public Bitmap renderClock(Bitmap backgroundBitmap,clockLayout layout) {

        int pxRad = Math.min(backgroundBitmap.getHeight(), backgroundBitmap.getWidth());
        if (layout.clockCenter==null) layout.clockCenter = new PointF(backgroundBitmap.getWidth()/2,backgroundBitmap.getHeight()/2);
        float newdaytime_angle = ((this.mNewDay.getTime() / 60000f) % 1440)/4;
        float time = System.currentTimeMillis() / 60000f % 1440;
        float time_angle = (time / 4) % 360;
        float angle_offset = layout.noRotation ? -time_angle:layout.rotationDeg;

        Canvas canvas = new Canvas(backgroundBitmap);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        //Draw Clock background Frame
        p.setColor(layout.colorFrameBackground);
        p.setStrokeWidth(layout.sizeFrame);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(layout.clockCenter.x, layout.clockCenter.y, pxRad, p);

        //Elapsed Time
        p.setColor(layout.colorFrameForeground);
        p.setPathEffect(renderDashPathEffect(pxRad,layout.resTimeMins,layout.szTimeMins));
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

        return backgroundBitmap;
    }

    private DashPathEffect renderDashPathEffect(float rad, int resTimeMins, float szTimeMins){
        szTimeMins = szTimeMins%1;
        float f =(float)(Math.PI*rad*resTimeMins/720);
        return new DashPathEffect(new float[]{f*szTimeMins,f*(1-szTimeMins)},0);
    }


        //endregion

    //region getPreferences Methods
    private int getIntPref(String key){
        int ResId = mContext.getResources().getIdentifier(key,"integer",mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, mContext.getResources().getInteger(ResId));
    }
    private float getDimensPref(String key){
        int ResId = mContext.getResources().getIdentifier(key,"dimen",mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, 100)/100f*mContext.getResources().getDimension(ResId);
    }
    private float getSizePref(String key){
        int ResId = mContext.getResources().getIdentifier(key,"dimen",mContext.getPackageName());
        return mSharedPreferences.getFloat(key + mAppWidgetId, mContext.getResources().getDimension(ResId));
    }
    private String getStringPref(String key) {
        int ResId = mContext.getResources().getIdentifier(key,"string",mContext.getPackageName());
        return mSharedPreferences.getString(key + mAppWidgetId, mContext.getResources().getString(ResId));
    }
    private boolean getBoolPref(String key){
        int ResId = mContext.getResources().getIdentifier(key,"bool",mContext.getPackageName());
        return mSharedPreferences.getBoolean(key + mAppWidgetId, mContext.getResources().getBoolean(ResId));
    }
    private int getColorPref(String key){
        int ResId = mContext.getResources().getIdentifier(key,"color",mContext.getPackageName());
        return mSharedPreferences.getInt(key + mAppWidgetId, mContext.getResources().getColor(ResId));
    }
    //endregion
}
