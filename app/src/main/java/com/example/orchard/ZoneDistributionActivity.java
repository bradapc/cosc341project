package com.example.orchard;

import android.graphics.Color;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

public class ZoneDistributionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zone_distribution);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.homeButton).setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        // TODO: replace with orchard coordinates
        LatLng orchardCenter = new LatLng(50.2654, -119.2711);

        PolygonOptions blockA = new PolygonOptions()
                .add(new LatLng(50.2660, -119.2720),
                        new LatLng(50.2660, -119.2710),
                        new LatLng(50.2650, -119.2710),
                        new LatLng(50.2650, -119.2720))
                .strokeColor(Color.parseColor("#005005"))
                .fillColor(Color.argb(100, 0, 80, 5))
                .strokeWidth(5);
        mMap.addPolygon(blockA);

        PolygonOptions blockB = new PolygonOptions()
                .add(new LatLng(50.2660, -119.2708),
                        new LatLng(50.2660, -119.2698),
                        new LatLng(50.2650, -119.2698),
                        new LatLng(50.2650, -119.2708))
                .strokeColor(Color.parseColor("#F57C00"))
                .fillColor(Color.argb(100, 245, 124, 0))
                .strokeWidth(5);
        mMap.addPolygon(blockB);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(orchardCenter, 16f));
    }
}