package pt.gu.zclock;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;

/**
 * Created by GU on 21-12-2014.
 */
public class zcPreferences extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener{

    static final String ACTION_PREFS="android.appwidget.action.APPWIDGET_CONFIGURE";
    private final int PREF_INT = 0, PREF_FLOAT = 1, PREF_STRING = 2, PREF_BOOLEAN = 3, PREF_COLOR = 4, PREF_SIZE = 5;
    private final String[] resType = {"integer", "dimen", "string", "bool", "color", "integer", "dimens"};
    private int mAppWidgetId=AppWidgetManager.INVALID_APPWIDGET_ID;
    private SharedPreferences sharedPreferences;
    private PrefsFragment mPrefsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.e("zcPrefs","onCreate");
        // Display the fragment as the main content.
        FragmentManager mFragmentManager = getFragmentManager();
        FragmentTransaction mFragmentTransaction = mFragmentManager
                .beginTransaction();
        mPrefsFragment = new PrefsFragment();
        mFragmentTransaction.replace(android.R.id.content, mPrefsFragment);
        mFragmentTransaction.commit();


        sharedPreferences=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        // Set up a listener whenever a key changes

    }

    @Override
    public void onStart(){
        super.onStart();
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onResume(){
        super.onResume();

        setResult(RESULT_CANCELED);
        // Find the widget id from the intent.
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        if (extras != null) {
            mAppWidgetId = extras.getInt(
                    AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        // If this activity was started with an intent without an app widget ID, finish with an error.
        if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            Log.e("zcPrefs.onResume","invalid AppWidgetId");
            finish();
        }

        Log.e("Prefs.onResume", String.valueOf(mAppWidgetId));

        //Load preferences for selected mAppWidgetId
        loadWidgetPreferences(mAppWidgetId);

        // Set up a listener whenever a key changes
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause(){
        // Unregister the listener whenever a key changes
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onStop() {
        // Unregister the listener whenever a key changes
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onStop();
    }

    @Override
    public void onBackPressed(){

        final Context context = getApplicationContext();
        // When the button is clicked, store the string locally
        saveWidgetPreferences(this,mAppWidgetId);
        

        // It is the responsibility of the configuration activity to update the app widget
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        Log.e("Exit Prefs", String.valueOf(mAppWidgetId));
        zcProvider.updateClock();
        zcProvider.updateAppWidget(context, appWidgetManager, mAppWidgetId);

        //context.startService(new Intent(zcService.ACTION_UPDATE));
        context.startService(new Intent(context,zcService.class));


        // Make sure we pass back the original appWidgetId
        Intent resultValue = new Intent();
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        setResult(RESULT_OK, resultValue);
        super.onBackPressed();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        //Log.e("onSharedPreferenceChanged", key);
        setSummary(key,null);

    }

    public void loadWidgetPreferences(int appWidgetId) {

        Context ctx = getApplicationContext();
        loadPref(ctx,PREF_INT,"clockMode",appWidgetId);
        loadPref(ctx,PREF_INT,"zmanimMode",appWidgetId);
        loadPref(ctx,PREF_INT,"resTimeMins",appWidgetId);
        loadPref(ctx,PREF_INT,"szTimeMins",appWidgetId);
        loadPref(ctx,PREF_INT,"szPtrHeight",appWidgetId);

        loadPref(ctx,PREF_SIZE,"wClockMargin",appWidgetId);
        loadPref(ctx,PREF_SIZE,"wClockFrame",appWidgetId);
        loadPref(ctx,PREF_SIZE,"wClockPointer",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szZmanim_sun",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szZmanim_main",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szZmanimAlotTzet",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szTimemarks",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szTime",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szDate",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szParshat",appWidgetId);
        loadPref(ctx,PREF_SIZE,"szShemot",appWidgetId);

        loadPref(ctx,PREF_STRING, "tsZmanim_sun",appWidgetId);
        loadPref(ctx,PREF_STRING, "tsZmanim_main",appWidgetId);
        loadPref(ctx,PREF_STRING, "tsZmanimAlotTzet",appWidgetId);
        loadPref(ctx,PREF_STRING, "tsTimemarks",appWidgetId);

        loadPref(ctx,PREF_BOOLEAN,"iZmanim_sun",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"iZmanim_main",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"iZmanimAlotTzet",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"iTimemarks",appWidgetId);

        loadPref(ctx,PREF_COLOR,"cClockFrameOn",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cClockFrameOff",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cZmanim_sun",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cZmanim_main",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cZmanimAlotTzet",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cTimemarks",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cTime",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cDate",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cParshat",appWidgetId);
        loadPref(ctx,PREF_COLOR,"cShemot",appWidgetId);

        //loadPref(ctx,PREF_BOOLEAN,"bAlotTzet72",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"showHebDate",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"showParashat",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"showAnaBekoach",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"show72Hashem",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"showZmanim",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"showTimeMarks",appWidgetId);
        loadPref(ctx,PREF_BOOLEAN,"bLangHebrew",appWidgetId);
        loadPref(ctx, PREF_BOOLEAN, "bClockElapsedTime", appWidgetId);
        loadPref(ctx, PREF_BOOLEAN, "bWhiteOnBlack", appWidgetId);

        loadPref(ctx,PREF_INT,"nShemot",appWidgetId);
    }

    private void loadPref(Context context,int type, String key, int appWidgetId){
        //Log.e("loadPref",key);
        SharedPreferences.Editor ed =sharedPreferences.edit();
        Object value=null;
        int ResId = (type==PREF_SIZE) ? 0 : context.getResources().getIdentifier(key,resType[type],context.getPackageName());
        switch (type){
            case PREF_INT:
                value = sharedPreferences.getInt(key + appWidgetId, context.getResources().getInteger(ResId));
                ed.putString(key,String.valueOf((int)value));
                break;
            case PREF_SIZE:
                value = sharedPreferences.getInt(key + appWidgetId,100);
                ed.putString(key,String.valueOf((int)value));
                break;
            case PREF_FLOAT:
                value =sharedPreferences.getFloat(key + appWidgetId, context.getResources().getDimension(ResId));
                ed.putString(key,String.valueOf((float)value));
                break;
            case PREF_STRING:
                value=sharedPreferences.getString(key + appWidgetId, context.getResources().getString(ResId));
                ed.putString(key,String.valueOf(value));
                break;
            case PREF_BOOLEAN:
                value =sharedPreferences.getBoolean(key + appWidgetId, context.getResources().getBoolean(ResId));
                ed.putBoolean(key, (boolean) value);
                break;
            case PREF_COLOR:
                value=sharedPreferences.getInt(key + appWidgetId, context.getResources().getColor(ResId));
                ed.putInt(key, (int)value);
                break;
        }
        ed.commit();
        setSummary(key,value);
    }

    private void setSummary(String key,Object value){
        //Log.e("setSummary", key);
        Preference preference = mPrefsFragment.findPreference(key);
        if (preference!=null) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                if (value != null) listPreference.setValue(String.valueOf(value));
                listPreference.setSummary(String.valueOf(listPreference.getEntry()));
            }
            if (preference instanceof ColorPreference) {
                ColorPreference colorPreference = (ColorPreference) preference;
                if (value==null) value = colorPreference.getColor();
                Spannable summary = new SpannableString(String.format("Alpha:#%02X Color:#%06X", Color.alpha((int) value), 0xFFFFFF & (int) value));
                summary.setSpan(new ForegroundColorSpan((int) value), 0, summary.length(), 0);
                colorPreference.setSummary(summary);
                //if (updatePreference) colorPreference.setValue(value);
            }
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                if (value==null) value = checkBoxPreference.isChecked();
                checkBoxPreference.setSummary((boolean)value ? "On" : "Off");
                //if (updatePreference) checkBoxPreference.setChecked(b);
            }
        }
    }

    private void saveWidgetPreferences(Context ctx,int appWidgetId) {

        savePref(ctx,PREF_INT,"clockMode",appWidgetId);
        savePref(ctx,PREF_INT,"zmanimMode",appWidgetId);
        savePref(ctx,PREF_INT,"resTimeMins",appWidgetId);
        savePref(ctx,PREF_INT,"szTimeMins",appWidgetId);
        savePref(ctx,PREF_SIZE,"wClockMargin",appWidgetId);
        savePref(ctx,PREF_SIZE,"wClockFrame",appWidgetId);
        savePref(ctx,PREF_SIZE,"wClockPointer",appWidgetId);
        savePref(ctx,PREF_INT,"szPtrHeight",appWidgetId);

        savePref(ctx,PREF_SIZE,"szZmanim_sun",appWidgetId);
        savePref(ctx,PREF_SIZE,"szZmanim_main",appWidgetId);
        savePref(ctx,PREF_SIZE,"szZmanimAlotTzet",appWidgetId);
        savePref(ctx,PREF_SIZE,"szTimemarks",appWidgetId);
        savePref(ctx,PREF_SIZE,"szTime",appWidgetId);
        savePref(ctx,PREF_SIZE,"szDate",appWidgetId);
        savePref(ctx,PREF_SIZE,"szParshat",appWidgetId);
        savePref(ctx,PREF_SIZE,"szShemot",appWidgetId);

        savePref(ctx,PREF_STRING,"tsZmanim_sun",appWidgetId);
        savePref(ctx,PREF_STRING,"tsZmanim_main",appWidgetId);
        savePref(ctx,PREF_STRING,"tsZmanimAlotTzet",appWidgetId);
        savePref(ctx,PREF_STRING,"tsTimemarks",appWidgetId);

        savePref(ctx,PREF_BOOLEAN,"iZmanim_sun",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"iZmanim_main",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"iZmanimAlotTzet",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"iTimemarks",appWidgetId);

        savePref(ctx,PREF_COLOR,"cClockFrameOn",appWidgetId);
        savePref(ctx,PREF_COLOR,"cClockFrameOff",appWidgetId);
        savePref(ctx,PREF_COLOR,"cZmanim_sun",appWidgetId);
        savePref(ctx,PREF_COLOR,"cZmanim_main",appWidgetId);
        savePref(ctx,PREF_COLOR,"cZmanimAlotTzet",appWidgetId);
        savePref(ctx,PREF_COLOR,"cTimemarks",appWidgetId);
        savePref(ctx,PREF_COLOR,"cTime",appWidgetId);
        savePref(ctx,PREF_COLOR,"cDate",appWidgetId);
        savePref(ctx,PREF_COLOR,"cParshat",appWidgetId);
        savePref(ctx,PREF_COLOR,"cShemot",appWidgetId);

        //savePref(ctx,PREF_BOOLEAN,"bAlotTzet72",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"showZmanim",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"showTimeMarks",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"showHebDate",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"showParashat",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"showAnaBekoach",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"show72Hashem",appWidgetId);
        savePref(ctx,PREF_BOOLEAN,"bLangHebrew",appWidgetId);
        savePref(ctx, PREF_BOOLEAN, "bClockElapsedTime", appWidgetId);
        savePref(ctx, PREF_BOOLEAN, "bWhiteOnBlack", appWidgetId);

        savePref(ctx,PREF_INT,"nShemot",appWidgetId);
    }

    private void savePref(Context context,int type,String key, int appWidgetId){
        SharedPreferences.Editor ed =sharedPreferences.edit();
        int ResId = (type==PREF_SIZE) ? 0 : context.getResources().getIdentifier(key,resType[type],context.getPackageName());
        switch (type){
            case PREF_INT:
                ed.putInt(key+appWidgetId,Integer.valueOf(
                        sharedPreferences.getString(key,String.valueOf(context.getResources().getInteger(ResId)))));
                break;
            case PREF_SIZE:
                ed.putInt(key+appWidgetId,Integer.valueOf(
                        sharedPreferences.getString(key,"100")));
                break;
            case PREF_FLOAT:
                ed.putFloat(key+appWidgetId,Float.valueOf(
                        sharedPreferences.getString(key,String.valueOf(context.getResources().getDimension(ResId)))));
                break;
            case PREF_STRING:
                ed.putString(key+appWidgetId,
                        sharedPreferences.getString(key,context.getResources().getString(ResId)));
                break;
            case PREF_BOOLEAN:
                ed.putBoolean(key+mAppWidgetId,
                        sharedPreferences.getBoolean(key,context.getResources().getBoolean(ResId)));
                break;
            case PREF_COLOR:
                ed.putInt(key+appWidgetId,
                        sharedPreferences.getInt(key,context.getResources().getColor(ResId)));
                break;
            default: break;
        }
        ed.commit();
    }

}
