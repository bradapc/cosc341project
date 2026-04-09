package com.example.orchard;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolygonOptions;

public class ZoneDistributionActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private final LatLng orchardCenter = new LatLng(50.26405037998366, -119.30966114359651);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_zone_distribution);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        findViewById(R.id.homeButton).setOnClickListener(v -> finish());
        findViewById(R.id.viewMapButton).setOnClickListener(v -> openExternalMap());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void openExternalMap() {
        Uri gmmIntentUri = Uri.parse("geo:" + orchardCenter.latitude + "," + orchardCenter.longitude + "?z=17&q=" + orchardCenter.latitude + "," + orchardCenter.longitude + "(Apple+Orchard)");
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            if (mMap != null) {
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(orchardCenter, 17f));
            }
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        PolygonOptions blockA = new PolygonOptions()
                .add(new LatLng(50.26455, -119.31076),
                        new LatLng(50.26455, -119.30976),
                        new LatLng(50.26355, -119.30976),
                        new LatLng(50.26355, -119.31076))
                .strokeColor(Color.parseColor("#005005"))
                .fillColor(Color.argb(100, 0, 80, 5))
                .strokeWidth(5);
        mMap.addPolygon(blockA);

        PolygonOptions blockB = new PolygonOptions()
                .add(new LatLng(50.26455, -119.30956),
                        new LatLng(50.26455, -119.30856),
                        new LatLng(50.26355, -119.30856),
                        new LatLng(50.26355, -119.30956))
                .strokeColor(Color.parseColor("#F57C00"))
                .fillColor(Color.argb(100, 245, 124, 0))
                .strokeWidth(5);
        mMap.addPolygon(blockB);

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(orchardCenter, 16f));
    }
}