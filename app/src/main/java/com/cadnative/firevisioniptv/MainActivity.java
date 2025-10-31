package com.cadnative.firevisioniptv;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import com.cadnative.firevisioniptv.update.UpdateManager;


/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {
    private static final String TAG = "MainActivity";
    private UpdateManager updateManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseApp.initializeApp(this);

        // Initialize update manager
        updateManager = new UpdateManager(this);

        // Check for updates on startup (silently, no dialog if no update)
        updateManager.checkForUpdates(false);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.main_browse_fragment, new MainFragment())
                    .commitNow();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateManager != null) {
            updateManager.cleanup();
        }
    }
}