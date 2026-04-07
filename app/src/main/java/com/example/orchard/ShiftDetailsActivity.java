package com.example.orchard;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ShiftDetailsActivity extends AppCompatActivity {

    private TextView detailWorkerName, detailZone, detailStartTime, detailEndTime, detailDuration;
    private TextView detailEstimatedEarnings, detailEarningsBreakdown, detailHarvestBreakdownText;
    private TextView managerNotesText;
    private View detailEarningsCard, managerNotesCard;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private double hourlyRate = 19.0;

    private final Map<String, Double> binRates = new HashMap<String, Double>() {{
        put("Apples", 3.50);
        put("Pears", 4.00);
        put("Cherries", 7.00);
        put("Peaches", 5.50);
        put("Plums", 4.50);
    }};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shift_details);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        detailWorkerName = findViewById(R.id.detailWorkerName);
        detailZone = findViewById(R.id.detailZone);
        detailStartTime = findViewById(R.id.detailStartTime);
        detailEndTime = findViewById(R.id.detailEndTime);
        detailDuration = findViewById(R.id.detailDuration);
        detailHarvestBreakdownText = findViewById(R.id.detailHarvestBreakdownText);
        
        managerNotesCard = findViewById(R.id.managerNotesCard);
        managerNotesText = findViewById(R.id.managerNotesText);
        
        detailEarningsCard = findViewById(R.id.detailEarningsCard);
        detailEstimatedEarnings = findViewById(R.id.detailEstimatedEarnings);
        detailEarningsBreakdown = findViewById(R.id.detailEarningsBreakdown);
        
        Button backButton = findViewById(R.id.backToEarningsButton);
        backButton.setOnClickListener(v -> finish());

        loadUserData();
        loadShiftDetails();
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double rate = doc.getDouble("hourlyRate");
                        if (rate != null) hourlyRate = rate;
                    }
                });
    }

    private void loadShiftDetails() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();

        db.collection("users").document(userId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) detailWorkerName.setText("Worker: " + doc.getString("name"));
                });

        db.collection("shifts")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    QueryDocumentSnapshot latestShift = null;
                    Timestamp latestTime = null;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp currentStart = doc.getTimestamp("startTime");
                        if (latestTime == null || (currentStart != null && currentStart.compareTo(latestTime) > 0)) {
                            latestTime = currentStart;
                            latestShift = doc;
                        }
                    }

                    if (latestShift != null) {
                        displayShiftData(latestShift);
                    } else {
                        detailHarvestBreakdownText.setText("No shifts found.");
                    }
                })
                .addOnFailureListener(e -> Log.e("ShiftDetails", "Error loading shifts", e));
    }

    private void displayShiftData(QueryDocumentSnapshot doc) {
        Timestamp start = doc.getTimestamp("startTime");
        Timestamp end = doc.getTimestamp("endTime");
        String zone = doc.getString("zone");
        String shiftId = doc.getId();
        boolean isActive = doc.getBoolean("active") != null && doc.getBoolean("active");
        String managerNotes = doc.getString("managerNotes");

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        detailStartTime.setText("Start Time: " + (start != null ? sdf.format(start.toDate()) : "--"));
        detailZone.setText("Assigned Zone: " + (zone != null ? zone : "Apple Orchard - Row B"));

        if (isActive || end == null) {
            detailEndTime.setText("End Time: Active Now");
            detailEarningsCard.setVisibility(View.GONE);
            managerNotesCard.setVisibility(View.GONE);
        } else {
            detailEndTime.setText("End Time: " + sdf.format(end.toDate()));
            detailEarningsCard.setVisibility(View.VISIBLE);
            
            if (managerNotes != null && !managerNotes.isEmpty()) {
                managerNotesCard.setVisibility(View.VISIBLE);
                managerNotesText.setText(managerNotes);
            } else {
                managerNotesCard.setVisibility(View.GONE);
            }
        }

        // Always calculate/load harvest production regardless of active status
        calculateSessionEarnings(shiftId, start, end, doc);

        if (start != null) {
            long endTimeMillis = (end != null) ? end.toDate().getTime() : new Date().getTime();
            long diff = endTimeMillis - start.toDate().getTime();
            long hours = diff / (60 * 60 * 1000);
            long minutes = (diff / (1000 * 60)) % 60;
            detailDuration.setText("Total Time on Shift: " + hours + "h " + minutes + "m");
        }
    }

    private void calculateSessionEarnings(String shiftId, Timestamp start, Timestamp end, QueryDocumentSnapshot shiftDoc) {
        db.collection("harvest_logs")
                .whereEqualTo("shiftId", shiftId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double binPay = 0;
                    Map<String, Integer> breakdown = new HashMap<>();
                    
                    for (QueryDocumentSnapshot logDoc : queryDocumentSnapshots) {
                        String crop = logDoc.getString("cropType");
                        Long bins = logDoc.getLong("binCount");
                        if (bins != null && crop != null) {
                            int count = bins.intValue();
                            breakdown.put(crop, breakdown.getOrDefault(crop, 0) + count);
                            binPay += count * binRates.getOrDefault(crop, 3.0);
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : breakdown.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" Bins\n");
                    }
                    detailHarvestBreakdownText.setText(sb.length() > 0 ? sb.toString().trim() : "No bins logged for this shift.");

                    // Only update earnings UI if shift is completed (as per previous logic)
                    boolean isActive = shiftDoc.getBoolean("active") != null && shiftDoc.getBoolean("active");
                    if (!isActive && start != null && end != null) {
                        long diff = end.toDate().getTime() - start.toDate().getTime();
                        double hours = diff / (1000.0 * 60.0 * 60.0);
                        double hourlyPay = hours * hourlyRate;
                        double totalPay = hourlyPay + binPay;
                        updateSessionEarningsUI(totalPay, hourlyPay, binPay);
                    }
                });
    }

    @SuppressLint("DefaultLocale")
    private void updateSessionEarningsUI(double total, double hourly, double bin) {
        detailEstimatedEarnings.setText(String.format("$%.2f", total));
        detailEarningsBreakdown.setText(String.format("Hourly: $%.2f | Bins: $%.2f", hourly, bin));
    }
}
