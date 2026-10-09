package com.productapp;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        String crashLog = getIntent().getStringExtra("crash_log");
        TextView tv = findViewById(R.id.tvCrashLog);
        if (crashLog != null) {
            tv.setText(crashLog);
        } else {
            tv.setText("No error details available.");
        }
    }
}
