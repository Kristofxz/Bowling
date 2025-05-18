package com.example.bowling;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AdminAddSlotActivity extends AppCompatActivity {

    private EditText dateInput, timeInput, laneInput;
    private Button addSlotButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_add_slot);

        dateInput = findViewById(R.id.dateInput);     // pl. "2025-05-17"
        timeInput = findViewById(R.id.timeInput);     // pl. "18:00"
        laneInput = findViewById(R.id.laneInput);     // pl. "1"
        addSlotButton = findViewById(R.id.addSlotButton);
        db = FirebaseFirestore.getInstance();

        // 🔹 Dátumválasztó megnyitása
        dateInput.setFocusable(false);
        dateInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(
                    AdminAddSlotActivity.this,
                    (view, year1, monthOfYear, dayOfMonth) -> {
                        String selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year1, monthOfYear + 1, dayOfMonth);
                        dateInput.setText(selectedDate);
                    },
                    year, month, day
            );
            datePickerDialog.show();
        });

        // 🔹 Időválasztó megnyitása
        timeInput.setFocusable(false);
        timeInput.setOnClickListener(v -> {
            Calendar calendar = Calendar.getInstance();
            int hour = calendar.get(Calendar.HOUR_OF_DAY);
            int minute = calendar.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(
                    AdminAddSlotActivity.this,
                    (view, hourOfDay, minute1) -> {
                        String selectedTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute1);
                        timeInput.setText(selectedTime);
                    },
                    hour, minute, true // 24 órás formátum
            );
            timePickerDialog.show();
        });

        // 🔹 Slot hozzáadása
        addSlotButton.setOnClickListener(v -> {
            String date = dateInput.getText().toString().trim();
            String time = timeInput.getText().toString().trim();
            String lane = laneInput.getText().toString().trim();

            if (date.isEmpty() || time.isEmpty() || lane.isEmpty()) {
                Toast.makeText(this, "Minden mezőt ki kell tölteni", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> slot = new HashMap<>();
            slot.put("date", date);
            slot.put("time", time);
            slot.put("lane", lane);
            slot.put("reserved", false);

            db.collection("slots")
                    .add(slot)
                    .addOnSuccessListener(documentReference ->
                            Toast.makeText(this, "Időpont hozzáadva", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Hiba: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
        });


    }
}
