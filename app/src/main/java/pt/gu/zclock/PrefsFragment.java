package pt.gu.zclock;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Created by GU on 24-12-2014.
 */
public class PrefsFragment
        extends PreferenceFragment
        implements Preference.OnPreferenceClickListener{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.zc_prefs);

        //Reset prefs to default click
        findPreference("resetDefault").setOnPreferenceClickListener(this);

        //Set Click Update Location
        findPreference("updateLocation").setOnPreferenceClickListener(this);

        //Set Click Update Location
        findPreference("removeAll").setOnPreferenceClickListener(this);

        //Update Location
        updateLocationInfo(findPreference("updateLocation"));

    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        Log.e("onPreferenceClick", key);
        switch (key){
            case "resetDefault":
                setWidgetDefaultPreferences();
                break;
            case "updateLocation":
                try {
                    zcService.gps_info.update();
                    String s = zcService.gps_info.act ? String.format("loc:%s lat:%f long:%f alt:%f",
                            zcService.gps_info.getGeolocationName(),
                            zcService.gps_info.lat,
                            zcService.gps_info.lng,
                            zcService.gps_info.alt
                    ) : "no gps info";
                    preference.setSummary(s);
                    //updateLocationInfo();
                }catch(NullPointerException ignored){}
                break;
            case "removeAll":
                break;
        }
        return false;
    }

    private void setWidgetDefaultPreferences(){

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
        SharedPreferences.Editor ed = sharedPreferences.edit();

        ed.putString("clockMode","0");

        ed.putString("szZmanim_sun","7");
        ed.putString("szZmanim_main","5");
        ed.putString("szZmanimAlotTzet","5");
        ed.putString("szTimemarks","4");
        ed.putString("szTime","90");
        ed.putString("szDate","26");
        ed.putString("szParshat","26");
        ed.putString("szShemot","144");

        ed.putString("tsZmanim_sun","HH:mm");
        ed.putString("tsZmanim_main","HH:mm");
        ed.putString("tsZmanimAlotTzet","HH:mm");
        ed.putString("tsTimemarks","HH");

        ed.putBoolean("iZmanim_sun",false);
        ed.putBoolean("iZmanim_main",false);
        ed.putBoolean("iZmanimAlotTzet",false);
        ed.putBoolean("iTimemarks",false);

        ed.putInt("cClockFrameOn", 0xff00c3ff);
        ed.putInt("cClockFrameOff", 0x1000c3ff);
        ed.putInt("cZmanim_sun", 0xff00c3ff);
        ed.putInt("cZmanim_main", 0xa00090ff);
        ed.putInt("cZmanimAlotTzet", 0x800090c0);
        ed.putInt("cTimemarks", 0x80ffffff);
        ed.putInt("cTime", 0xc3ffffff);
        ed.putInt("cDate", 0x80ffffff);
        ed.putInt("cParshat", 0x80ffffff);
        ed.putInt("cShemot", 0x07ffffff);

        ed.putString("wClockMargin","32");
        ed.putString("wClockFrame","7");
        ed.putString("wClockPointer","0.34");
        ed.putString("resTimeMins", "2");
        ed.putString("szTimeMins", "10");

        //ed.putBoolean("bAlotTzet72",true);
        ed.putBoolean("showHebDate", true);
        ed.putBoolean("showParashat", true);
        ed.putBoolean("showAnaBekoach", true);
        ed.putBoolean("show72Hashem", true);
        ed.putBoolean("showZmanim", true);
        ed.putBoolean("showTimeMarks", true);

        ed.putString("nShemot","20");

        ed.apply();
    }

    private void updateLocationInfo(Preference preference) {
        zcService.gps_info.update();
        try {
            String s = zcService.gps_info.act ? String.format("loc:%s lat:%f long:%f alt:%f",
                    zcService.gps_info.getGeolocationName(),
                    zcService.gps_info.lat,
                    zcService.gps_info.lng,
                    zcService.gps_info.alt
            ) : "no gps info";
            preference.setSummary(s);
        }
        catch (NullPointerException ignored){}
    }
}
