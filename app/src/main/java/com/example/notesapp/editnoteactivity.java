package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class editnoteactivity extends AppCompatActivity {

    Intent data;
    EditText medittitleofnote,meditcontentofnote;
    FloatingActionButton msaveeditnote;

    FirebaseAuth firebaseAuth;
    FirebaseFirestore firebaseFirestore;
    FirebaseUser firebaseUser;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_editnoteactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        medittitleofnote=findViewById(R.id.edittitleofnote);
        meditcontentofnote=findViewById(R.id.editcontentofnote);
        msaveeditnote=findViewById(R.id.saveeditnote);

        data=getIntent();

        firebaseFirestore=FirebaseFirestore.getInstance();
        firebaseUser=FirebaseAuth.getInstance().getCurrentUser();


        Toolbar toolbar=findViewById(R.id.toolbarofeditnote);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        String collectionName = data.getStringExtra("collectionName");
        String subCollectionName = data.getStringExtra("subCollectionName");


        msaveeditnote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newtitle = medittitleofnote.getText().toString();
                String newcontent = meditcontentofnote.getText().toString();

                if (newtitle.isEmpty() || newcontent.isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Tiêu đề và nội dung không để trống", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    DocumentReference documentReference = firebaseFirestore
                            .collection(collectionName)  // Collection như "notes" hoặc "archived"
                            .document(firebaseUser.getUid())
                            .collection(subCollectionName)  // Sub-collection như "myNotes" hoặc "myArchivedNotes"
                            .document(data.getStringExtra("noteId"));

                    long timestamp = System.currentTimeMillis();  // Lấy thời gian hiện tại

                    Map<String, Object> note = new HashMap<>();
                    note.put("title", newtitle);
                    note.put("content", newcontent);
                    note.put("createdAt", timestamp);  // Ghi đè createdAt với thời gian hiện tại

                    documentReference.set(note)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(getApplicationContext(), "Cập nhật ghi chú thành công", Toast.LENGTH_SHORT).show();

                                if ("archived".equals(collectionName)) {
                                    startActivity(new Intent(editnoteactivity.this, archived.class));
                                } else {
                                    startActivity(new Intent(editnoteactivity.this, notesactivity.class));
                                }
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getApplicationContext(), "Cập nhật ghi chú thất bại", Toast.LENGTH_SHORT).show()
                            );
                }
            }
        });




        String notetitle=data.getStringExtra("title");
        String notecontent=data.getStringExtra("content");
        meditcontentofnote.setText(notecontent);
        medittitleofnote.setText(notetitle);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            String collectionName = data.getStringExtra("collectionName");
            Intent intent;

            if ("archived".equals(collectionName)) {
                intent = new Intent(editnoteactivity.this, archived.class);
            } else {
                intent = new Intent(editnoteactivity.this, notesactivity.class);
            }

            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}