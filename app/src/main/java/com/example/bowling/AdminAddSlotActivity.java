package com.example.bowling;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAddSlotActivity extends AppCompatActivity {

    private EditText dateInput, timeInput, laneInput;
    private Button addSlotButton;
    private LinearLayout slotsLayout;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_slot);

        dateInput = findViewById(R.id.dateInput);
        timeInput = findViewById(R.id.timeInput);
        laneInput = findViewById(R.id.laneInput);
        addSlotButton = findViewById(R.id.addSlotButton);
        slotsLayout = findViewById(R.id.slotsLayout);
        db = FirebaseFirestore.getInstance();

        setupDateTimePickers();
        addSlotButton.setOnClickListener(v -> addNewSlot());
        loadAllSlots();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_main);

        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_main) {
                startActivity(new Intent(this, mainPage.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, profile.class));
                return true;
            } else if (id == R.id.nav_add) {
                startActivity(new Intent(this, AdminAddSlotActivity.class));
                return true;
            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return true;
            }
            return false;
        });

    }

    private void setupDateTimePickers() {
        dateInput.setFocusable(false);
        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            new android.app.DatePickerDialog(
                    this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                        dateInput.setText(selectedDate);
                    },
                    year, month, day
            ).show();
        });

        timeInput.setFocusable(false);
        timeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            new android.app.TimePickerDialog(
                    this,
                    (view, hourOfDay, minute1) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                        timeInput.setText(selectedTime);
                    },
                    hour, minute, true
            ).show();
        });
    }

    private void addNewSlot() {
        String date = dateInput.getText().toString().trim();
        String time = timeInput.getText().toString().trim();
        String lane = laneInput.getText().toString().trim();

        if (date.isEmpty() || time.isEmpty() || lane.isEmpty()) {
            Toast.makeText(this, "Minden mezőt ki kell tölteni", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("slots")
                .orderBy("index", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    int newIndex = 0;
                    if (!querySnapshot.isEmpty()) {
                        Long maxIndex = querySnapshot.getDocuments().get(0).getLong("index");
                        if (maxIndex != null) newIndex = maxIndex.intValue() + 1;
                    }

                    Map<String, Object> slot = new HashMap<>();
                    slot.put("date", date);
                    slot.put("time", time);
                    slot.put("lane", lane);
                    slot.put("reserved", false);
                    slot.put("index", newIndex);

                    db.collection("slots")
                            .add(slot)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(this, "Időpont hozzáadva", Toast.LENGTH_SHORT).show();
                                loadAllSlots();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                            );
                });
    }

    private void loadAllSlots() {
        slotsLayout.removeAllViews();

        db.collection("slots")
                .orderBy("index")
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
                        tv.setText(String.format("Dátum: %s | Idő: %s | Pálya: %s", date, time, lane));
                        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        Button deleteBtn = new Button(this);
                        deleteBtn.setText("Törlés");

                        deleteBtn.setOnClickListener(v -> {
                            db.collection("slots").document(docId)
                                    .delete()
                                    .addOnSuccessListener(unused -> {
                                        Toast.makeText(this, "Slot törölve", Toast.LENGTH_SHORT).show();
                                        loadAllSlots();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        });

                        slotLayout.addView(tv);
                        slotLayout.addView(deleteBtn);
                        slotsLayout.addView(slotLayout);


                        slotLayout.setAlpha(0f);
                        slotLayout.setScaleX(0.8f);
                        slotLayout.setScaleY(0.8f);

                        slotLayout.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(300)
                                .start();
                    }
                });
    }

}
