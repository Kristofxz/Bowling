package com.example.bowling;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.bumptech.glide.Glide;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import androidx.appcompat.app.AppCompatActivity;


import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class profile extends AppCompatActivity {

    private TextView firstNameTextView;
    private TextView lastNameTextView;
    private LinearLayout bookedSlotsLayout;
    private String userId;
    private FirebaseUser user;
    private ImageView profileImageView;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_PERMISSION_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        TextView emailTextView = findViewById(R.id.emailTextView);
        firstNameTextView = findViewById(R.id.firstNameTextView);
        lastNameTextView = findViewById(R.id.lastNameTextView);
        bookedSlotsLayout = findViewById(R.id.bookedSlotsLayout);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        user = FirebaseAuth.getInstance().getCurrentUser();

        if (user != null) {
            String email = user.getEmail();
            emailTextView.setText(email);
            userId = user.getUid();

            boolean isGoogleSignIn = false;
            for (com.google.firebase.auth.UserInfo userInfo : user.getProviderData()) {
                if ("google.com".equals(userInfo.getProviderId())) {
                    isGoogleSignIn = true;
                    break;
                }
            }

            if (isGoogleSignIn) {
                firstNameTextView.setText("Google-fiók");
                lastNameTextView.setText("");
            } else {
                getUserProfileFromFirestore(userId);
            }
        }

        profileImageView = findViewById(R.id.profileImageView);
        Button uploadImageButton = findViewById(R.id.uploadImageButton);

        uploadImageButton.setOnClickListener(v -> openFileChooser());

        loadProfileImage();

        Button takePhotoButton = findViewById(R.id.takePhotoButton);
        takePhotoButton.setOnClickListener(v -> {
            if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
            } else {
                openCamera();
            }
        });

        EditText newPasswordEditText = findViewById(R.id.newPasswordEditText);
        Button changePasswordButton = findViewById(R.id.changePasswordButton);

        changePasswordButton.setOnClickListener(v -> {
            String newPassword = newPasswordEditText.getText().toString().trim();

            if (TextUtils.isEmpty(newPassword)) {
                Toast.makeText(profile.this, "Add meg az új jelszót!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPassword.length() < 6) {
                Toast.makeText(profile.this, "A jelszónak legalább 6 karakter hosszúnak kell lennie!", Toast.LENGTH_SHORT).show();
                return;
            }


            user.updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(profile.this, "Jelszó sikeresen módosítva.", Toast.LENGTH_SHORT).show();
                            newPasswordEditText.setText("");
                        } else {
                            Toast.makeText(profile.this, "Hiba: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        });

        Button deleteProfileButton = findViewById(R.id.deleteProfileButton);

        deleteProfileButton.setOnClickListener(v -> {
            if (user == null) return;

            String uid = user.getUid();

            FirebaseFirestore.getInstance().collection("users").document(uid)
                    .delete()
                    .addOnSuccessListener(aVoid -> {
                        user.delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(profile.this, "Profil sikeresen törölve.", Toast.LENGTH_LONG).show();
                                    Intent intent = new Intent(profile.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish();
                                })
                                .addOnFailureListener(e -> Toast.makeText(profile.this, "Hiba a profil törlésekor: " + e.getMessage(), Toast.LENGTH_LONG).show());
                    })
                    .addOnFailureListener(e -> Toast.makeText(profile.this, "Hiba az adatbázisból való törléskor: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });

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
            }else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AdminAddSlotActivity.class));
                return true;
            }
            else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(profile.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }

            return false;
        });

        db.collection("users").document(userId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isAdmin = documentSnapshot.getBoolean("admin");
                        if (Boolean.TRUE.equals(isAdmin)) {
                            bottomNavigationView.getMenu().findItem(R.id.nav_add).setVisible(true);
                        }
                    }
                });
    } @Override
    protected void onResume() {
        super.onResume();
        if (user == null) {
            user = FirebaseAuth.getInstance().getCurrentUser();
        }

        if (user != null) {
            userId = user.getUid();
            if (bookedSlotsLayout != null) {
                loadBookedSlots(userId, bookedSlotsLayout);
            }
        }
    }
    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Válassz képet"), PICK_IMAGE_REQUEST);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImageToImgur(imageUri);
        } else if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {

            Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap imageBitmap = (Bitmap) extras.get("data");

                Uri tempUri = Utils.getImageUri(this, imageBitmap);
                uploadImageToImgur(tempUri);
            }
        }
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        }
    }


    private void uploadImageToImgur(Uri imageUri) {
        ImgurUploader.uploadImage(imageUri, this, new ImgurUploader.UploadCallback() {
            @Override
            public void onSuccess(String imageUrl) {
                runOnUiThread(() -> {
                    Toast.makeText(profile.this, "Feltöltés sikeres", Toast.LENGTH_SHORT).show();


                    FirebaseFirestore.getInstance().collection("users").document(userId)
                            .update("profilePictureUrl", imageUrl)
                            .addOnSuccessListener(aVoid -> {
                                Glide.with(profile.this).load(imageUrl).into(profileImageView);
                                Toast.makeText(profile.this, "Profilkép frissítve.", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(profile.this, "Firestore update hiba: " + e.getMessage(), Toast.LENGTH_LONG).show());
                });
            }

            @Override
            public void onFailure(String error) {
                runOnUiThread(() -> Toast.makeText(profile.this, "Feltöltés hiba: " + error, Toast.LENGTH_LONG).show());
            }
        });
    }


    private void loadProfileImage() {
        FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.contains("profilePictureUrl")) {
                        String imageUrl = doc.getString("profilePictureUrl");
                        if (imageUrl != null) {
                            Glide.with(this).load(imageUrl).into(profileImageView);
                        }
                    }
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

    private void loadBookedSlots(String userId, LinearLayout bookedSlotsLayout) {
        bookedSlotsLayout.removeAllViews();

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("slots")
                .whereEqualTo("userId", userId)
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String lane = doc.getString("lane");

                        String docId = doc.getId();

                        LinearLayout slotLayout = new LinearLayout(this);
                        slotLayout.setOrientation(LinearLayout.HORIZONTAL);

                        TextView tv = new TextView(this);
                        tv.setText("Lefoglalt időpont: " + date + " " + time + " | Pálya " + lane);
                        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        Button deleteBtn = new Button(this);
                        deleteBtn.setText("Törlés");

                        deleteBtn.setOnClickListener(v -> {
                            Map<String, Object> update = new HashMap<>();
                            update.put("userId", null);
                            update.put("reserved", false);

                            db.collection("slots").document(docId)
                                    .update(update)
                                    .addOnSuccessListener(unused -> {

                                        Animation slideOut = AnimationUtils.loadAnimation(this, R.anim.slide_out_left);
                                        slideOut.setAnimationListener(new Animation.AnimationListener() {
                                            @Override
                                            public void onAnimationStart(Animation animation) { }

                                            @Override
                                            public void onAnimationEnd(Animation animation) {

                                                bookedSlotsLayout.removeView(slotLayout);
                                                Toast.makeText(profile.this, "Időpont szabaddá téve", Toast.LENGTH_SHORT).show();
                                            }

                                            @Override
                                            public void onAnimationRepeat(Animation animation) { }
                                        });
                                        slotLayout.startAnimation(slideOut);
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Hiba történt: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });

                        slotLayout.addView(tv);
                        slotLayout.addView(deleteBtn);
                        bookedSlotsLayout.addView(slotLayout);
                    }
                });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Kamera engedély szükséges a fényképezéshez", Toast.LENGTH_SHORT).show();
            }
        }
    }



}
