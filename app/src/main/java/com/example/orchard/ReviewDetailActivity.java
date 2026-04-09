package com.example.orchard;

import android.annotation.SuppressLint;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReviewDetailActivity extends AppCompatActivity {

    private TextView headerWorkerName, detailClockIn, detailClockOut, detailTotalTime;
    private TextView harvestBreakdownText, averageRateText;
    private TextView reviewEstimatedEarnings, reviewEarningsBreakdown;
    private String shiftId, workerId, workerName;
    private FirebaseFirestore db;
    private int currentTotalBins = 0;
    private long totalDurationMillis = 0;
    private Map<String, Integer> currentBreakdown = new HashMap<>();
    private Map<String, List<String>> cropDocIds = new HashMap<>();
    private Timestamp shiftStartTimestamp, shiftEndTimestamp;
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
        setContentView(R.layout.activity_review_detail);

        db = FirebaseFirestore.getInstance();
        shiftId = getIntent().getStringExtra("shiftId");
        workerId = getIntent().getStringExtra("workerId");
        workerName = getIntent().getStringExtra("workerName");

        headerWorkerName = findViewById(R.id.headerWorkerName);
        detailClockIn = findViewById(R.id.detailClockIn);
        detailClockOut = findViewById(R.id.detailClockOut);
        detailTotalTime = findViewById(R.id.detailTotalTime);
        harvestBreakdownText = findViewById(R.id.harvestBreakdownText);
        averageRateText = findViewById(R.id.averageRateText);
        
        reviewEstimatedEarnings = findViewById(R.id.reviewEstimatedEarnings);
        reviewEarningsBreakdown = findViewById(R.id.reviewEarningsBreakdown);

        headerWorkerName.setText(workerName);

        findViewById(R.id.backButton).setOnClickListener(v -> finish());
        findViewById(R.id.approveButton).setOnClickListener(v -> approveShift());
        findViewById(R.id.editLogButton).setOnClickListener(v -> showDetailedEditDialog());

        loadWorkerData();
        loadShiftData();
    }

    private void loadWorkerData() {
        db.collection("users").document(workerId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Double rate = doc.getDouble("hourlyRate");
                        if (rate != null) hourlyRate = rate;
                    }
                });
    }

    private void loadShiftData() {
        db.collection("shifts").document(shiftId).get()
                .addOnSuccessListener(doc -> {
                    shiftStartTimestamp = doc.getTimestamp("startTime");
                    shiftEndTimestamp = doc.getTimestamp("endTime");
                    SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

                    detailClockIn.setText("Clock In: " + (shiftStartTimestamp != null ? sdf.format(shiftStartTimestamp.toDate()) : "--"));
                    detailClockOut.setText("Clock Out: " + (shiftEndTimestamp != null ? sdf.format(shiftEndTimestamp.toDate()) : "Active"));

                    if (shiftStartTimestamp != null) {
                        long endMillis = (shiftEndTimestamp != null) ? shiftEndTimestamp.toDate().getTime() : new Date().getTime();
                        totalDurationMillis = endMillis - shiftStartTimestamp.toDate().getTime();
                        long hours = totalDurationMillis / (60 * 60 * 1000);
                        long minutes = (totalDurationMillis / (1000 * 60)) % 60;
                        detailTotalTime.setText("Total Time: " + hours + "h " + minutes + "m");
                    }
                    loadHarvestLogs();
                });
    }

    private void loadHarvestLogs() {
        db.collection("harvest_logs")
                .whereEqualTo("shiftId", shiftId)
                .get()
                .addOnSuccessListener(logs -> {
                    currentBreakdown.clear();
                    cropDocIds.clear();
                    currentTotalBins = 0;
                    double totalBinPay = 0;

                    for (QueryDocumentSnapshot doc : logs) {
                        String crop = doc.getString("cropType");
                        Long bins = doc.getLong("binCount");
                        if (bins != null && crop != null) {
                            int count = bins.intValue();
                            currentTotalBins += count;
                            currentBreakdown.put(crop, currentBreakdown.getOrDefault(crop, 0) + count);
                            totalBinPay += count * binRates.getOrDefault(crop, 3.0);
                            if (!cropDocIds.containsKey(crop)) cropDocIds.put(crop, new ArrayList<>());
                            cropDocIds.get(crop).add(doc.getId());
                        }
                    }

                    StringBuilder sb = new StringBuilder();
                    for (Map.Entry<String, Integer> entry : currentBreakdown.entrySet()) {
                        sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" Bins\n");
                    }
                    harvestBreakdownText.setText(sb.length() > 0 ? sb.toString().trim() : "No bins logged.");

                    updateRateAndEarningsUI(totalBinPay);
                });
    }

    @SuppressLint("DefaultLocale")
    private void updateRateAndEarningsUI(double totalBinPay) {
        if (totalDurationMillis > 0) {
            double hours = totalDurationMillis / (1000.0 * 60.0 * 60.0);
            double rate = currentTotalBins / hours;
            averageRateText.setText(String.format("Average Rate: %.1f Bins/hr", rate));
            
            double hourlyPay = hours * hourlyRate;
            double totalPay = hourlyPay + totalBinPay;
            
            reviewEstimatedEarnings.setText(String.format("$%.2f", totalPay));
            reviewEarningsBreakdown.setText(String.format("Hourly: $%.2f | Bins: $%.2f", hourlyPay, totalBinPay));
        }
    }

    private void approveShift() {
        db.collection("shifts").document(shiftId)
                .update("approved", true)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Shift Approved", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showDetailedEditDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_log, null);
        builder.setView(dialogView);

        EditText editClockIn = dialogView.findViewById(R.id.editClockInTime);
        EditText editClockOut = dialogView.findViewById(R.id.editClockOutTime);
        EditText editNotes = dialogView.findViewById(R.id.editNotes);
        LinearLayout fruitContainer = dialogView.findViewById(R.id.fruitBinsContainer);

        editClockIn.setFocusable(false);
        editClockOut.setFocusable(false);

        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        if (shiftStartTimestamp != null) editClockIn.setText(timeFormat.format(shiftStartTimestamp.toDate()));
        if (shiftEndTimestamp != null) editClockOut.setText(timeFormat.format(shiftEndTimestamp.toDate()));

        editClockIn.setOnClickListener(v -> showTimePicker(editClockIn));
        editClockOut.setOnClickListener(v -> showTimePicker(editClockOut));

        Map<String, EditText> fruitInputs = new HashMap<>();
        for (String fruit : currentBreakdown.keySet()) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setPadding(0, 16, 0, 16);
            
            TextView fruitLabel = new TextView(this);
            fruitLabel.setText(fruit);
            fruitLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            
            EditText countInput = new EditText(this);
            countInput.setHint("Bins");
            countInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
            countInput.setText(String.valueOf(currentBreakdown.get(fruit)));
            countInput.setLayoutParams(new LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.WRAP_CONTENT));
            
            rowLayout.addView(fruitLabel);
            rowLayout.addView(countInput);
            fruitContainer.addView(rowLayout);
            
            fruitInputs.put(fruit, countInput);
        }

        builder.setPositiveButton("Update & Approve", (dialog, which) -> {
            processDetailedUpdate(editClockIn.getText().toString(), editClockOut.getText().toString(), editNotes.getText().toString(), fruitInputs);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showTimePicker(EditText targetField) {
        Calendar mcurrentTime = Calendar.getInstance();
        int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
        int minute = mcurrentTime.get(Calendar.MINUTE);
        
        TimePickerDialog mTimePicker = new TimePickerDialog(this, (timePicker, selectedHour, selectedMinute) -> {
            targetField.setText(String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute));
        }, hour, minute, true);
        mTimePicker.setTitle("Select Time");
        mTimePicker.show();
    }

    private void processDetailedUpdate(String inStr, String outStr, String notes, Map<String, EditText> fruitInputs) {
        try {
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            
            if (shiftStartTimestamp != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(shiftStartTimestamp.toDate());
                Date newTime = timeFormat.parse(inStr);
                Calendar tCal = Calendar.getInstance();
                tCal.setTime(newTime);
                cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE));
                shiftStartTimestamp = new Timestamp(cal.getTime());
            }

            if (shiftEndTimestamp != null) {
                Calendar cal = Calendar.getInstance();
                cal.setTime(shiftEndTimestamp.toDate());
                Date newTime = timeFormat.parse(outStr);
                Calendar tCal = Calendar.getInstance();
                tCal.setTime(newTime);
                cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY));
                cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE));
                shiftEndTimestamp = new Timestamp(cal.getTime());
            }

            // Update harvest_logs for each edited crop
            for (Map.Entry<String, EditText> entry : fruitInputs.entrySet()) {
                String crop = entry.getKey();
                String inputStr = entry.getValue().getText().toString();
                int newCount = inputStr.isEmpty() ? 0 : Integer.parseInt(inputStr);
                List<String> docIds = cropDocIds.get(crop);
                if (docIds != null && !docIds.isEmpty()) {
                    // Update the first document with the new total
                    db.collection("harvest_logs").document(docIds.get(0))
                            .update("binCount", newCount);
                    // Delete any extra documents for this crop
                    for (int i = 1; i < docIds.size(); i++) {
                        db.collection("harvest_logs").document(docIds.get(i)).delete();
                    }
                }
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("startTime", shiftStartTimestamp);
            updates.put("endTime", shiftEndTimestamp);
            updates.put("managerNotes", notes);
            updates.put("approved", true);
            updates.put("verificationStatus", "Manager Verified");

            db.collection("shifts").document(shiftId)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Log Updated and Approved", Toast.LENGTH_SHORT).show();
                        finish();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Error processing update", Toast.LENGTH_SHORT).show();
        }
    }
}
