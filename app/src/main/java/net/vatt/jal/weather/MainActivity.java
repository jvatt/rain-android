package net.vatt.jal.weather;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;

import net.vatt.jal.weather.FmiController.WeatherData;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private FmiController mFmiController;
    private LocationService mLocationService;
    private long updateRate = 600000; // update every ten minutes
    private WeatherData mCurrentWeather = new WeatherData();

    private OnSuccessListener<WeatherData> mWeatherSuccessListener = new OnSuccessListener<WeatherData>() {
        @Override
        public void onSuccess(WeatherData w) {
            setSpinnerVisible(false);
            setWeatherUiVisible(true);
            loadWeatherData(w);
        }
    };

    private OnSuccessListener<Location> mLocationSuccessListener = new OnSuccessListener<Location>() {
        @Override
        public void onSuccess(Location location) {
            if(location != null) {
                String loc = LocationService.cityNameFromLocation(getApplicationContext(), location);
                setLocation(loc);
                Date date = addHours(removeMinutesAndSeconds(new Date()), 1);
                mFmiController.getForecastData(loc, date, addHours(date, 24), 60,  mWeatherSuccessListener);
            }
        }
    };

    private CountDownTimer mWeatherTimer = new CountDownTimer(updateRate, updateRate) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            mLocationService.checkLocation(mLocationSuccessListener);
            this.start();
        }
    };

    private SeekBar.OnSeekBarChangeListener mSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            setDataIndex(i);
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestLocationPermission();

        mFmiController = new FmiController(this);
        mLocationService = new LocationService(this);

        mLocationService.checkLocation(mLocationSuccessListener);
        mWeatherTimer.start();

        SeekBar hourSeekBar = findViewById(R.id.hourSeekBar);
        hourSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        loadIconLabelFont();
    }

    @Override
    public void onResume() {
        super.onResume();
        mLocationService.checkLocation(mLocationSuccessListener);
        mWeatherTimer.start();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWeatherTimer.cancel();
    }

    private void setLocation(String loc) {
        TextView cityLabel = findViewById(R.id.cityLabel);
        cityLabel.setText(loc);
    }

    private void setDate(Date date) {
        TextView dateLabel = findViewById(R.id.dateLabel);
        dateLabel.setText(dateToLocalTimeString(date));
    }

    private void setHour(int h) {
        setDate(addHours(mCurrentWeather.startTime, h));
    }

    private void loadWeatherData(WeatherData wd) {
        mCurrentWeather = wd;
        setDate(wd.startTime);
        setDataIndex(0);
    }

    private void setDataIndex(int i) {
        List<Map<String, Double>> d = mCurrentWeather.getData();

        if(d.size() < i || d.size() == 0)
            return;

        Map<String, Double> m = d.get(i);
        setTemperature(m.get("Temperature"));
        setCloudiness(m.get("TotalCloudCover"));
        setPrecipitation(m.get("Precipitation1h"));
        setIcon(m.get("WeatherSymbol3").intValue());
        setHour(i);
    }

    private void setTemperature(Double temp) {
        TextView temperatureLabel = findViewById(R.id.temperatureLabel);
        temperatureLabel.setTextColor(calculateTemperatureColor(temp));
        temperatureLabel.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
    }

    private void loadIconLabelFont() {
        TextView iconLabel = findViewById(R.id.iconLabel);
        Typeface font = Typeface.createFromAsset(getAssets(), "weathericons-regular-webfont.ttf");
        iconLabel.setTypeface(font);
    }

    private void setIcon(int code) {
        TextView iconLabel = findViewById(R.id.iconLabel);
        Calendar cal = Calendar.getInstance();
        String iconName;
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        if(hour < 6 || hour > 20)
            iconName = WeatherIcons.getNameForIconCodeNight(code);
        else
            iconName = WeatherIcons.getNameForIconCodeDay(code);
        iconLabel.setText(getStringResourceByName(iconName));
    }

    private int calculateTemperatureColor(Double temp) {
        int max_temp = 25;
        int min_temp = -25;

        double red, green, blue;
        if(temp > 0.0) {
            red = temp > max_temp ? 255 : temp / max_temp * 255;
            green = 100;
            blue = 255 - red < 50 ? 50 : 255 - red;
        }
        else {
            red = temp < min_temp ? 255 : Math.abs(temp) / Math.abs(min_temp) * 255;
            blue = 255;
            green = temp < min_temp ? 255 : Math.min(Math.abs(temp) / Math.abs(min_temp) * 255 + 100, 255);
        }

        return Color.rgb((int)red, (int)green, (int)blue);
    }

    private void setCloudiness(Double cloudiness) {
        TextView cloudinessLabel = findViewById(R.id.cloudinessLabel);
        String percentage = Integer.toString(cloudiness.intValue());
        cloudinessLabel.setText(getString(R.string.cloudiness) + ": " + percentage + "% ");
    }

    private void setPrecipitation(Double precipitation) {
        TextView rainLabel = findViewById(R.id.rainLabel);
        rainLabel.setText(getString(R.string.rain) + ": " + precipitation.toString() + " mm/h");
    }

    private void setSpinnerVisible(boolean visible) {
        ProgressBar spinner = findViewById(R.id.loadingSpinner);
        spinner.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setWeatherUiVisible(boolean visible) {
        ConstraintLayout layout = findViewById(R.id.mainLayout);
        for(int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if(v.getId() == R.id.loadingSpinner)
                continue;
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String getStringResourceByName(String resName) {
        String packageName = getPackageName();
        int resId = getResources().getIdentifier(resName, "string", packageName);
        return getString(resId);
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{ Manifest.permission.ACCESS_COARSE_LOCATION }, 666);
            }
        }
    }

    private Date addHours(Date date, int hours) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.HOUR_OF_DAY, hours);
        return calendar.getTime();
    }

    private Date removeMinutesAndSeconds(Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    private String dateToLocalTimeString(Date date) {
        Calendar cal = Calendar.getInstance();
        TimeZone tz = cal.getTimeZone();
        SimpleDateFormat format = new SimpleDateFormat("E d MMM HH:mm", Locale.getDefault());
        format.setTimeZone(tz);
        return format.format(date);
    }
}
