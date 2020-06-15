package com.example.maphw2;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleMap.OnMapLoadedCallback,
        GoogleMap.OnMarkerClickListener,
        GoogleMap.OnMapLongClickListener,
        SensorEventListener {

    private static final int MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 101;
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest mLocationRequest;
    private LocationCallback locationCallback;
    Marker gpsMarker = null;

    public String JSON_FILE = "tasks.json";
    List<MarkerPos> markerPositionList;

    static public SensorManager mSensorManager;
    private Sensor mSensor;
    private boolean Activated;
    FloatingActionButton fab_stop;
    FloatingActionButton fab_rec;
    private boolean Invisible;
    private TextView sensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        markerPositionList = new ArrayList<>();
        Activated = false;
        Invisible = true;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else { }

        fab_stop = findViewById(R.id.stop);
        fab_rec = findViewById(R.id.record);


    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
    }

    private void stopLocationUpdates() {
        if (locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void createLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {

                if (locationResult != null) {
                    if (gpsMarker != null)
                        gpsMarker.remove();
                }
            }
        };
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLoadedCallback(this);
        mMap.setOnMarkerClickListener(this);
        mMap.setOnMapLongClickListener(this);
        restoreFromJsonFile();
    }

    @Override
    public void onMapLoaded() {

        Log.i(MapsActivity.class.getSimpleName(), "MapLoaded");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }
      //  restoreMarkersFromJsonFile();
        createLocationRequest();
        createLocationCallback();
        startLocationUpdates();
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        Animation stop = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_stop);
        Animation rec = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_record);

        if (Invisible) {
            fab_stop.setVisibility(View.VISIBLE);
            fab_stop.startAnimation(stop);
            fab_rec.setVisibility(View.VISIBLE);
            fab_rec.startAnimation(rec);
            Invisible = false;
        }
        return false;
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(latLng.latitude, latLng.longitude))
                .alpha(0.8f)
                .title(String.format("Position:(%.2f, %.2f)", latLng.latitude, latLng.longitude)));
        markerPositionList.add(new MarkerPos(latLng.latitude, latLng.longitude));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("Acceleration:\nx:"+event.values[0]+" y:"+event.values[1]+" z:"+event.values[2]));

        TextView textView = findViewById(R.id.sensor);
        textView.setText(stringBuilder.toString());
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();

        if (mSensor != null) MapsActivity.mSensorManager.unregisterListener(this, mSensor);
    }

    public void hideButton(View view) {
        Animation h_stop = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide_stop);
        Animation h_rec = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide_record);
        fab_stop.startAnimation(h_stop);
        fab_stop.setVisibility(View.INVISIBLE);
        fab_rec.startAnimation(h_rec);
        fab_rec.setVisibility(View.INVISIBLE);
        Invisible = true;
        findViewById(R.id.sensor).setVisibility(View.INVISIBLE);
    }

    public void Acc_dis(View view) {
        if (Activated) {
            Activated = false;
            onPause();
            findViewById(R.id.sensor).setVisibility(View.INVISIBLE);
        } else {
            Activated = true;
            super.onResume();
            if (mSensor != null) MapsActivity.mSensorManager.registerListener(this, mSensor, 100000);
            findViewById(R.id.sensor).setVisibility(View.VISIBLE);
        }
    }

    public void zoomInClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomIn());
    }

    public void zoomOutClick(View view) {
        mMap.moveCamera(CameraUpdateFactory.zoomOut());
    }

    private void saveMarkersToJsonFile() {
        Gson gson = new Gson();
        String listJson = gson.toJson(markerPositionList);
        FileOutputStream outputStream;
        try {
            outputStream = openFileOutput(JSON_FILE, MODE_PRIVATE);
            FileWriter writer = new FileWriter(outputStream.getFD());
            writer.write(listJson);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void restoreFromJsonFile() {
        FileInputStream inputStream;
        int DEFAULT_BUFFER_SIZE = 10000;
        Gson gson = new Gson();
        String readJson;

        try {
            inputStream = openFileInput(JSON_FILE);
            FileReader reader = new FileReader(inputStream.getFD());
            char[] buf = new char[DEFAULT_BUFFER_SIZE];
            int n;
            StringBuilder builder = new StringBuilder();
            while ((n = reader.read(buf)) >= 0) {
                String tmp = String.valueOf(buf);
                String substring = (n < DEFAULT_BUFFER_SIZE) ? tmp.substring(0, n) : tmp;
                builder.append(substring);
            }
            reader.close();
            readJson = builder.toString();
            Type collectionType = new TypeToken<List<MarkerPos>>() {
            }.getType();
            List<MarkerPos> o = gson.fromJson(readJson, collectionType);
            if (o != null) {
                markerPositionList.clear();
                for (MarkerPos marker : o) {
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(marker.latitude, marker.longitude))
                            .alpha(0.8f)
                            .title(String.format("Position:(%.2f, %.2f)", marker.latitude, marker.longitude)));
                    markerPositionList.add(new MarkerPos(marker.latitude, marker.longitude));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void delMarkers(View view) {
        mMap.clear();
        markerPositionList.removeAll(markerPositionList);
        saveMarkersToJsonFile();
    }

    @Override
    protected void onDestroy() {
        saveMarkersToJsonFile();
        super.onDestroy();
    }

}
