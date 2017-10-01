package net.vatt.jal.weather;

import android.util.SparseArray;

/**
 * Created by ltr on 1.10.2017.
 */

public class WeatherIcons {
    /**
     * List of weather icons mapped to icon codes provided by FMI (http://ilmatieteenlaitos.fi/latauspalvelun-pikaohje)
     */
    private static final SparseArray<String> icons = new SparseArray<>();
    static {
        icons.put(1, "clear");
        icons.put(2, "half_cloudy");
        icons.put(3, "cloudy");
        icons.put(21, "weak_showers");
        icons.put(22, "showers");
        icons.put(23, "strong_showers");
        icons.put(31, "rain");
        icons.put(32, "rain");
        icons.put(33, "strong_rain");
        icons.put(41, "snow_shower");
        icons.put(42, "snow_shower");
        icons.put(43, "snow_shower");
        icons.put(51, "snow");
        icons.put(52, "snow");
        icons.put(53, "snow");
        icons.put(61, "thunder");
        icons.put(62, "heavy_thunder");
        icons.put(63, "thunder");
        icons.put(64, "heavy_thunder");
        icons.put(71, "snow");
        icons.put(72, "snow");
        icons.put(73, "snow");
        icons.put(81, "snow");
        icons.put(82, "snow");
        icons.put(83, "snow");
        icons.put(91, "light_fog");
        icons.put(92, "fog");
    }

    public static String getNameForIconCodeDay(Integer code) {
        return "wi_day_" + icons.get(code);
    }

    public static String getNameForIconCodeNight(Integer code) {
        return "wi_night_" + icons.get(code);
    }
}
