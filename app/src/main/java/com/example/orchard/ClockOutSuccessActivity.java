package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockOutSuccessActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clock_out_success);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            int padding = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
            v.setPadding(systemBars.left + padding, systemBars.top + padding, systemBars.right + padding, systemBars.bottom + padding);
            return insets;
        });

        TextView successTimeText = findViewById(R.id.successTimeText);
        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        successTimeText.setText("Time: " + currentTime);

        TextView workerNameText = findViewById(R.id.workerNameText);
        loadUserName(workerNameText);

        // Receive the shift details from the intent
        String duration = getIntent().getStringExtra("duration");
        int totalBins = getIntent().getIntExtra("totalBins", 0);

        TextView shiftDurationText = findViewById(R.id.shiftDurationText);
        TextView totalBinsText = findViewById(R.id.totalBinsText);

        if (duration != null) {
            shiftDurationText.setText("Duration: " + duration);
        }
        totalBinsText.setText("Total Bins: " + totalBins);

        Button returnDashboardButton = findViewById(R.id.returnDashboardButton);
        returnDashboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClockOutSuccessActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });
    }

    private void loadUserName(TextView textView) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        textView.setText("Worker: " + (name != null ? name : "Unknown"));
                    }
                })
                .addOnFailureListener(e -> Log.e("ClockOutSuccess", "Error loading name", e));
    }
}
