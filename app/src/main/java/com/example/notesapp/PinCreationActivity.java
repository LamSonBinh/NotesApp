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

public class PinCreationActivity extends AppCompatActivity {
    private EditText pinEditText, confirmPinEditText;
    private Button createPinButton, backToNotesButton;  // Khai báo nút Quay lại

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pin_creation);

        pinEditText = findViewById(R.id.pinEditText);
        confirmPinEditText = findViewById(R.id.confirmPinEditText);
        createPinButton = findViewById(R.id.createPinButton);
        backToNotesButton = findViewById(R.id.backToNotesButton);  // Khởi tạo nút Quay lại

        createPinButton.setOnClickListener(v -> createPin());

        // Sự kiện cho nút Quay lại
        backToNotesButton.setOnClickListener(v -> backToNotesActivity());
    }

    private void createPin() {
        String pin = pinEditText.getText().toString();
        String confirmPin = confirmPinEditText.getText().toString();

        if (pin.equals(confirmPin)) {
            // Lấy UID của người dùng hiện tại
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

            if (currentUser != null) {
                String userId = currentUser.getUid();  // UID của người dùng

                // Lưu mã PIN vào SharedPreferences theo UID
                SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(userId + "_userPin", pin);
                editor.putBoolean(userId + "_isPinVerified", false); // Chưa xác thực mã PIN
                editor.apply();

                // Sau khi tạo mã PIN xong, chuyển đến màn hình yêu cầu nhập mã PIN
                Intent intent = new Intent(PinCreationActivity.this, PinActivity.class);
                startActivity(intent);
                finish();
            }
        } else {
            // Hiển thị thông báo nếu mã PIN không khớp
            Toast.makeText(PinCreationActivity.this, "Mã PIN không khớp. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }

    private void backToNotesActivity() {
        // Quay lại màn hình NotesActivity
        Intent intent = new Intent(PinCreationActivity.this, notesactivity.class);
        startActivity(intent);
        finish();  // Đóng màn hình hiện tại
    }
}
