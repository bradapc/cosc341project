package com.example.orchard;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.widget.Button;
import android.widget.LinearLayout;
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
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DashboardActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private TextView managerGreetingText, dashTotalBinsText, dashActiveWorkersText;
    private LinearLayout leaderboardContainer;
    private Button dateFilterButton;

    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_dashboard);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        managerGreetingText = findViewById(R.id.managerGreetingText);
        dashTotalBinsText = findViewById(R.id.dashTotalBinsText);
        dashActiveWorkersText = findViewById(R.id.dashActiveWorkersText);
        leaderboardContainer = findViewById(R.id.leaderboardContainer);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        dateFilterButton = findViewById(R.id.dateFilterButton);

        if (dateFilterButton != null) {
            dateFilterButton.setOnClickListener(v -> showDatePicker());
            updateDateButtonText();
        }

        loadUserData();

        // bottom navigation
        bottomNavigationView.setSelectedItemId(R.id.nav_dashboard);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_work) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_harvest) {
                startActivity(new Intent(this, HarvestActivity.class));
                return true;
            } else if (id == R.id.nav_earnings) {
                startActivity(new Intent(this, EarningsActivity.class));
                return true;
            } else if (id == R.id.nav_dashboard) {
                return true;
            } else if (id == R.id.nav_review) {
                startActivity(new Intent(this, ReviewActivity.class));
                return true;
            }
            return false;
        });

        Button navHarvestProgressBtn = findViewById(R.id.navHarvestProgressBtn);
        Button navZoneDistBtn = findViewById(R.id.navZoneDistBtn);

        if(navHarvestProgressBtn != null) {
            navHarvestProgressBtn.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, HarvestProgressActivity.class));
            });
        }

        if(navZoneDistBtn != null) {
            navZoneDistBtn.setOnClickListener(v -> {
                startActivity(new Intent(DashboardActivity.this, ZoneDistributionActivity.class));
            });
        }
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButtonText();
                    fetchDashboardMetrics();
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void updateDateButtonText() {
        Calendar today = Calendar.getInstance();
        if (selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
            dateFilterButton.setText("Today");
        } else {
            dateFilterButton.setText(displayFormat.format(selectedDate.getTime()));
        }
    }

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        String name = documentSnapshot.getString("name");

                        if ("worker".equals(role)) {
                            // if a worker somehow gets here, hide manager tabs and redirect
                            Menu menu = bottomNavigationView.getMenu();
                            menu.findItem(R.id.nav_dashboard).setVisible(false);
                            menu.findItem(R.id.nav_review).setVisible(false);
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        } else {
                            // setup manager dashboard
                            if (name != null) {
                                String firstName = name.contains(" ") ? name.split(" ")[0] : name;
                                managerGreetingText.setText("Welcome Back, " + firstName);
                            }
                            fetchDashboardMetrics();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("DashboardActivity", "Error loading user data", e));
    }

    private void fetchDashboardMetrics() {
        Calendar cal = (Calendar) selectedDate.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startOfDay = new Timestamp(cal.getTime());

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp endOfDay = new Timestamp(cal.getTime());

        Calendar today = Calendar.getInstance();
        boolean isToday = selectedDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                selectedDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR);

        // active worker count/shift count
        if (isToday) {
            db.collection("shifts")
                    .whereEqualTo("active", true)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        dashActiveWorkersText.setText(queryDocumentSnapshots.size() + " Crew");
                    });
        } else {
            db.collection("shifts")
                    .whereGreaterThanOrEqualTo("startTime", startOfDay)
                    .whereLessThan("startTime", endOfDay)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        dashActiveWorkersText.setText(queryDocumentSnapshots.size() + " Shifts");
                    });
        }

        // harvest logic for selected date
        db.collection("harvest_logs")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int totalBinsToday = 0;
                    Map<String, Integer> userBinCounts = new HashMap<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Long bins = doc.getLong("binCount");
                        String wId = doc.getString("userId");

                        if (bins != null) {
                            int count = bins.intValue();
                            totalBinsToday += count;

                            if (wId != null) {
                                userBinCounts.put(wId, userBinCounts.getOrDefault(wId, 0) + count);
                            }
                        }
                    }

                    dashTotalBinsText.setText(totalBinsToday + " Bins");
                    generateLeaderboard(userBinCounts);
                });
    }

    private void generateLeaderboard(Map<String, Integer> userBinCounts) {
        if (userBinCounts.isEmpty()) {
            leaderboardContainer.removeAllViews();
            TextView emptyText = new TextView(this);
            emptyText.setText("No harvest logs recorded for this date.");
            emptyText.setTextColor(Color.parseColor("#757575"));
            emptyText.setPadding(16, 16, 16, 16);
            leaderboardContainer.addView(emptyText);
            return;
        }

        // sort by highest bins
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(userBinCounts.entrySet());
        sortedEntries.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        leaderboardContainer.removeAllViews();

        // user names to map to the IDs
        db.collection("users").get().addOnSuccessListener(usersSnap -> {
            Map<String, String> userNames = new HashMap<>();
            for (DocumentSnapshot doc : usersSnap) {
                userNames.put(doc.getId(), doc.getString("name"));
            }

            // display top 4 workers
            int rank = 1;
            for (Map.Entry<String, Integer> entry : sortedEntries) {
                if (rank > 4) break;

                String wId = entry.getKey();
                int bins = entry.getValue();
                String name = userNames.getOrDefault(wId, "Unknown Worker");

                addLeaderboardRow(rank, name, bins);
                rank++;
            }
        });
    }

    private void addLeaderboardRow(int rank, String name, int bins) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 24, 0, 24);
        row.setGravity(Gravity.CENTER_VERTICAL);

        if (rank % 2 == 0) {
            row.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        // rank
        TextView rankText = new TextView(this);
        rankText.setText("0" + rank);
        rankText.setTextSize(18f);
        rankText.setTypeface(null, Typeface.BOLD);
        rankText.setTextColor(Color.parseColor("#212121"));
        rankText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // name
        TextView nameText = new TextView(this);
        nameText.setText(name);
        nameText.setTextSize(16f);
        nameText.setTextColor(Color.parseColor("#757575"));
        nameText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 3f));

        // bins
        TextView binsText = new TextView(this);
        binsText.setText(bins + " Bins");
        binsText.setTextSize(16f);
        binsText.setTypeface(null, Typeface.BOLD);
        binsText.setTextColor(Color.parseColor("#212121"));
        binsText.setGravity(Gravity.END);
        binsText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        row.addView(rankText);
        row.addView(nameText);
        row.addView(binsText);

        leaderboardContainer.addView(row);
    }
}