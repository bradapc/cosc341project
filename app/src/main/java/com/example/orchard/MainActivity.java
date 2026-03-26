package com.example.orchard;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private TextView statusText;
    private Button clockInButton;
    private boolean isClockedIn = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        statusText = findViewById(R.id.statusText);
        clockInButton = findViewById(R.id.clockInButton);

        clockInButton.setOnClickListener(v -> {
            if (!isClockedIn) {
                isClockedIn = true;
                statusText.setText("Status: Clocked In");
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark));
                clockInButton.setText("Clock Out");

                Toast.makeText(MainActivity.this, "Clock In Clicked!", Toast.LENGTH_SHORT).show();


                Intent intent = new Intent(MainActivity.this, ClockInActivity.class);
                 startActivity(intent);
            } else {
                isClockedIn = false;
                statusText.setText("Status: Not clocked in");
                statusText.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                clockInButton.setText("Clock In");

                Toast.makeText(MainActivity.this, "Clock Out Clicked!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}