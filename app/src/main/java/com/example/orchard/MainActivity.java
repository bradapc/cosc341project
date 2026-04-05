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

import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private TextView statusText, shiftDurationText, binsHarvestedText, greetingText;
    private Button clockInButton, mainLogHarvestButton;
    private View activeShiftCard;
    private BottomNavigationView bottomNavigationView;
    
    private boolean isClockedIn = false;
    private String currentShiftId = null;
    private Timestamp shiftStartTime = null;
    
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private Handler timerHandler = new Handler();
    private Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            updateDuration();
            timerHandler.postDelayed(this, 60000); // Update every minute
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

        statusText = findViewById(R.id.statusText);
        clockInButton = findViewById(R.id.clockInButton);
        mainLogHarvestButton = findViewById(R.id.mainLogHarvestButton);
        greetingText = findViewById(R.id.greetingText);
        activeShiftCard = findViewById(R.id.activeShiftCard);
        shiftDurationText = findViewById(R.id.shiftDurationText);
        binsHarvestedText = findViewById(R.id.binsHarvestedText);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        ImageButton profileButton = findViewById(R.id.profileButton);

        profileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, ProfileActivity.class));
        });

        mainLogHarvestButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, HarvestActivity.class));
        });
        
        loadUserData();
        checkActiveShift();

        clockInButton.setOnClickListener(v -> {
            if (!isClockedIn) {
                Intent intent = new Intent(MainActivity.this, ClockInActivity.class);
                startActivity(intent);
            } else {
                Intent intent = new Intent(MainActivity.this, ClockOutActivity.class);
                intent.putExtra("shiftId", currentShiftId);
                startActivity(intent);
            }
        });

        bottomNavigationView.setSelectedItemId(R.id.nav_work);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_work) {
                return true;
            } else if (id == R.id.nav_harvest) {
                startActivity(new Intent(this, HarvestActivity.class));
                return true;
            } else if (id == R.id.nav_earnings) {
                startActivity(new Intent(this, EarningsActivity.class));
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
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        String role = documentSnapshot.getString("role");
                        if (name != null && !name.isEmpty()) {
                            greetingText.setText("Welcome, " + name);
                        }
                        
                        if ("worker".equals(role)) {
                            Menu menu = bottomNavigationView.getMenu();
                            menu.findItem(R.id.nav_dashboard).setVisible(false);
                            menu.findItem(R.id.nav_review).setVisible(false);
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("MainActivity", "Error loading user data", e));
    }

    private void checkActiveShift() {
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

    private void startTimer() {
        timerHandler.post(timerRunnable);
    }

    private void stopTimer() {
        timerHandler.removeCallbacks(timerRunnable);
    }

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
