package com.example.artshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getName();
    private static final String PREF_KEY = MainActivity.class.getPackage().toString();

    EditText emailET;
    EditText passwordET;

    private SharedPreferences preferences;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        emailET = findViewById(R.id.editTextUserEmail);
        passwordET = findViewById(R.id.editTextPassword);

        preferences = getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();

        Log.i(LOG_TAG, "onCreate");
    }

    // Opcionális: Ha a felhasználó már be van jelentkezve, átirányítjuk
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if(currentUser != null){
            Log.i(LOG_TAG, "User already logged in, redirecting...");
            goToArtList();
        } else {
            Log.i(LOG_TAG, "No user logged in.");
        }
    }

    public void login(View view) {
        String email = emailET.getText().toString();
        String password = passwordET.getText().toString();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(MainActivity.this, "Email és jelszó megadása kötelező!", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "User logged in successfully");
                            goToArtList();
                        } else {
                            Log.w(LOG_TAG, "User log in failure", task.getException());
                            Toast.makeText(MainActivity.this, "Sikertelen bejelentkezés: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    private void goToArtList() {
        Intent intent = new Intent(this, ArtListActivity.class);
        startActivity(intent);
        finish();
    }

    public void register(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        String emailFromLogin = emailET.getText().toString().trim();

        // Adjuk át az emailt az Intent extra adataiként, ha nem üres
        if (!emailFromLogin.isEmpty()) {
            intent.putExtra("USER_EMAIL", emailFromLogin);
            Log.d(LOG_TAG, "Passing email to RegisterActivity: " + emailFromLogin);
        } else {
            Log.d(LOG_TAG, "Email field is empty, not passing to RegisterActivity.");
        }

        startActivity(intent);
    }


    // Mentjük az emailt, ha elhagyjuk az Activity-t
    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userEmail", emailET.getText().toString());
        // Jelszót biztonsági okokból általában nem mentünk SharedPreferences-be!
        editor.apply();
        Log.i(LOG_TAG, "onPause - Email saved");
    }

    // Visszatöltjük az emailt, ha visszatérünk
    @Override
    protected void onResume() {
        super.onResume();
        emailET.setText(preferences.getString("userEmail", ""));
        Log.i(LOG_TAG, "onResume - Email restored");
    }

    @Override
    protected void onStop() {
        super.onStop(); Log.i(LOG_TAG, "onStop"); }
    @Override
    protected void onDestroy() {
        super.onDestroy(); Log.i(LOG_TAG, "onDestroy"); }
    @Override
    protected void onRestart() {
        super.onRestart(); Log.i(LOG_TAG, "onRestart"); }
}