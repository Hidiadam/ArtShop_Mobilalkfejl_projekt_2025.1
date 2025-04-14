package com.example.artshop;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = RegisterActivity.class.getName();

    EditText userNameEditText; // Opcionális, most nem használjuk a regisztrációhoz
    EditText userEmailEditText;
    EditText passwordEditText;
    EditText passwordConfirmEditText;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Inicializálás
        userNameEditText = findViewById(R.id.editTextUserName); // Ha van ilyen meződ
        userEmailEditText = findViewById(R.id.editTextUserEmailReg); // Fontos az egyedi ID!
        passwordEditText = findViewById(R.id.editTextPasswordReg); // Fontos az egyedi ID!
        passwordConfirmEditText = findViewById(R.id.editTextPasswordConfirm); // Fontos az egyedi ID!

        mAuth = FirebaseAuth.getInstance();

        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("USER_EMAIL")) {
            String receivedEmail = intent.getStringExtra("USER_EMAIL");
            if (receivedEmail != null && !receivedEmail.isEmpty()) {
                userEmailEditText.setText(receivedEmail);
                Log.d(LOG_TAG, "Received email from MainActivity: " + receivedEmail);
                // Opcionálisan a fókuszt a jelszó mezőre helyezhetjük
                passwordEditText.requestFocus();
            }
        } else {
            Log.d(LOG_TAG, "No email passed from MainActivity.");
        }

        Log.i(LOG_TAG, "onCreate");
    }

    public void register(View view) {
        String email = userEmailEditText.getText().toString();
        String password = passwordEditText.getText().toString();
        String passwordConfirm = passwordConfirmEditText.getText().toString();
        // String userName = userNameEditText.getText().toString(); // Ha használnád

        if (email.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "Minden mező kitöltése kötelező!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(passwordConfirm)) {
            Log.e(LOG_TAG, "Passwords do not match.");
            Toast.makeText(this, "A két jelszó nem egyezik!", Toast.LENGTH_SHORT).show();
            passwordEditText.setText(""); // Jelszavak törlése hiba esetén
            passwordConfirmEditText.setText("");
            passwordEditText.requestFocus(); // Fókusz a jelszó mezőre
            return;
        }

        // Validációk (opcionális, de ajánlott):
        // - Email formátum ellenőrzése (Patterns.EMAIL_ADDRESS)
        // - Jelszó erősség ellenőrzése (pl. min. 6 karakter)
        if (password.length() < 6) {
            Toast.makeText(this, "A jelszónak legalább 6 karakter hosszúnak kell lennie!", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(LOG_TAG, "Registering user: " + email);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "User created successfully");
                            // Sikeres regisztráció után átirányítás
                            goToArtList();
                        } else {
                            Log.w(LOG_TAG, "User creation failed", task.getException());
                            Toast.makeText(RegisterActivity.this, "Sikertelen regisztráció: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void cancel(View view) {
        // Visszalépés az előző Activity-re (MainActivity)
        finish();
    }

    private void goToArtList() {
        Intent intent = new Intent(this, ArtListActivity.class);
        // intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Ne lehessen visszalépni a regisztrációhoz
        startActivity(intent);
        finishAffinity(); // Bezárja ezt és a mögötte lévő (pl. Login) Activity-ket is, hogy ne lehessen visszalépni
    }

    // Lifecycle logolás (jó gyakorlat)
    @Override protected void onStart() { super.onStart(); Log.i(LOG_TAG, "onStart"); }
    @Override protected void onStop() { super.onStop(); Log.i(LOG_TAG, "onStop"); }
    @Override protected void onDestroy() { super.onDestroy(); Log.i(LOG_TAG, "onDestroy"); }
    @Override protected void onPause() { super.onPause(); Log.i(LOG_TAG, "onPause"); }
    @Override protected void onResume() { super.onResume(); Log.i(LOG_TAG, "onResume"); }
    @Override protected void onRestart() { super.onRestart(); Log.i(LOG_TAG, "onRestart"); }
}