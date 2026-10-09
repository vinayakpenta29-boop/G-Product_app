package com.productapp;

import android.app.Application;
import android.content.Intent;
import java.io.PrintWriter;
import java.io.StringWriter;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            String stackTrace = sw.toString();

            Intent intent = new Intent(getApplicationContext(), CrashActivity.class);
            intent.putExtra("crash_log", stackTrace);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        });
    }
}
