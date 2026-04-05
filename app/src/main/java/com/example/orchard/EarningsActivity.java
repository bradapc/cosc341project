package com.example.orchard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class EarningsActivity extends AppCompatActivity {

    private TextView estimatedEarningsText, earningsBreakdownText, totalBinsText;
    private TextView totalPayrollText, payrollBreakdownText, avgCostPerBinText, earningsTitle, statsCardTitle, binsLabelText;
    private View noActiveShiftEarningsText, workerEarningsCard, managerPayrollCard, efficiencyLayout;
    private Button viewShiftDetailsButton, managerReviewLogsButton;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private String currentShiftId;
    private Timestamp shiftStartTime;
    private double hourlyRate = 19.0;
    private String userRole = "worker";
    
    // Adjusted bin rates to be bonuses (max ~$7)
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
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_earnings);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize Views
        earningsTitle = findViewById(R.id.earningsTitle);
        workerEarningsCard = findViewById(R.id.workerEarningsCard);
        estimatedEarningsText = findViewById(R.id.estimatedEarningsText);
        earningsBreakdownText = findViewById(R.id.earningsBreakdownText);
        
        managerPayrollCard = findViewById(R.id.managerPayrollCard);
        totalPayrollText = findViewById(R.id.totalPayrollText);
        payrollBreakdownText = findViewById(R.id.payrollBreakdownText);
        
        statsCardTitle = findViewById(R.id.statsCardTitle);
        binsLabelText = findViewById(R.id.binsLabelText);
        totalBinsText = findViewById(R.id.totalBinsText);
        
        efficiencyLayout = findViewById(R.id.efficiencyLayout);
        avgCostPerBinText = findViewById(R.id.avgCostPerBinText);
        
        noActiveShiftEarningsText = findViewById(R.id.noActiveShiftEarningsText);
        viewShiftDetailsButton = findViewById(R.id.viewShiftDetailsButton);
        managerReviewLogsButton = findViewById(R.id.managerReviewLogsButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        viewShiftDetailsButton.setOnClickListener(v -> startActivity(new Intent(this, ShiftDetailsActivity.class)));
        managerReviewLogsButton.setOnClickListener(v -> startActivity(new Intent(this, ReviewActivity.class)));

        loadUserData();

        bottomNavigationView.setSelectedItemId(R.id.nav_earnings);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_work) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_harvest) {
                startActivity(new Intent(this, HarvestActivity.class));
                return true;
            } else if (id == R.id.nav_earnings) {
                return true;
            } else if (id == R.id.nav_dashboard) {
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_review) {
                startActivity(new Intent(this, ReviewActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRole = documentSnapshot.getString("role");
                        updateUIBasedOnRole();
                        
                        Double rate = documentSnapshot.getDouble("hourlyRate");
                        if (rate != null) hourlyRate = rate;
                    }
                })
                .addOnFailureListener(e -> Log.e("EarningsActivity", "Error loading user data", e));
    }

    private void updateUIBasedOnRole() {
        boolean isManager = "manager".equals(userRole);
        
        // Header and Titles
        earningsTitle.setText(isManager ? "FARM PAYROLL" : "EARNINGS");
        statsCardTitle.setText(isManager ? "FARM PRODUCTION" : "SHIFT PRODUCTION");
        binsLabelText.setText(isManager ? "Total Bins Today" : "Total Bins Harvested");
        
        // Cards
        workerEarningsCard.setVisibility(isManager ? View.GONE : View.VISIBLE);
        managerPayrollCard.setVisibility(isManager ? View.VISIBLE : View.GONE);
        efficiencyLayout.setVisibility(isManager ? View.VISIBLE : View.GONE);
        
        // Buttons
        viewShiftDetailsButton.setVisibility(isManager ? View.GONE : View.VISIBLE);
        managerReviewLogsButton.setVisibility(isManager ? View.VISIBLE : View.GONE);
        
        // Bottom Nav
        Menu menu = bottomNavigationView.getMenu();
        menu.findItem(R.id.nav_dashboard).setVisible(isManager);
        menu.findItem(R.id.nav_review).setVisible(isManager);

        if (isManager) {
            loadManagerPayrollData();
        } else {
            checkActiveShift();
        }
    }

    private void loadManagerPayrollData() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(cal.getTime());

        // 1. Fetch all harvest logs from today to calculate bonus pay
        db.collection("harvest_logs")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get()
                .addOnSuccessListener(logs -> {
                    double totalBonusPay = 0;
                    int totalBins = 0;
                    for (QueryDocumentSnapshot doc : logs) {
                        String crop = doc.getString("cropType");
                        Long bins = doc.getLong("binCount");
                        if (bins != null) {
                            int count = bins.intValue();
                            totalBins += count;
                            totalBonusPay += count * binRates.getOrDefault(crop, 3.0);
                        }
                    }
                    
                    final double finalBonusPay = totalBonusPay;
                    final int finalTotalBins = totalBins;

                    // 2. Fetch all shifts from today to calculate base wages
                    db.collection("shifts")
                            .whereGreaterThanOrEqualTo("startTime", startOfDay)
                            .get()
                            .addOnSuccessListener(shifts -> {
                                double totalBaseWages = 0;
                                for (QueryDocumentSnapshot doc : shifts) {
                                    Timestamp start = doc.getTimestamp("startTime");
                                    Timestamp end = doc.getTimestamp("endTime");
                                    if (start != null) {
                                        long endMillis = (end != null) ? end.toDate().getTime() : new Date().getTime();
                                        double hours = (endMillis - start.toDate().getTime()) / (1000.0 * 60.0 * 60.0);
                                        // Simple default for manager view prototype: use $19 for everyone
                                        totalBaseWages += hours * 19.0;
                                    }
                                }
                                
                                updateManagerUI(totalBaseWages, finalBonusPay, finalTotalBins);
                            });
                });
    }

    @SuppressLint("DefaultLocale")
    private void updateManagerUI(double base, double bonuses, int totalBins) {
        double totalPayroll = base + bonuses;
        totalPayrollText.setText(String.format("$%.2f", totalPayroll));
        payrollBreakdownText.setText(String.format("Base Wages: $%.2f | Bonuses: $%.2f", base, bonuses));
        totalBinsText.setText(totalBins + " Bins");
        
        if (totalBins > 0) {
            double costPerBin = totalPayroll / totalBins;
            avgCostPerBinText.setText(String.format("$%.2f", costPerBin));
        } else {
            avgCostPerBinText.setText("$0.00");
        }
    }

    private void checkActiveShift() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("shifts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        currentShiftId = doc.getId();
                        shiftStartTime = doc.getTimestamp("startTime");
                        noActiveShiftEarningsText.setVisibility(View.GONE);
                        calculateEarnings();
                    } else {
                        noActiveShiftEarningsText.setVisibility(View.VISIBLE);
                        loadLastShiftEarnings(userId);
                    }
                });
    }

    private void loadLastShiftEarnings(String userId) {
        db.collection("shifts")
                .whereEqualTo("userId", userId)
                .orderBy("startTime", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(query -> {
                    if (!query.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = query.getDocuments().get(0);
                        currentShiftId = doc.getId();
                        shiftStartTime = doc.getTimestamp("startTime");
                        Timestamp endTime = doc.getTimestamp("endTime");
                        calculateStaticEarnings(endTime);
                    }
                });
    }

    private void calculateEarnings() {
        if (currentShiftId == null || shiftStartTime == null) return;

        db.collection("harvest_logs")
                .whereEqualTo("shiftId", currentShiftId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double binPay = 0;
                    int totalBins = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String crop = doc.getString("cropType");
                        Long bins = doc.getLong("binCount");
                        if (bins != null) {
                            totalBins += bins.intValue();
                            double rate = binRates.getOrDefault(crop, 3.0);
                            binPay += bins.intValue() * rate;
                        }
                    }
                    
                    long diff = new Date().getTime() - shiftStartTime.toDate().getTime();
                    double hours = diff / (1000.0 * 60.0 * 60.0);
                    double hourlyPay = hours * hourlyRate;
                    double totalPay = hourlyPay + binPay;

                    updateEarningsUI(totalPay, hourlyPay, binPay, totalBins);
                });
    }

    private void calculateStaticEarnings(Timestamp endTime) {
        if (currentShiftId == null || shiftStartTime == null || endTime == null) return;

        db.collection("harvest_logs")
                .whereEqualTo("shiftId", currentShiftId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    double binPay = 0;
                    int totalBins = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String crop = doc.getString("cropType");
                        Long bins = doc.getLong("binCount");
                        if (bins != null) {
                            totalBins += bins.intValue();
                            double rate = binRates.getOrDefault(crop, 3.0);
                            binPay += bins.intValue() * rate;
                        }
                    }
                    
                    long diff = endTime.toDate().getTime() - shiftStartTime.toDate().getTime();
                    double hours = diff / (1000.0 * 60.0 * 60.0);
                    double hourlyPay = hours * hourlyRate;
                    double totalPay = hourlyPay + binPay;

                    updateEarningsUI(totalPay, hourlyPay, binPay, totalBins);
                });
    }

    @SuppressLint("DefaultLocale")
    private void updateEarningsUI(double total, double hourly, double bin, int bins) {
        estimatedEarningsText.setText(String.format("$%.2f", total));
        earningsBreakdownText.setText(String.format("Hourly: $%.2f | Bins: $%.2f", hourly, bin));
        totalBinsText.setText(bins + " Bins");
    }
}
