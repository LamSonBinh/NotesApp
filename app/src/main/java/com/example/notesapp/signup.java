package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
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
import com.google.firebase.Firebase;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class signup extends AppCompatActivity {


    private EditText msignupemail,msignuppassword;
    private RelativeLayout msignup;
    private TextView mgotologin;



    private FirebaseAuth firebaseAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

//        getSupportActionBar().hide();
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        msignupemail=findViewById(R.id.signupemail);
        msignuppassword=findViewById(R.id.signuppassword);
        msignup=findViewById(R.id.signup);
        mgotologin=findViewById(R.id.gotologin);

        firebaseAuth= FirebaseAuth.getInstance();


        mgotologin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(signup.this,MainActivity.class);
                startActivity(intent);
            }
        });

        msignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String mail = msignupemail.getText().toString().trim();
                String password = msignuppassword.getText().toString().trim();
                String confirmPassword = ((EditText) findViewById(R.id.signupconfirm_password)).getText().toString().trim();

                if(mail.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "All Fields are Required", Toast.LENGTH_SHORT).show();
                }
                else if(password.length() < 7) {
                    Toast.makeText(getApplicationContext(), "Password Should be Greater than 7 Digits", Toast.LENGTH_SHORT).show();
                }
                else if(!password.equals(confirmPassword)) {
                    Toast.makeText(getApplicationContext(), "Passwords Do Not Match", Toast.LENGTH_SHORT).show();
                }
                else {
                    // Register the user to Firebase
                    firebaseAuth.createUserWithEmailAndPassword(mail, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                                sendEmailVerification();
                            } else {
                                // In ra lỗi chi tiết
                                Exception exception = task.getException();
                                if (exception != null) {
                                    Toast.makeText(getApplicationContext(), "Failed To Register: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Failed To Register", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }
                    });

                }
            }
        });

    }

    // send email verification
    private void sendEmailVerification()
    {
        FirebaseUser firebaseUser=firebaseAuth.getCurrentUser();
        if(firebaseUser!=null)
        {
            firebaseUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Toast.makeText(getApplicationContext(),"Verification Email is Sent, Verify and Log In Again",Toast.LENGTH_SHORT).show();
                    firebaseAuth.signOut();
                    finish();
                    startActivity(new Intent(signup.this,MainActivity.class));
                }
            });
        }

        else
        {
            Toast.makeText(getApplicationContext(),"Failed To Send Verification Email",Toast.LENGTH_SHORT).show();
        }
    }


}