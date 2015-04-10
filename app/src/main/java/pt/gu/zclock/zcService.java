package pt.gu.zclock;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import net.sourceforge.zmanim.util.GeoLocation;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Created by GU on 12-11-2014.
 */
public final class zcService extends Service{

    static final String ACTION_UPDATE = "zClock.action.SERVICE";
    private final static IntentFilter sIntentFilter;
    static {
        sIntentFilter = new IntentFilter();
        sIntentFilter.addAction(Intent.ACTION_TIME_TICK);
        sIntentFilter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        sIntentFilter.addAction(Intent.ACTION_TIME_CHANGED);
        sIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        sIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
    }

    static Date LastZmanimUpdate;
    static gpsInfo gps_info;
    //private PowerManager pm;
    private boolean pmScreenOn=true;
    private ComponentName widgets;
    private AppWidgetManager manager;

    private final BroadcastReceiver mTimeChangedReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            final String action = intent.getAction();

            if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.e("zcService.onReceive", "Screen OFF, suspending...");
                pmScreenOn = false;
            } else if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.e("zcService.onReceive", "Screen ON: updating");
                pmScreenOn = true;
                zcProvider.updateWidgets(context, manager, manager.getAppWidgetIds(widgets));
            }

            if (pmScreenOn) {
                Log.e("zcService.onReceive", "Screen on");
                if (action.equals(Intent.ACTION_TIME_CHANGED) ||
                        action.equals(Intent.ACTION_TIMEZONE_CHANGED)) {
                    zcProvider.updateLocation(context);
                    Log.e("onReceive", "Time/Timezone changed");
                }

                if (action.equals(Intent.ACTION_TIME_TICK)) {
                    Log.e("zcService.onReceive", "Time-tick");
                    if (!gps_info.act) {
                        gps_info.update();
                        zcProvider.updateLocation(context);
                    }
                    zcProvider.updateWidgets(context, manager, manager.getAppWidgetIds(widgets));

                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        widgets = new ComponentName(getApplicationContext(), zcProvider.class);
        manager = AppWidgetManager.getInstance(getApplicationContext());
        gps_info = new gpsInfo(getApplicationContext());

        try {
            PackageManager packageManager = getPackageManager();
            if (packageManager != null) {
                Log.e("zcService.onCreate", "packagemanager dont kill app");
                packageManager.setComponentEnabledSetting(
                        new ComponentName(this, zcService.class),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
            }
        } catch (Exception ignore) {
        Log.e("zcService.onCreate", "packagemanager failed");
        }

        //Register Receiver
        registerReceiver(mTimeChangedReceiver, sIntentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mTimeChangedReceiver);
        Toast.makeText(getApplicationContext(), "Zmanim Clock Service Stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Log.e("zcService.onStartCommand", "");
        super.onStartCommand(intent, flags, startId);
        registerReceiver(mTimeChangedReceiver, sIntentFilter);
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    static final class gpsInfo {
        double lat;
        double lng;
        double alt;
        boolean act;
        long timestamp;
        String provider;
        private Context context;

        public gpsInfo(Context context) {
            act = false;
            this.context=context;
        }

        public void update() {
            LocationManager lm = (LocationManager) context.getSystemService(
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
            }else act=false;
        }

        public String getGeolocationName() {
            String _Location = null;
            if (act) {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                try {
                    List<Address> listAddresses = geocoder.getFromLocation(lat, lng, 1);
                    if (null != listAddresses && listAddresses.size() > 0) {
                        _Location = listAddresses.get(0).getAddressLine(0);
                    }
                } catch (IOException ignored) {}
            }
            return _Location;
        }
        
        public GeoLocation geoLocation(){
            String n = getGeolocationName();
            Calendar c = Calendar.getInstance();
            return new GeoLocation(c.getTimeZone().getDisplayName(),
                this.lat, this.lng, this.alt, c.getTimeZone());
        }
    }
}
