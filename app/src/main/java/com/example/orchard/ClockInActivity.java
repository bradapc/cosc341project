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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockInActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clock_in);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

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
        loadUserName(workerNameText);

        Button confirmClockInButton = findViewById(R.id.confirmClockInButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        cancelButton.setOnClickListener(v -> finish());

        confirmClockInButton.setOnClickListener(v -> {
            clockInUser();
        });
    }

    private void loadUserName(TextView textView) {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String name = documentSnapshot.getString("name");
                        textView.setText(name != null ? name : "Unknown");
                    }
                })
                .addOnFailureListener(e -> Log.e("ClockInActivity", "Error loading name", e));
    }

    private void clockInUser() {
        if (mAuth.getCurrentUser() == null) return;

        String userId = mAuth.getCurrentUser().getUid();
        // Updated to match the Shift constructor (userId, startTime, zone)
        Shift newShift = new Shift(userId, Timestamp.now(), "Apple Orchard - Row B");

        db.collection("shifts")
            .add(newShift)
            .addOnSuccessListener(documentReference -> {
                Intent intent = new Intent(ClockInActivity.this, ClockInSuccessActivity.class);
                startActivity(intent);
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(this, "Error clocking in: " + e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }
}
