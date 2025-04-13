package com.example.bowling;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class profile extends AppCompatActivity {

    private TextView firstNameTextView;
    private TextView lastNameTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView emailTextView = findViewById(R.id.emailTextView);
        firstNameTextView = findViewById(R.id.firstNameTextView);
        lastNameTextView = findViewById(R.id.lastNameTextView);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            emailTextView.setText(email);

            boolean isGoogleSignIn = false;
            for (com.google.firebase.auth.UserInfo userInfo : user.getProviderData()) {
                if ("google.com".equals(userInfo.getProviderId())) {
                    isGoogleSignIn = true;
                    break;
                }
            }

            if (isGoogleSignIn) {
                firstNameTextView.setText("");
                lastNameTextView.setText("");
            } else {
                getUserProfileFromFirestore(user.getUid());
            }
        }

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_main) {
                Intent intent = new Intent(profile.this, MainActivity.class);
                startActivity(intent);
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(profile.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });
    }
    private void getUserProfileFromFirestore(String userId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(userId);

        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("keresztnev");
                String lastName = documentSnapshot.getString("vezeteknev");

                if (firstName != null) {
                    firstNameTextView.setText(firstName);
                }
                if (lastName != null) {
                    lastNameTextView.setText(lastName);
                }
            }
        }).addOnFailureListener(Throwable::printStackTrace);
    }

}
