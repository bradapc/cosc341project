package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ClockOutActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentShiftId;
    private Timestamp startTime;
    private String assignedZone = "Apple Orchard - Row B"; // Default fallback

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clock_out);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentShiftId = getIntent().getStringExtra("shiftId");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            v.setPadding(systemBars.left + padding, systemBars.top + padding, systemBars.right + padding, systemBars.bottom + padding);
            return insets;
        });

        TextView timeText = findViewById(R.id.timeText);
        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        timeText.setText(currentTime);

        TextView workerNameText = findViewById(R.id.workerNameText);
        TextView zoneNameText = findViewById(R.id.zoneNameText);
        
        loadUserData(workerNameText, zoneNameText);
        loadShiftData();

        Button confirmClockOutButton = findViewById(R.id.confirmClockOutButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(v -> finish());

        confirmClockOutButton.setOnClickListener(v -> {
            clockOutUser();
        });
    }

    private void loadUserData(TextView nameView, TextView zoneView) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String zone = documentSnapshot.getString("assignedZone");
                        
                        nameView.setText(name != null ? name : "Unknown");
                        if (zone != null) {
                            assignedZone = zone;
                            zoneView.setText(zone);
                        } else {
                            zoneView.setText(assignedZone);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ClockOutActivity", "Error loading user data", e);
                    Toast.makeText(this, "Connection error. Displaying local defaults.", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadShiftData() {
        if (currentShiftId == null) return;
        db.collection("shifts").document(currentShiftId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        startTime = documentSnapshot.getTimestamp("startTime");
                    }
                });
    }

    private void clockOutUser() {
        if (currentShiftId == null) {
            Toast.makeText(this, "No active shift found", Toast.LENGTH_SHORT).show();
            return;
        }

        Timestamp endTime = Timestamp.now();
        Map<String, Object> updates = new HashMap<>();
        updates.put("endTime", endTime);
        updates.put("active", false);

        db.collection("shifts").document(currentShiftId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    fetchHarvestStatsAndFinish(endTime);
                })
                .addOnFailureListener(e -> {
                    Log.e("ClockOutActivity", "Error updating shift", e);
                    Toast.makeText(this, "Unable to reach database. Please check your connection.", Toast.LENGTH_LONG).show();
                });
    }

    private void fetchHarvestStatsAndFinish(Timestamp endTime) {
        db.collection("harvest_logs")
                .whereEqualTo("shiftId", currentShiftId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBins = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long bins = doc.getLong("binCount");
                        if (bins != null) totalBins += bins.intValue();
                    }
                    
                    String durationStr = calculateDuration(startTime, endTime);
                    
                    Intent intent = new Intent(ClockOutActivity.this, ClockOutSuccessActivity.class);
                    intent.putExtra("duration", durationStr);
                    intent.putExtra("totalBins", totalBins);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // Even if logs fail, we should still finish the clock out process
                    Intent intent = new Intent(ClockOutActivity.this, ClockOutSuccessActivity.class);
                    intent.putExtra("duration", calculateDuration(startTime, endTime));
                    intent.putExtra("totalBins", 0);
                    startActivity(intent);
                    finish();
                });
    }

    private String calculateDuration(Timestamp start, Timestamp end) {
        if (start == null || end == null) return "0h 0m";
        long diff = end.toDate().getTime() - start.toDate().getTime();
        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff / (1000 * 60)) % 60;
        return hours + "h " + minutes + "m";
    }
}
