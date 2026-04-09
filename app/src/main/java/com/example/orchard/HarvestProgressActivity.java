package com.example.orchard;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Locale;

public class HarvestProgressActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private LinearLayout harvestLogContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_harvest_progress);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        db = FirebaseFirestore.getInstance();
        harvestLogContainer = findViewById(R.id.harvestLogContainer);

        // spinners
        Spinner blockSpinner = findViewById(R.id.blockSpinner);
        String[] blocks = {"All Orchard Blocks", "Block A", "Block B", "Block C"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, blocks);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        blockSpinner.setAdapter(adapter);

        // buttons
        findViewById(R.id.homeButton).setOnClickListener(v -> finish());

        // quota for prototype
        ProgressBar quotaProgressBar = findViewById(R.id.quotaProgressBar);
        TextView quotaText = findViewById(R.id.quotaText);
        quotaProgressBar.setProgress(75);
        quotaText.setText("75%");

        loadHarvestLogs();
    }

    private void loadHarvestLogs() {
        // recent harvest logs, sorted by newest first
        db.collection("harvest_logs")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    harvestLogContainer.removeAllViews();

                    if (queryDocumentSnapshots.isEmpty()) {
                        TextView emptyText = new TextView(this);
                        emptyText.setText("No harvest logs found.");
                        emptyText.setPadding(16, 16, 16, 16);
                        emptyText.setGravity(Gravity.CENTER);
                        harvestLogContainer.addView(emptyText);
                        return;
                    }

                    SimpleDateFormat sdf = new SimpleDateFormat("MMM\ndd,\nyyyy", Locale.getDefault());

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Timestamp timestamp = doc.getTimestamp("timestamp");
                        String dateStr = (timestamp != null) ? sdf.format(timestamp.toDate()) : "--";

                        String cropType = doc.getString("cropType");
                        Long bins = doc.getLong("binCount");
                        String binStr = (bins != null) ? String.valueOf(bins) : "0";

                        String block = doc.getString("block");
                        if (block == null) block = "A4-North";

                        addTableRow(dateStr, block, cropType, binStr);
                    }
                });
    }

    private void addTableRow(String date, String block, String crop, String bins) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(16, 24, 16, 24);
        row.setGravity(Gravity.CENTER_VERTICAL);

        // date
        TextView dateText = new TextView(this);
        dateText.setText(date);
        dateText.setTextSize(12f);
        dateText.setTextColor(Color.parseColor("#757575"));
        dateText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));

        // block
        TextView blockText = new TextView(this);
        blockText.setText(block);
        blockText.setTextSize(12f);
        blockText.setTypeface(null, Typeface.BOLD);
        blockText.setTextColor(Color.parseColor("#212121"));
        blockText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // crop
        TextView cropText = new TextView(this);
        cropText.setText(crop);
        cropText.setTextSize(12f);
        cropText.setTextColor(Color.parseColor("#757575"));
        cropText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f));

        // bins
        TextView binsText = new TextView(this);
        binsText.setText(bins);
        binsText.setTextSize(12f);
        binsText.setTypeface(null, Typeface.BOLD);
        binsText.setTextColor(Color.parseColor("#212121"));
        binsText.setGravity(Gravity.END);
        binsText.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(dateText);
        row.addView(blockText);
        row.addView(cropText);
        row.addView(binsText);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1));
        divider.setBackgroundColor(Color.parseColor("#EEEEEE"));

        harvestLogContainer.addView(row);
        harvestLogContainer.addView(divider);
    }
}