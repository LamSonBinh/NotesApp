package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.EmailAuthProvider;

public class changepasswordactivity extends AppCompatActivity {

    private EditText mNewPassword, mConfirmPassword, mCurrentPassword;
    private Button mChangePasswordButton;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_changepasswordactivity);

        // Kích hoạt ActionBar và thêm nút back
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mNewPassword = findViewById(R.id.new_password);
        mConfirmPassword = findViewById(R.id.confirm_password);
        mCurrentPassword = findViewById(R.id.current_password);
        mChangePasswordButton = findViewById(R.id.change_password_button);
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();

        mChangePasswordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newPassword = mNewPassword.getText().toString().trim();
                String confirmPassword = mConfirmPassword.getText().toString().trim();
                String currentPassword = mCurrentPassword.getText().toString().trim();

                // Kiểm tra nếu trường nhập liệu trống
                if (newPassword.isEmpty() || confirmPassword.isEmpty() || currentPassword.isEmpty()) {
                    Toast.makeText(changepasswordactivity.this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Kiểm tra mật khẩu xác nhận có khớp với mật khẩu mới không
                if (!newPassword.equals(confirmPassword)) {
                    Toast.makeText(changepasswordactivity.this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Xác thực lại người dùng với mật khẩu hiện tại
                AuthCredential credential = EmailAuthProvider.getCredential(firebaseUser.getEmail(), currentPassword);

                firebaseUser.reauthenticate(credential)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    // Sau khi xác thực, đổi mật khẩu
                                    firebaseUser.updatePassword(newPassword)
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(changepasswordactivity.this, "Password updated successfully", Toast.LENGTH_SHORT).show();

                                                        // Chuyển đến màn hình notesactivity
                                                        Intent intent = new Intent(changepasswordactivity.this, notesactivity.class);
                                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK); // Đảm bảo không quay lại màn hình cũ
                                                        startActivity(intent);
                                                        finish(); // Đóng activity hiện tại
                                                    } else {
                                                        Toast.makeText(changepasswordactivity.this, "Password update failed", Toast.LENGTH_SHORT).show();
                                                        Log.e("PasswordChange", "Error updating password", task.getException());
                                                    }
                                                }
                                            });
                                } else {
                                    Toast.makeText(changepasswordactivity.this, "Authentication failed", Toast.LENGTH_SHORT).show();
                                    Log.e("ReauthError", "Error reauthenticating", task.getException());
                                }
                            }
                        });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Chuyển đến NotesActivity thay vì quay lại màn hình trước đó
            Intent intent = new Intent(changepasswordactivity.this, notesactivity.class);
            startActivity(intent);
            finish(); // Đảm bảo không trở lại màn hình tạo ghi chú
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
