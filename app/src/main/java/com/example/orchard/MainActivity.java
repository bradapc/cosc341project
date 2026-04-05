package com.example.orchard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView statusText, shiftDurationText, binsHarvestedText, greetingText;
    private TextView totalFarmBinsText, activeWorkersCountText, cropBreakdownText;
    private Button clockInButton, mainLogHarvestButton;
    private View activeShiftCard, workerStatusCard, managerOverviewCard, managerQuickActions, managerCropsCard, scheduleCard;
    private BottomNavigationView bottomNavigationView;
    
    private boolean isClockedIn = false;
    private String currentShiftId = null;
    private Timestamp shiftStartTime = null;
    private String userRole = "worker";
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateDuration();
            timerHandler.postDelayed(this, 60000);
        }
    };

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        // Initialize Views
        statusText = findViewById(R.id.statusText);
        clockInButton = findViewById(R.id.clockInButton);
        mainLogHarvestButton = findViewById(R.id.mainLogHarvestButton);
        greetingText = findViewById(R.id.greetingText);
        activeShiftCard = findViewById(R.id.activeShiftCard);
        workerStatusCard = findViewById(R.id.workerStatusCard);
        managerOverviewCard = findViewById(R.id.managerOverviewCard);
        managerQuickActions = findViewById(R.id.managerQuickActions);
        managerCropsCard = findViewById(R.id.managerCropsCard);
        scheduleCard = findViewById(R.id.scheduleCard);
        
        totalFarmBinsText = findViewById(R.id.totalFarmBinsText);
        activeWorkersCountText = findViewById(R.id.activeWorkersCountText);
        cropBreakdownText = findViewById(R.id.cropBreakdownText);
        
        shiftDurationText = findViewById(R.id.shiftDurationText);
        binsHarvestedText = findViewById(R.id.binsHarvestedText);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        ImageButton profileButton = findViewById(R.id.profileButton);

        profileButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        mainLogHarvestButton.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, HarvestActivity.class)));
        
        findViewById(R.id.mainDashboardButton).setOnClickListener(v -> startActivity(new Intent(this, DashboardActivity.class)));
        findViewById(R.id.mainReviewButton).setOnClickListener(v -> startActivity(new Intent(this, ReviewActivity.class)));

        loadUserData();
        checkActiveShift();

        clockInButton.setOnClickListener(v -> {
            if (!isClockedIn) {
                startActivity(new Intent(MainActivity.this, ClockInActivity.class));
            } else {
                Intent intent = new Intent(MainActivity.this, ClockOutActivity.class);
                intent.putExtra("shiftId", currentShiftId);
                startActivity(intent);
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_work);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_work) return true;
            if (id == R.id.nav_harvest) { startActivity(new Intent(this, HarvestActivity.class)); return true; }
            if (id == R.id.nav_earnings) { startActivity(new Intent(this, EarningsActivity.class)); return true; }
            if (id == R.id.nav_dashboard) { startActivity(new Intent(this, DashboardActivity.class)); return true; }
            if (id == R.id.nav_review) { startActivity(new Intent(this, ReviewActivity.class)); return true; }
            return false;
        });
    }

    private void loadUserData() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        userRole = documentSnapshot.getString("role");
                        if (name != null && !name.isEmpty()) greetingText.setText("Welcome, " + name);
                        
                        updateRoleUI();
                    }
                });
    }

    private void updateRoleUI() {
        boolean isManager = "manager".equals(userRole);
        workerStatusCard.setVisibility(isManager ? View.GONE : View.VISIBLE);
        managerOverviewCard.setVisibility(isManager ? View.VISIBLE : View.GONE);
        managerQuickActions.setVisibility(isManager ? View.VISIBLE : View.GONE);
        managerCropsCard.setVisibility(isManager ? View.VISIBLE : View.GONE);
        scheduleCard.setVisibility(isManager ? View.GONE : View.VISIBLE);
        
        Menu menu = bottomNavigationView.getMenu();
        menu.findItem(R.id.nav_dashboard).setVisible(isManager);
        menu.findItem(R.id.nav_review).setVisible(isManager);

        if (isManager) {
            loadManagerOverview();
        }
    }

    private void loadManagerOverview() {
        // Active Workers Count
        db.collection("shifts")
                .whereEqualTo("active", true)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    activeWorkersCountText.setText(String.valueOf(queryDocumentSnapshots.size()));
                });

        // Farm-wide Bins Today
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(cal.getTime());

        db.collection("harvest_logs")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int total = 0;
                    Map<String, Integer> cropTotals = new HashMap<>();
                    
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long bins = doc.getLong("binCount");
                        String crop = doc.getString("cropType");
                        if (bins != null) {
                            int count = bins.intValue();
                            total += count;
                            if (crop != null) {
                                cropTotals.put(crop, cropTotals.getOrDefault(crop, 0) + count);
                            }
                        }
                    }
                    totalFarmBinsText.setText(String.valueOf(total));
                    
                    if (cropTotals.isEmpty()) {
                        cropBreakdownText.setText("No harvest logs yet today.");
                    } else {
                        StringBuilder sb = new StringBuilder();
                        for (Map.Entry<String, Integer> entry : cropTotals.entrySet()) {
                            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append(" Bins\n");
                        }
                        cropBreakdownText.setText(sb.toString().trim());
                    }
                });
    }

    private void checkActiveShift() {
        if ("manager".equals(userRole)) return;
        
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("shifts")
                .whereEqualTo("userId", userId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        DocumentSnapshot doc = queryDocumentSnapshots.getDocuments().get(0);
                        currentShiftId = doc.getId();
                        shiftStartTime = doc.getTimestamp("startTime");
                        updateUI(true);
                        loadHarvestStats();
                        startTimer();
                    } else {
                        updateUI(false);
                        stopTimer();
                    }
                });
    }

    private void loadHarvestStats() {
        if (currentShiftId == null) return;
        db.collection("harvest_logs")
                .whereEqualTo("shiftId", currentShiftId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBins = 0;
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long bins = doc.getLong("binCount");
                        if (bins != null) totalBins += bins.intValue();
                    }
                    binsHarvestedText.setText(totalBins + " Bins");
                });
    }

    private void updateDuration() {
        if (shiftStartTime == null) return;
        long diff = new Date().getTime() - shiftStartTime.toDate().getTime();
        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff / (60 * 1000)) % 60;
        shiftDurationText.setText(hours + "h " + minutes + "m");
    }

    private void startTimer() { timerHandler.post(timerRunnable); }
    private void stopTimer() { timerHandler.removeCallbacks(timerRunnable); }

    private void updateUI(boolean clockedIn) {
        this.isClockedIn = clockedIn;
        if (clockedIn) {
            statusText.setText("Status: Clocked In");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
            clockInButton.setText("Clock Out");
            activeShiftCard.setVisibility(View.VISIBLE);
            mainLogHarvestButton.setVisibility(View.VISIBLE);
        } else {
            statusText.setText("Status: Not clocked in");
            statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
            clockInButton.setText("Clock In");
            activeShiftCard.setVisibility(View.GONE);
            mainLogHarvestButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mAuth.getCurrentUser() != null) {
            loadUserData();
            checkActiveShift();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopTimer();
    }
}
