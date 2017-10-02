package net.vatt.jal.weather;

import android.content.Context;
import android.content.ContextWrapper;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnSuccessListener;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ltr on 30.9.2017.
 */

public class FmiController extends ContextWrapper {

    static class WeatherData {
        Date startTime;
        Date endTime;
        private List<Map<String, Double>> data;

        WeatherData() {
            this.data = new ArrayList<>();
        }

        WeatherData(List<Map<String, Double>> data, Date start, Date end) {
            this.data = data;
            this.startTime = start;
            this.endTime = end;
        }

        WeatherData(List<String> record, String dataStr, Date start, Date end) {
            this();
            startTime = start;
            endTime = end;
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
                    int index = i * (intervals - 1);
                    for (String rec : record) {
                        map.put(rec, parseDouble(splitData.get(index++)));
                    }
                    data.add(map);
                }
            }
        }

        public void setData (List<Map<String, Double>> d) {
            data = d;
        }

        public List<Map<String, Double>> getData() {
            return data;
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
    }

    private RequestQueue queue = null;
    private final String FM_API_KEY = "8e24585a-5c8e-432e-b765-c8de61f29c99";

    public FmiController(Context base) {
        super(base);
    }

    public void getForecastData(String location, final Date start, final Date end, Integer timeStep, final OnSuccessListener<WeatherData> listener) {
        initializeVolleyQueue();
        String url = "http://data.fmi.fi/fmi-apikey/" + FM_API_KEY + "/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::multipointcoverage&place="
                + location
                + "&starttime=" + dateToIso8601(start)
                + "&endtime=" + dateToIso8601(end)
                + "&timestep=" + timeStep.toString();

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        FmiXmlParser fmiParser = new FmiXmlParser();
                        FmiXmlParser.WeatherDataTuple w = fmiParser.readFeed(response);
                        WeatherData data = new WeatherData(w.dataRecord, w.data, start, end);
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

    private String dateToIso8601(Date date) {
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'", Locale.getDefault());
        dateFormat.setTimeZone(timeZone);
        return dateFormat.format(date);
    }

    private void initializeVolleyQueue(){
        // Instantiate the RequestQueue.
        if(queue == null)
            queue = Volley.newRequestQueue(this);
    }
}
