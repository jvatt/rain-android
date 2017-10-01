package net.vatt.jal.weather;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.os.CountDownTimer;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class WeatherFragment extends Fragment {

    private final String tag = "Weather";
    private WeatherData mCurrentWeather;
    private View mRootView;
    private FmiController mFmiController;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.weather_view, container, false);

        SeekBar hourSeekBar = mRootView.findViewById(R.id.hourSeekBar);
        hourSeekBar.setOnSeekBarChangeListener(mSeekBarChangeListener);
        loadIconLabelFont();

        mFmiController = new FmiController(getActivity());

        return mRootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        //setLocation(location);
        OnSuccessListener<FmiController.WeatherData> weatherSuccessListener = new OnSuccessListener<FmiController.WeatherData>() {
            @Override
            public void onSuccess(FmiController.WeatherData w) {
                loadWeatherData(w);
            }
        };
        int position = getArguments().getInt("Position", 0);
        Date date = Utils.addToDate(Utils.removeMinutesAndSeconds(new Date()), position, 1);
        mFmiController.getForecastData("Tampere", Utils.addToDate(date, position, 0), Utils.addToDate(date, position, 24), 60,  weatherSuccessListener);
    }

    public void setmFmiController(FmiController fmiController) {
        mFmiController = fmiController;
    }

    public void setLocation(String loc) {
        TextView cityLabel = mRootView.findViewById(R.id.cityLabel);
        cityLabel.setText(loc);
    }

    public void loadWeatherData(WeatherData wd) {
        mCurrentWeather = wd;
        setDate(wd.startTime);

        setDataIndex(0);
        setSpinnerVisible(false);
        setWeatherUiVisible(true);
    }

    private void setDate(Date date) {
        TextView dateLabel = mRootView.findViewById(R.id.dateLabel);
        dateLabel.setText(Utils.dateToLocalTimeString(date));
    }

    private void setHour(int h) {
        setDate(Utils.addToDate(mCurrentWeather.startTime, 0, h));
    }

    private void setDataIndex(int i) {
        List<Map<String, Double>> d = mCurrentWeather.getData();

        if(d.size() < i)
            return;

        Map<String, Double> m = d.get(i);
        setTemperature(m.get("Temperature"));
        setCloudiness(m.get("TotalCloudCover"));
        setPrecipitation(m.get("Precipitation1h"));
        setIcon(m.get("WeatherSymbol3").intValue());
        setHour(i);
    }

    private void setTemperature(Double temp) {
        TextView temperatureLabel = mRootView.findViewById(R.id.temperatureLabel);
        temperatureLabel.setTextColor(calculateTemperatureColor(temp));
        temperatureLabel.setText(String.format(Locale.getDefault(), "%.1f °C", temp));
    }

    private void loadIconLabelFont() {
        TextView iconLabel = mRootView.findViewById(R.id.iconLabel);
        Typeface font = Typeface.createFromAsset(getActivity().getAssets(), "weathericons-regular-webfont.ttf");
        iconLabel.setTypeface(font);
    }

    private void setIcon(int code) {
        TextView iconLabel = mRootView.findViewById(R.id.iconLabel);
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
        TextView cloudinessLabel = mRootView.findViewById(R.id.cloudinessLabel);
        String percentage = Integer.toString(cloudiness.intValue());
        cloudinessLabel.setText(getString(R.string.cloudiness) + ": " + percentage + "% ");
    }

    private void setPrecipitation(Double precipitation) {
        TextView rainLabel = mRootView.findViewById(R.id.rainLabel);
        rainLabel.setText(getString(R.string.rain) + ": " + precipitation.toString() + " mm/h");
    }

    private void setSpinnerVisible(boolean visible) {
        ProgressBar spinner = mRootView.findViewById(R.id.loadingSpinner);
        spinner.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void setWeatherUiVisible(boolean visible) {
        ConstraintLayout layout = mRootView.findViewById(R.id.mainLayout);
        for(int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if(v.getId() == R.id.loadingSpinner)
                continue;
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    private String getStringResourceByName(String resName) {
        String packageName = getActivity().getPackageName();
        int resId = getResources().getIdentifier(resName, "string", packageName);
        return getString(resId);
    }
}
