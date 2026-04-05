package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ReviewActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_placeholder);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        TextView textView = findViewById(R.id.placeholder_text);
        textView.setText("Review and Adjust Harvest Logs");

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        
        loadUserData();

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

    private void loadUserData() {
        if (mAuth.getCurrentUser() == null) return;
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if ("worker".equals(role)) {
                            Menu menu = bottomNavigationView.getMenu();
                            menu.findItem(R.id.nav_dashboard).setVisible(false);
                            menu.findItem(R.id.nav_review).setVisible(false);
                            
                            // Redirect if unauthorized
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        }
                    }
                })
                .addOnFailureListener(e -> Log.e("ReviewActivity", "Error loading user data", e));
    }
}