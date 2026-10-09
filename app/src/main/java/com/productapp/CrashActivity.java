package com.productapp;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class CrashActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crash);

        String crashLog = getIntent().getStringExtra("crash_log");
        TextView tv = findViewById(R.id.tvCrashLog);
        Button btnCopy = findViewById(R.id.btnCopyCrash);

        if (crashLog != null) {
            tv.setText(crashLog);
        } else {
            tv.setText("No error details available.");
        }

        btnCopy.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Crash Log", tv.getText().toString());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Crash details copied to clipboard!", Toast.LENGTH_SHORT).show();
        });
    }
}
