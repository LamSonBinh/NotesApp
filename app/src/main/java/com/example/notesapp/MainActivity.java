package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.android.gms.common.api.ApiException;




public class MainActivity extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private final int RC_SIGN_IN = 100;





    private EditText mloginemail, mloginpassword;
    private RelativeLayout mlogin, mgotosignup;
    private TextView mgotoforgotpassword;

    private FirebaseAuth firebaseAuth;

    ProgressBar mprogressbarofmainactivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Setup Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.google_sign_in_button).setOnClickListener(v -> signInWithGoogle());

        // Initialize FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();









        // Ẩn ActionBar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Khởi tạo các view
        mloginemail = findViewById(R.id.loginemail);
        mloginpassword = findViewById(R.id.loginpassword);
        mlogin = findViewById(R.id.login);
        mgotoforgotpassword = findViewById(R.id.gotoforgotpassword);
        mgotosignup = findViewById(R.id.gotosignup);
        mprogressbarofmainactivity=findViewById(R.id.progressbarofmainactivity);

        // Khởi tạo FirebaseAuth
        firebaseAuth = FirebaseAuth.getInstance();

        // Kiểm tra người dùng đã đăng nhập hay chưa
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            // Nếu đã đăng nhập, chuyển đến NotesActivity
            finish();
            startActivity(new Intent(MainActivity.this, notesactivity.class));
        }

        // Chuyển đến màn hình đăng ký khi nhấn "Đăng ký"
        mgotosignup.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, signup.class)));

        // Chuyển đến màn hình quên mật khẩu
        mgotoforgotpassword.setOnClickListener(v -> startActivity(new Intent(MainActivity.this, fogotpassword.class)));

        // Xử lý sự kiện nhấn "Đăng nhập"
        mlogin.setOnClickListener(v -> {
            String mail = mloginemail.getText().toString().trim();
            String password = mloginpassword.getText().toString().trim();

            if (mail.isEmpty() || password.isEmpty()) {
                Toast.makeText(getApplicationContext(), "All fields are required", Toast.LENGTH_SHORT).show();
            }
            else
            {
                // Login the user


                mprogressbarofmainactivity.setVisibility(View.VISIBLE);

                // Thực hiện đăng nhập
                firebaseAuth.signInWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Nếu đăng nhập thành công, kiểm tra email đã xác minh chưa
                            checkEmailVerification();
                        } else {
                            // Nếu đăng nhập thất bại, hiển thị thông báo lỗi chi tiết
                            Toast.makeText(getApplicationContext(), "Account does not exist or invalid credentials", Toast.LENGTH_SHORT).show();
                            mprogressbarofmainactivity.setVisibility(View.INVISIBLE);
                        }
                    }
                });
            }
        });
    }

    // Kiểm tra xem email đã được xác minh hay chưa
    private void checkEmailVerification() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null && firebaseUser.isEmailVerified()) {
            // Nếu email đã được xác minh, chuyển sang màn hình NotesActivity
            Toast.makeText(getApplicationContext(), "Logged In", Toast.LENGTH_SHORT).show();
            finish();
            startActivity(new Intent(MainActivity.this, notesactivity.class));
        }
        else
        {
            mprogressbarofmainactivity.setVisibility(View.INVISIBLE);
            // Nếu email chưa được xác minh, yêu cầu người dùng xác minh email
            Toast.makeText(getApplicationContext(), "Please verify your email first", Toast.LENGTH_SHORT).show();
            firebaseAuth.signOut();
        }
    }

    private void signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener(this, task -> {
            googleSignInClient.revokeAccess().addOnCompleteListener(this, revokeTask -> {
                Intent signInIntent = googleSignInClient.getSignInIntent();
                startActivityForResult(signInIntent, RC_SIGN_IN);
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                Toast.makeText(this, "Google Sign-In failed.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Toast.makeText(MainActivity.this, "Google Sign-In Successful", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, notesactivity.class));
                finish();
            } else {
                Toast.makeText(MainActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
