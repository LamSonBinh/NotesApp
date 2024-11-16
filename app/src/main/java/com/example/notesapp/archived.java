package com.example.notesapp;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class archived extends AppCompatActivity {

    RecyclerView archivedRecyclerView;
    FirebaseFirestore firebaseFirestore;
    FirebaseUser firebaseUser;
    FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder> archivedAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archived);

        // Đổi tiêu đề của ActionBar thành "Ghi chú đã lưu trữ"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Ghi chú đã lưu trữ");
        }

        // Kích hoạt nút back trên ActionBar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        archivedRecyclerView = findViewById(R.id.recyclerview);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Query để lấy ghi chú từ Archive
        Query query = firebaseFirestore.collection("archived")
                .document(firebaseUser.getUid())
                .collection("myArchivedNotes")
                .orderBy("title", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<firebasemodel> allArchivedNotes = new FirestoreRecyclerOptions.Builder<firebasemodel>()
                .setQuery(query, firebasemodel.class)
                .build();

        archivedAdapter = new FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder>(allArchivedNotes) {
            @Override
            protected void onBindViewHolder(@NonNull NoteViewHolder holder, int position, @NonNull firebasemodel model) {
                holder.notetitle.setText(model.getTitle());

                // Kiểm tra độ dài của nội dung ghi chú
                String content = model.getContent();
                if (content.length() > 60) {
                    content = content.substring(0, 60) + "...";  // Cắt nội dung và thêm dấu ba chấm
                }
                holder.notecontent.setText(content);

                // Chuyển đổi timestamp thành thời gian đọc được
                long timestamp = model.getCreatedAt();  // Giả sử bạn đã lưu trường createdAt trong model
                String formattedTime = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(timestamp));

                // Hiển thị thời gian trên giao diện
                holder.createdAtTextView.setText(formattedTime);

                // Get document ID at the current position
                String docId = getSnapshots().getSnapshot(position).getId();

                holder.menupopbutton.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);

                    // Add "Edit" option
                    popupMenu.getMenu().add("Chỉnh sửa").setOnMenuItemClickListener(item -> {
                        Intent intent = new Intent(v.getContext(), editnoteactivity.class);
                        intent.putExtra("title", model.getTitle());
                        intent.putExtra("content", model.getContent());
                        intent.putExtra("noteId", docId);  // Pass the docId here
                        intent.putExtra("collectionName", "archived");
                        intent.putExtra("subCollectionName", "myArchivedNotes");  // Pass sub-collection name
                        v.getContext().startActivity(intent);
                        return true;
                    });

                    // Add "Remove from Archived" option
                    popupMenu.getMenu().add("Bỏ lưu").setOnMenuItemClickListener(item -> {
                        removeFromArchived(docId, model); // Move note out of archived
                        return true;
                    });

                    // Add "Delete" option
                    popupMenu.getMenu().add("Xóa").setOnMenuItemClickListener(item -> {
                        moveToTrash(docId, model); // Move note to trash
                        return true;
                    });

                    // Add "Export to PDF" option
                    popupMenu.getMenu().add("Xuất file PDF").setOnMenuItemClickListener(item -> {
                        exportNoteToPDF(model.getTitle(), model.getContent());
                        return false;
                    });

                    // Add "Export to DOCX" option
                    popupMenu.getMenu().add("Xuất file DOCX").setOnMenuItemClickListener(item -> {
                        exportNoteToDOCX(model.getTitle(), model.getContent());
                        return false;
                    });

                    popupMenu.show();
                });
            }


            // Chỉnh sửa ghi chú
            private void editNote(String docId) {
                Intent intent = new Intent(archived.this, editnoteactivity.class);
                intent.putExtra("noteId", docId);
                startActivity(intent);
            }

            // Di chuyển ghi chú ra khỏi lưu trữ
            private void removeFromArchived(String docId, firebasemodel model) {
                // Thêm ghi chú trở lại collection "myNotes"
                firebaseFirestore.collection("notes")
                        .document(firebaseUser.getUid())
                        .collection("myNotes")
                        .document(docId)
                        .set(model)
                        .addOnSuccessListener(aVoid -> {
                            // Xóa ghi chú khỏi Archive
                            deleteNoteFromArchived(docId);

                            Toast.makeText(archived.this, "Ghi chú đã được khôi phục", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(archived.this, "Khôi phục ghi chú thất bại", Toast.LENGTH_SHORT).show();
                        });
            }

            // Xóa ghi chú khỏi Archive
            private void deleteNoteFromArchived(String docId) {
                firebaseFirestore.collection("archived")
                        .document(firebaseUser.getUid())
                        .collection("myArchivedNotes")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(archived.this, "Ghi chú đã bị xóa khỏi lưu trữ", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(archived.this, "Xóa ghi chú khỏi lưu trữ thất bại", Toast.LENGTH_SHORT).show();
                        });
            }

            // Chuyển ghi chú vào thùng rác
            private void moveToTrash(String docId, firebasemodel model) {
                // Thêm ghi chú vào Trash
                firebaseFirestore.collection("trash")
                        .document(firebaseUser.getUid())
                        .collection("myTrashNotes")
                        .document(docId)
                        .set(model)
                        .addOnSuccessListener(aVoid -> {
                            // Xóa ghi chú khỏi Archive
                            deleteNoteFromArchived(docId);

                            Toast.makeText(archived.this, "Ghi chú đã được chuyển vào thùng rác", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(archived.this, "Chuyển ghi chú vào thùng rác thất bại", Toast.LENGTH_SHORT).show();
                        });
            }

            @NonNull
            @Override
            public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.notes_layout, parent, false);
                return new NoteViewHolder(view);
            }
        };

        // Đặt RecyclerView
        archivedRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        archivedRecyclerView.setAdapter(archivedAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        archivedAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        archivedAdapter.stopListening();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Quay về NotesActivity
            Intent intent = new Intent(archived.this, notesactivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Đảm bảo không quay lại Archived
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
}
