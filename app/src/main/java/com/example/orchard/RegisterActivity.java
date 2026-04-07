package com.example.orchard;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText nameEditText, emailEditText, passwordEditText, confirmPasswordEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        nameEditText = findViewById(R.id.registerNameEditText);
        emailEditText = findViewById(R.id.registerEmailEditText);
        passwordEditText = findViewById(R.id.registerPasswordEditText);
        confirmPasswordEditText = findViewById(R.id.registerConfirmPasswordEditText);
        Button registerButton = findViewById(R.id.registerButton);
        TextView backToLogin = findViewById(R.id.backToLoginTextView);

        registerButton.setOnClickListener(v -> {
            String name = nameEditText.getText().toString().trim();
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();
            String confirmPassword = confirmPasswordEditText.getText().toString().trim();

            if (name.isEmpty()) {
                nameEditText.setError("Full name is required");
                return;
            }

            if (email.isEmpty()) {
                emailEditText.setError("Email is required");
                return;
            }

            if (password.length() < 6) {
                passwordEditText.setError("Password must be at least 6 characters");
                return;
            }

            if (!password.equals(confirmPassword)) {
                confirmPasswordEditText.setError("Passwords do not match");
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String userId = mAuth.getCurrentUser().getUid();
                        saveUserToFirestore(userId, name, email, "worker");
                    } else {
                        Toast.makeText(RegisterActivity.this, "Registration failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        backToLogin.setOnClickListener(v -> finish());
    }

    private void saveUserToFirestore(String userId, String name, String email, String role) {
        Map<String, Object> user = new HashMap<>();
        user.put("name", name);
        user.put("email", email);
        user.put("role", role);
        // Added a default zone assignment upon registration
        user.put("assignedZone", "Apple Orchard - Row B");

        db.collection("users").document(userId)
            .set(user)
            .addOnSuccessListener(aVoid -> {
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finish();
            })
            .addOnFailureListener(e -> {
                Toast.makeText(RegisterActivity.this, "Error saving user data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
}
