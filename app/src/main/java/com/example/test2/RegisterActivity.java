package com.example.test2;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText emailField = findViewById(R.id.editTextRegEmail);
        EditText passwordField = findViewById(R.id.editTextRegPassword);
        Button submitButton = findViewById(R.id.buttonSubmitRegister);
        Button addBiometricButton = findViewById(R.id.buttonAddBiometric);

        submitButton.setOnClickListener(v -> {
            String email = emailField.getText().toString().trim();
            String password = passwordField.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                SharedPreferences securePrefs = getSecurePrefs();
                SharedPreferences.Editor editor = securePrefs.edit();
                editor.putString("email", email);
                editor.putString("password", password);
                editor.putBoolean(email + "_biometric_enabled", true); // Biometric tied to email
                editor.apply();

                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show();
                finish();
            } catch (Exception e) {
                Toast.makeText(this, "Error saving credentials", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });

        addBiometricButton.setOnClickListener(v -> {
            BiometricManager biometricManager = BiometricManager.from(this);
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    != BiometricManager.BIOMETRIC_SUCCESS) {
                Intent enrollIntent = new Intent(Settings.ACTION_BIOMETRIC_ENROLL);
                enrollIntent.putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED,
                        BiometricManager.Authenticators.BIOMETRIC_STRONG);
                try {
                    startActivity(enrollIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to open biometric settings", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Biometric already enrolled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private SharedPreferences getSecurePrefs() throws Exception {
        MasterKey masterKey = new MasterKey.Builder(this)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();

        return EncryptedSharedPreferences.create(
                this,
                "secure_user_data",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        );
    }
}
