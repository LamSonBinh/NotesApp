package com.example.notesapp;

import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class PinActivity extends AppCompatActivity {
    private EditText pinEditText;
    private Button submitButton, backToNotesButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin);

        pinEditText = findViewById(R.id.pinEditText);
        submitButton = findViewById(R.id.submitButton);
        backToNotesButton = findViewById(R.id.backToNotesButton);

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
            String userPin = sharedPreferences.getString(userId + "_userPin", null);

            if (userPin == null) {
                // Chuyển đến màn hình tạo mã PIN nếu người dùng chưa có
                Intent intent = new Intent(PinActivity.this, PinCreationActivity.class);
                startActivity(intent);
                finish();
            }
        }

        submitButton.setOnClickListener(v -> checkPin());
        backToNotesButton.setOnClickListener(v -> backToNotesActivity());
    }

    private void checkPin() {
        // Lấy UID của người dùng hiện tại
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();  // UID của người dùng
            SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);

            // Lấy mã PIN của người dùng từ SharedPreferences theo UID
            String correctPin = sharedPreferences.getString(userId + "_userPin", null);

            String enteredPin = pinEditText.getText().toString();

            if (enteredPin.equals(correctPin)) {
                // Mã PIN đúng, cập nhật trạng thái đã xác thực
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(userId + "_isPinVerified", true);
                editor.apply();

                // Chuyển đến Lưu trữ
                Intent intent = new Intent(PinActivity.this, archived.class);
                startActivity(intent);
                finish();
            } else {
                // Mã PIN sai, hiển thị thông báo
                Toast.makeText(PinActivity.this, "Mã PIN sai. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void backToNotesActivity() {
        // Quay lại màn hình NotesActivity
        Intent intent = new Intent(PinActivity.this, notesactivity.class);
        startActivity(intent);
        finish();  // Đóng màn hình hiện tại
    }
}
