package com.example.orchard;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReviewActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReviewAdapter adapter;
    private List<ReviewAdapter.WorkerShiftSummary> workerShifts = new ArrayList<>();
    private BottomNavigationView bottomNavigationView;
    private Button dateFilterButton;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    
    private Calendar selectedDate = Calendar.getInstance();
    private SimpleDateFormat displayFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_review);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        dateFilterButton = findViewById(R.id.dateFilterButton);
        dateFilterButton.setOnClickListener(v -> showDatePicker());
        updateDateButtonText();

        recyclerView = findViewById(R.id.reviewRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(workerShifts, summary -> {
            Intent intent = new Intent(ReviewActivity.this, ReviewDetailActivity.class);
            intent.putExtra("shiftId", summary.getShiftId());
            intent.putExtra("workerId", summary.getWorkerId());
            intent.putExtra("workerName", summary.getWorkerName());
            startActivity(intent);
        });
        recyclerView.setAdapter(adapter);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        loadUserData();
        loadWorkerShifts();

        bottomNavigationView.setSelectedItemId(R.id.nav_review);
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
                startActivity(new Intent(this, DashboardActivity.class));
                return true;
            } else if (id == R.id.nav_review) {
                return true;
            }
            return false;
        });
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    selectedDate.set(Calendar.YEAR, year);
                    selectedDate.set(Calendar.MONTH, month);
                    selectedDate.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateDateButtonText();
                    loadWorkerShifts();
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
        db.collection("users").document(mAuth.getCurrentUser().getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        if (!"manager".equals(role)) {
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    }
                });
    }

    private void loadWorkerShifts() {
        // Calculate start and end of selected day
        Calendar cal = (Calendar) selectedDate.clone();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Timestamp startRange = new Timestamp(cal.getTime());

        cal.add(Calendar.DAY_OF_MONTH, 1);
        Timestamp endRange = new Timestamp(cal.getTime());

        db.collection("shifts")
                .whereGreaterThanOrEqualTo("startTime", startRange)
                .whereLessThan("startTime", endRange)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    workerShifts.clear();
                    if (queryDocumentSnapshots.isEmpty()) {
                        adapter.notifyDataSetChanged();
                        return;
                    }

                    for (QueryDocumentSnapshot shiftDoc : queryDocumentSnapshots) {
                        String userId = shiftDoc.getString("userId");
                        String shiftId = shiftDoc.getId();
                        Timestamp start = shiftDoc.getTimestamp("startTime");
                        Timestamp end = shiftDoc.getTimestamp("endTime");
                        boolean approved = shiftDoc.getBoolean("approved") != null && shiftDoc.getBoolean("approved");

                        String duration = calculateDuration(start, end);

                        db.collection("users").document(userId).get()
                                .addOnSuccessListener(userDoc -> {
                                    String name = userDoc.getString("name");
                                    
                                    db.collection("harvest_logs")
                                            .whereEqualTo("shiftId", shiftId)
                                            .get()
                                            .addOnSuccessListener(logs -> {
                                                int bins = 0;
                                                for (QueryDocumentSnapshot log : logs) {
                                                    Long b = log.getLong("binCount");
                                                    if (b != null) bins += b.intValue();
                                                }
                                                workerShifts.add(new ReviewAdapter.WorkerShiftSummary(userId, shiftId, name, duration, bins, approved));
                                                adapter.notifyDataSetChanged();
                                            });
                                });
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private String calculateDuration(Timestamp start, Timestamp end) {
        if (start == null) return "0h 0m";
        long endMillis = (end != null) ? end.toDate().getTime() : new Date().getTime();
        long diff = endMillis - start.toDate().getTime();
        long hours = diff / (60 * 60 * 1000);
        long minutes = (diff / (1000 * 60)) % 60;
        return hours + "h " + minutes + "m";
    }
}
