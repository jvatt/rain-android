package net.vatt.jal.weather;

import android.Manifest;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Created by ltr on 1.10.2017.
 */

public class LocationService extends ContextWrapper {
    private FusedLocationProviderClient mFusedLocationClient;

    public LocationService (Context base) {
        super(base);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void checkLocation(OnSuccessListener listener) {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED)
            mFusedLocationClient.getLastLocation().addOnSuccessListener(listener);
    }

    public static String cityNameFromLocation(Context context, Location location){
        Geocoder gcd = new Geocoder(context, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = gcd.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(addresses != null && addresses.size() > 0)
            return addresses.get(0).getLocality();
        return "";
    }
}
