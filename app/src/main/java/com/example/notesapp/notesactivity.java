package com.example.notesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Dành cho PDF
import android.graphics.pdf.PdfDocument;

// Dành cho DOCX (Apache POI)
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.text.SimpleDateFormat;
import java.util.Date;
public class notesactivity extends AppCompatActivity {

    FloatingActionButton mcreatenotesfab;
    private FirebaseAuth firebaseAuth;

    RecyclerView mrecyclerView;
    StaggeredGridLayoutManager staggeredGridLayoutManager;

    FirebaseUser firebaseUser;
    FirebaseFirestore firebaseFirestore;

    FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder> noteAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notesactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mcreatenotesfab = findViewById(R.id.createnotefab);
        firebaseAuth = FirebaseAuth.getInstance();

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        firebaseFirestore = FirebaseFirestore.getInstance();

        getSupportActionBar().setTitle("Tất cả ghi chú");

        mcreatenotesfab.setOnClickListener(v -> startActivity(new Intent(notesactivity.this, createnote.class)));

        Query query = firebaseFirestore.collection("notes")
                .document(firebaseUser.getUid())
                .collection("myNotes")
                .orderBy("title", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<firebasemodel> allusernotes = new FirestoreRecyclerOptions.Builder<firebasemodel>()
                .setQuery(query, firebasemodel.class)
                .build();

        noteAdapter = new FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder>(allusernotes) {
            @Override
            protected void onBindViewHolder(@NonNull NoteViewHolder noteViewHolder, int i, @NonNull firebasemodel firebasemodel) {

                ImageView popupbutton = noteViewHolder.itemView.findViewById(R.id.menupopbutton);

                int colourcode = getRandomColor();
                noteViewHolder.mnote.setBackgroundColor(noteViewHolder.itemView.getResources().getColor(colourcode, null));

                noteViewHolder.notetitle.setText(firebasemodel.getTitle());

                // Chỉ hiển thị 60 ký tự đầu tiên
                String noteContent = firebasemodel.getContent();
                if (noteContent.length() > 60) {
                    noteViewHolder.notecontent.setText(noteContent.substring(0, 60) + "...");  // Thêm dấu "..." nếu nội dung dài
                } else {
                    noteViewHolder.notecontent.setText(noteContent);  // Hiển thị đầy đủ nếu ngắn hơn 60 ký tự
                }


                // Hiển thị thời gian tạo ghi chú
                long createdAt = firebasemodel.getCreatedAt();
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String formattedDate = sdf.format(new Date(createdAt));
                noteViewHolder.createdAtTextView.setText(formattedDate);  // Giả sử bạn có một TextView để hiển thị thời gian


                String docId = noteAdapter.getSnapshots().getSnapshot(i).getId();

                noteViewHolder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(v.getContext(), notedetails.class);
                    intent.putExtra("title", firebasemodel.getTitle());
                    intent.putExtra("content", firebasemodel.getContent());  // Chuyển nội dung đầy đủ khi vào chi tiết
                    intent.putExtra("noteId", docId);

                    v.getContext().startActivity(intent);
                });

                popupbutton.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.setGravity(Gravity.END);
                    popupMenu.getMenu().add("Chỉnh sửa").setOnMenuItemClickListener(item -> {
                        Intent intent = new Intent(v.getContext(), editnoteactivity.class);
                        intent.putExtra("title", firebasemodel.getTitle());
                        intent.putExtra("content", firebasemodel.getContent());
                        intent.putExtra("noteId", docId);
                        intent.putExtra("collectionName", "notes");
                        intent.putExtra("subCollectionName", "myNotes");  // Truyền sub-collection
                        v.getContext().startActivity(intent);
                        return true;
                    });







                    popupMenu.getMenu().add("Lưu trữ").setOnMenuItemClickListener(item -> {
                        moveNoteToArchived(firebasemodel, docId); // Chuyển ghi chú vào lưu trữ
                        Toast.makeText(v.getContext(), "Ghi chú đã được lưu trữ", Toast.LENGTH_SHORT).show();
                        return false;
                    });



                    popupMenu.getMenu().add("Xóa").setOnMenuItemClickListener(item -> {
                        moveNoteToTrash(firebasemodel, docId); // Chuyển ghi chú vào trash
                        Toast.makeText(v.getContext(), "Ghi chú đã được chuyển vào thùng rác", Toast.LENGTH_SHORT).show();
                        return false;
                    });

                    popupMenu.getMenu().add("Xuất file PDF").setOnMenuItemClickListener(item -> {
                        exportNoteToPDF(firebasemodel.getTitle(), firebasemodel.getContent());
                        return false;
                    });

                    popupMenu.getMenu().add("Xuất file DOCX").setOnMenuItemClickListener(item -> {
                        exportNoteToDOCX(firebasemodel.getTitle(), firebasemodel.getContent());
                        return false;
                    });

                    popupMenu.show();
                });
            }


            @NonNull
            @Override
            public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_layout, parent, false);
                return new NoteViewHolder(view);
            }
        };

        mrecyclerView = findViewById(R.id.recyclerview);
        mrecyclerView.setHasFixedSize(true);
        staggeredGridLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mrecyclerView.setLayoutManager(staggeredGridLayoutManager);
        mrecyclerView.setAdapter(noteAdapter);
    }

    private void moveNoteToTrash(firebasemodel note, String docId) {
        // Thêm ghi chú vào collection "trash"
        firebaseFirestore.collection("trash")
                .document(firebaseUser.getUid())
                .collection("myTrashNotes")
                .document(docId)
                .set(note)
                .addOnSuccessListener(aVoid -> Log.d("Trash", "Note moved to trash"))
                .addOnFailureListener(e -> Log.e("TrashError", "Error moving note to trash", e));

        // Xóa ghi chú khỏi RecyclerView, nhưng không xóa Firebase
        DocumentReference documentReference = firebaseFirestore.collection("notes")
                .document(firebaseUser.getUid())
                .collection("myNotes")
                .document(docId);
        documentReference.delete()
                .addOnSuccessListener(aVoid -> Log.d("Trash", "Note removed from 'notes' collection"))
                .addOnFailureListener(e -> Log.e("Error", "Failed to remove note from 'notes' collection", e));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);

        // Kiểm tra xem người dùng đã đăng nhập bằng Google chưa
        if (firebaseUser != null && firebaseUser.getProviderData() != null) {
            for (UserInfo profile : firebaseUser.getProviderData()) {
                if (profile.getProviderId().equals("google.com")) {
                    // Ẩn "Change Password" nếu đăng nhập bằng Google
                    menu.findItem(R.id.change_password).setVisible(false);
                    break;
                }
            }
        }

        return true;
    }


    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.logout) {
            try {
                firebaseAuth.signOut();
                finish();
                startActivity(new Intent(notesactivity.this, MainActivity.class));
                return true;
            } catch (Exception e) {
                Log.e("LogoutError", "Error signing out", e);
            }
        } else if (itemId == R.id.change_password) {
            startActivity(new Intent(notesactivity.this, changepasswordactivity.class));
            return true;
        } else if (itemId == R.id.trash) { // Xử lý cho Trash
            startActivity(new Intent(notesactivity.this, trash.class));
            return true;
        } else if (itemId == R.id.archive) {
            // Kiểm tra xem người dùng đã tạo mã PIN chưa
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
                String userPin = sharedPreferences.getString(userId + "_userPin", null); // Lấy mã PIN theo UID của người dùng

                if (userPin == null || userPin.isEmpty()) {
                    // Nếu chưa có mã PIN, yêu cầu tạo mã PIN
                    Intent intent = new Intent(notesactivity.this, PinCreationActivity.class);
                    startActivity(intent);
                } else {
                    // Nếu đã có mã PIN, yêu cầu nhập mã PIN
                    Intent intent = new Intent(notesactivity.this, PinActivity.class);
                    startActivity(intent);
                }
            }

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onStart() {
        super.onStart();
        noteAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (noteAdapter != null) {
            noteAdapter.stopListening();
        }
    }

    private int getRandomColor() {
        List<Integer> colorcode = new ArrayList<>();
        colorcode.add(R.color.gray);
        colorcode.add(R.color.pink);
        colorcode.add(R.color.lightgreen);
        colorcode.add(R.color.skyblue);
        colorcode.add(R.color.color1);
        colorcode.add(R.color.color2);
        colorcode.add(R.color.color3);
        colorcode.add(R.color.color4);
        colorcode.add(R.color.color5);
        colorcode.add(R.color.green);

        Random random = new Random();
        int number = random.nextInt(colorcode.size());
        return colorcode.get(number);
    }


    private void exportNoteToPDF(String title, String content) {
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();

        paint.setTextSize(24f);
        canvas.drawText("Title: " + title, 80, 100, paint);

        paint.setTextSize(18f);
        canvas.drawText("Content: ", 80, 150, paint);

        paint.setTextSize(16f);
        canvas.drawText(content, 80, 200, paint);

        pdfDocument.finishPage(page);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "note.pdf");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            try (OutputStream out = getContentResolver().openOutputStream(
                    getContentResolver().insert(MediaStore.Files.getContentUri("external"), values))) {
                pdfDocument.writeTo(out);
                Toast.makeText(this, "PDF đã lưu vào Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu PDF", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Lưu cho thiết bị chạy API < 29
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "note.pdf");
            try (FileOutputStream out = new FileOutputStream(file)) {
                pdfDocument.writeTo(out);
                Toast.makeText(this, "PDF đã lưu vào Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu PDF", Toast.LENGTH_SHORT).show();
            }
        }
        pdfDocument.close();
    }


    private void exportNoteToDOCX(String title, String content) {
        XWPFDocument document = new XWPFDocument();
        XWPFParagraph titleParagraph = document.createParagraph();
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText("Title: " + title);
        titleRun.setBold(true);
        titleRun.setFontSize(20);

        XWPFParagraph contentParagraph = document.createParagraph();
        XWPFRun contentRun = contentParagraph.createRun();
        contentRun.setText("Content: " + content);
        contentRun.setFontSize(16);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, "note.docx");
            values.put(MediaStore.MediaColumns.MIME_TYPE, "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
            if (uri == null) {
                Toast.makeText(this, "Không tạo được tệp", Toast.LENGTH_SHORT).show();
                return;
            }

            try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                document.write(out);
                Toast.makeText(this, "DOCX đã lưu vào mục Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu DOCX", Toast.LENGTH_SHORT).show();
            }
        } else {
            // Lưu cho Android 9 trở xuống
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "note.docx");
            try (FileOutputStream out = new FileOutputStream(file)) {
                document.write(out);
                Toast.makeText(this, "DOCX đã lưu vào mục Downloads", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Lỗi khi lưu DOCX", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã được cấp phép", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Quyền bị từ chối. Chức năng xuất sẽ không hoạt động.", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private void moveNoteToArchived(firebasemodel note, String docId) {
        if (firebaseUser != null) {
            // Thêm ghi chú vào collection "archived"
            firebaseFirestore.collection("archived")
                    .document(firebaseUser.getUid())
                    .collection("myArchivedNotes")
                    .document(docId)
                    .set(note)
                    .addOnSuccessListener(aVoid -> Log.d("Archived", "Note moved to archived"))
                    .addOnFailureListener(e -> {
                        Log.e("ArchivedError", "Error moving note to archived", e);
                        Toast.makeText(getApplicationContext(), "Có lỗi khi di chuyển ghi chú", Toast.LENGTH_SHORT).show();
                    });

            // Xóa ghi chú khỏi collection "notes"
            DocumentReference documentReference = firebaseFirestore.collection("notes")
                    .document(firebaseUser.getUid())
                    .collection("myNotes")
                    .document(docId);
            documentReference.delete()
                    .addOnSuccessListener(aVoid -> Log.d("Archived", "Note removed from 'notes' collection"))
                    .addOnFailureListener(e -> {
                        Log.e("Error", "Failed to remove note from 'notes' collection", e);
                        Toast.makeText(getApplicationContext(), "Có lỗi khi xóa ghi chú", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("Firebase", "User is not logged in");
            Toast.makeText(getApplicationContext(), "Vui lòng đăng nhập lại", Toast.LENGTH_SHORT).show();
        }
    }






    private void saveUserPin(String pin) {
        SharedPreferences sharedPreferences = getSharedPreferences("userPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();  // Lấy UID của người dùng hiện tại
        editor.putString(userId + "_userPin", pin);  // Lưu mã PIN cho người dùng theo UID
        editor.apply();
    }


}
///fasklfa
//fsajklfa
//fsajfkl
//fafjaskfajl
