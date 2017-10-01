package net.vatt.jal.weather;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ltr on 30.9.2017.
 */

public class FmiController extends ContextWrapper {

    public static class WeatherData {
        Date startTime;
        Date endTime;
        List<Map<String, Double>> data;

        public WeatherData() {
            this.data = new ArrayList<>();
        }

        public WeatherData(List<String> record, String dataStr) {
            this();
            List<String> splitData = new ArrayList<>();
            Pattern p = Pattern.compile("([-+\\d.])+");
            Matcher m = p.matcher(dataStr);
            while(m.find()) {
                splitData.add(m.group());
            }

            int r = splitData.size() % record.size();
            if(r == 0) {
                int intervals = splitData.size() / record.size();

                for (int i = 0; i < intervals; i++) {
                    Map<String, Double> map = new HashMap<>();
                    int index = 0;
                    for (String rec : record) {
                        map.put(rec, parseDouble(splitData.get(index++)));
                    }
                    data.add(map);
                }
            }
        }

        private Double parseDouble(String str) {
            Double d = 0.0;
            try {
                d = Double.parseDouble(str);
            } catch (NullPointerException | NumberFormatException e) {
                e.printStackTrace();
            }
            return d;
        }

        public WeatherData(List<Map<String, Double>> data, Date start, Date end) {
            this.data = data;
            this.startTime = start;
            this.endTime = end;
        }
    }

    private RequestQueue queue = null;
    private String tag = "Weather.FMI";
    private String FM_API_KEY = "8e24585a-5c8e-432e-b765-c8de61f29c99";
    private String location = "";
    private FusedLocationProviderClient mFusedLocationClient;
    private long updateRate = 30000;
    private OnSuccessListener<WeatherData> listener = null;

    private CountDownTimer weatherTimer = new CountDownTimer(updateRate, updateRate) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            checkLocation();
            this.start();
        }
    };

    private OnSuccessListener<Location> mLocationSuccessListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            if(location != null) {
                String loc = cityNameFromLocation(location);
                setLocation(loc);
                fmiGetForecastData(loc);
            }
        }
    };

    public FmiController(Context base) {
        super(base);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void setLocation(String loc) {
        location = loc;
    }

    public String getLocation() {
        return location;
    }

    public void start(OnSuccessListener<WeatherData> onSuccessListener, int interval) {
        checkLocation();
        listener = onSuccessListener;
        updateRate = interval;
        weatherTimer.start();
    }

    public void stop() {
        weatherTimer.cancel();
    }

    public void resume() {
        if(listener != null)
            weatherTimer.start();
    }

    private void checkLocation() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.getLastLocation().addOnSuccessListener(mLocationSuccessListener);
    }

    private void fmiGetForecastData(String location) {
        Log.d(tag, "Getting weather for " + location);
        initializeVolleyQueue();
        String url = "http://data.fmi.fi/fmi-apikey/" + FM_API_KEY + "/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place=" + location;
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        FmiXmlParser fmiParser = new FmiXmlParser();
                        FmiXmlParser.WeatherDataTuple w = fmiParser.readFeed(response);
                        WeatherData data = new WeatherData(w.dataRecord, w.data);
                        listener.onSuccess(data);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    private void initializeVolleyQueue(){
        // Instantiate the RequestQueue.
        if(queue == null)
            queue = Volley.newRequestQueue(this);
    }

    private String cityNameFromLocation(Location location){
        Geocoder gcd = new Geocoder(this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            Log.e(tag, "Error retrieving address from location");
            e.printStackTrace();
        }
        if(addresses != null && addresses.size() > 0)
            return addresses.get(0).getLocality();
        return "";
    }
}
