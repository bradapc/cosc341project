package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class HarvestActivity extends AppCompatActivity {

    private Spinner cropSpinner;
    private TextView binCountText;
    private View harvestLoggingCard, noShiftCard;
    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String currentShiftId;
    private String userRole = "worker";
    private int binCount = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_harvest);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        cropSpinner = findViewById(R.id.cropSpinner);
        binCountText = findViewById(R.id.binCountText);
        harvestLoggingCard = findViewById(R.id.harvestLoggingCard);
        noShiftCard = findViewById(R.id.noShiftCard);
        Button plusButton = findViewById(R.id.plusButton);
        Button minusButton = findViewById(R.id.minusButton);
        Button saveHarvestButton = findViewById(R.id.saveHarvestButton);
        Button navClockInButton = findViewById(R.id.navClockInButton);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        String[] crops = {"Apples", "Pears", "Cherries", "Peaches", "Plums"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, crops);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        cropSpinner.setAdapter(adapter);

        plusButton.setOnClickListener(v -> {
            binCount++;
            binCountText.setText(String.valueOf(binCount));
        });

        minusButton.setOnClickListener(v -> {
            if (binCount > 1) {
                binCount--;
                binCountText.setText(String.valueOf(binCount));
            }
        });

        navClockInButton.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        loadUserData();
        checkActiveShift();

        saveHarvestButton.setOnClickListener(v -> saveHarvestLog());

        bottomNavigationView.setSelectedItemId(R.id.nav_harvest);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_work) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_harvest) {
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
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        userRole = documentSnapshot.getString("role");
                        updateUIBasedOnRole();
                    }
                })
                .addOnFailureListener(e -> Log.e("HarvestActivity", "Error loading user data", e));
    }

    private void updateUIBasedOnRole() {
        boolean isManager = "manager".equals(userRole);
        Menu menu = bottomNavigationView.getMenu();
        menu.findItem(R.id.nav_dashboard).setVisible(isManager);
        menu.findItem(R.id.nav_review).setVisible(isManager);

        if (isManager) {
            harvestLoggingCard.setVisibility(View.VISIBLE);
            noShiftCard.setVisibility(View.GONE);
        } else {
            // Worker logic handled by checkActiveShift
            updateWorkerUI();
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
                        currentShiftId = queryDocumentSnapshots.getDocuments().get(0).getId();
                    } else {
                        currentShiftId = null;
                    }
                    updateWorkerUI();
                });
    }

    private void updateWorkerUI() {
        if ("worker".equals(userRole)) {
            if (currentShiftId != null) {
                harvestLoggingCard.setVisibility(View.VISIBLE);
                noShiftCard.setVisibility(View.GONE);
            } else {
                harvestLoggingCard.setVisibility(View.GONE);
                noShiftCard.setVisibility(View.VISIBLE);
            }
        }
    }

    private void saveHarvestLog() {
        if (!"manager".equals(userRole) && currentShiftId == null) {
            Toast.makeText(this, "You must be clocked in to log harvest!", Toast.LENGTH_LONG).show();
            return;
        }

        String cropType = cropSpinner.getSelectedItem().toString();
        String userId = mAuth.getCurrentUser().getUid();

        Map<String, Object> log = new HashMap<>();
        log.put("userId", userId);
        log.put("shiftId", currentShiftId != null ? currentShiftId : "manager_log");
        log.put("cropType", cropType);
        log.put("binCount", binCount);
        log.put("timestamp", Timestamp.now());

        db.collection("harvest_logs")
                .add(log)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Harvest logged successfully!", Toast.LENGTH_SHORT).show();
                    binCount = 1;
                    binCountText.setText("1");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
