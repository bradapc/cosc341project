package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ClockInActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_clock_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Grab the current time dynamically so the prototype feels real
        TextView timeText = findViewById(R.id.timeText);
        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(new Date());
        timeText.setText("Time: " + currentTime);

        Button confirmClockInButton = findViewById(R.id.confirmClockInButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Cancel fulfills your "Efficiency" goal by requiring only 1 tap to return
        cancelButton.setOnClickListener(v -> finish());

        // Confirm moves to the Success Screen
        confirmClockInButton.setOnClickListener(v -> {
            Intent intent = new Intent(ClockInActivity.this, ClockInSuccessActivity.class);
            startActivity(intent);
        });
    }
}