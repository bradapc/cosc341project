package com.example.orchard;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
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
    private final LatLng correctOrchardCenter = new LatLng(50.2655, -119.2709);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zone_distribution);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.homeButton).setOnClickListener(v -> finish());

        findViewById(R.id.viewMapButton).setOnClickListener(v -> openExternalMap());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void openExternalMap() {
        Uri gmmIntentUri = Uri.parse("geo:" + correctOrchardCenter.latitude + "," + correctOrchardCenter.longitude + "?z=17&q=" + correctOrchardCenter.latitude + "," + correctOrchardCenter.longitude + "(Apple+Orchard)");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(correctOrchardCenter, 17f));
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(correctOrchardCenter, 16f));
    }
}