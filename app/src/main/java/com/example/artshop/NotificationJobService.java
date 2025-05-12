package com.example.artshop;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class NotificationJobService extends JobService {

    private static final String LOG_TAG = NotificationJobService.class.getName();

    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d(LOG_TAG, "Job started!");

        // Értesítés küldése a NotificationHelper segítségével
        new NotificationHelper(getApplicationContext())
                .send("Új műalkotások várnak az ArtShopban!");

        Log.d(LOG_TAG, "Job finished successfully.");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(LOG_TAG, "Job stopped before completion.");
        return true;
    }
}