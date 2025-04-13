package com.example.bowling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class register extends AppCompatActivity {

    private EditText emailEt, passEt, vnevEt, knevEt;
    private SwitchCompat adminSwitch;
    private Button regBtn;
    private Button backBtn;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailEt = findViewById(R.id.reg_email);
        passEt = findViewById(R.id.reg_jelszo);
        vnevEt = findViewById(R.id.reg_vnev);
        knevEt = findViewById(R.id.reg_knev);
        adminSwitch = findViewById(R.id.reg_admin);
        regBtn = findViewById(R.id.reg_btn);
        backBtn = findViewById(R.id.back_btn);

        regBtn.setOnClickListener(v -> {
            String email = emailEt.getText().toString().trim();
            String pass = passEt.getText().toString().trim();
            String vnev = vnevEt.getText().toString().trim();
            String knev = knevEt.getText().toString().trim();
            boolean isAdmin = adminSwitch.isChecked();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Email és jelszó szükséges", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.createUserWithEmailAndPassword(email, pass)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("email", email);
                                userData.put("vezeteknev", vnev);
                                userData.put("keresztnev", knev);
                                userData.put("admin", isAdmin);

                                db.collection("users").document(user.getUid())
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(this, "Sikeres regisztráció!", Toast.LENGTH_SHORT).show();
                                            Intent intent = new Intent(register.this, mainPage.class);
                                            startActivity(intent);
                                            overridePendingTransition(R.anim.slide_in_r, R.anim.slide_out_l);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(this, "Firestore hiba: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            }
                        } else {
                            Toast.makeText(this, "Regisztráció sikertelen: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });
        backBtn.setOnClickListener(view -> {
            Intent intent = new Intent(register.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();

        });
    }
}
