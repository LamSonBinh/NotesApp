package com.example.notesapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class trash extends AppCompatActivity {

    RecyclerView trashRecyclerView;
    StaggeredGridLayoutManager staggeredGridLayoutManager;
    FirebaseFirestore firebaseFirestore;
    FirebaseUser firebaseUser;
    FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder> trashAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        // Đổi tiêu đề của ActionBar thành "Thùng rác"
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Thùng rác");
        }

        // Kích hoạt nút back trên ActionBar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        trashRecyclerView = findViewById(R.id.trashRecyclerView);
        firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        // Query để lấy ghi chú từ Trash
        Query query = firebaseFirestore.collection("trash")
                .document(firebaseUser.getUid())
                .collection("myTrashNotes")
                .orderBy("title", Query.Direction.ASCENDING);

        FirestoreRecyclerOptions<firebasemodel> allTrashNotes = new FirestoreRecyclerOptions.Builder<firebasemodel>()
                .setQuery(query, firebasemodel.class)
                .build();

        trashAdapter = new FirestoreRecyclerAdapter<firebasemodel, NoteViewHolder>(allTrashNotes) {
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


                holder.menupopbutton.setOnClickListener(v -> {
                    PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
                    popupMenu.setGravity(Gravity.END);

                    // Thêm mục "Restore" vào menu
                    popupMenu.getMenu().add("Khôi phục").setOnMenuItemClickListener(item -> {
                        String docId = getSnapshots().getSnapshot(position).getId();
                        restoreNote(docId, model); // Phục hồi ghi chú từ Trash
                        return true;
                    });

                    // Mục "Delete Permanently" để xóa ghi chú vĩnh viễn
                    popupMenu.getMenu().add("Xóa vĩnh viễn").setOnMenuItemClickListener(item -> {
                        String docId = getSnapshots().getSnapshot(position).getId();
                        deleteNotePermanently(docId);
                        return true;
                    });

                    popupMenu.show();
                });
            }

            // Phục hồi ghi chú từ Trash về Notes
            private void restoreNote(String docId, firebasemodel model) {
                // Thêm ghi chú trở lại collection "myNotes"
                firebaseFirestore.collection("notes")
                        .document(firebaseUser.getUid())
                        .collection("myNotes")
                        .document(docId)
                        .set(model)
                        .addOnSuccessListener(aVoid -> {
                            // Xóa ghi chú khỏi Trash
                            deleteNoteFromTrash(docId);

                            Toast.makeText(trash.this, "Ghi chú đã được khôi phục", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("RestoreError", "Failed to restore note", e);
                            Toast.makeText(trash.this, "Khôi phục ghi chú thất bại", Toast.LENGTH_SHORT).show();
                        });
            }

            // Xóa ghi chú khỏi Trash sau khi khôi phục
            private void deleteNoteFromTrash(String docId) {
                firebaseFirestore.collection("trash")
                        .document(firebaseUser.getUid())
                        .collection("myTrashNotes")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Trash", "Note deleted from trash after restore");
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TrashError", "Failed to delete note from trash after restore", e);
                        });
            }

            // Xóa ghi chú vĩnh viễn khỏi Trash
            private void deleteNotePermanently(String docId) {
                firebaseFirestore.collection("trash")
                        .document(firebaseUser.getUid())
                        .collection("myTrashNotes")
                        .document(docId)
                        .delete()
                        .addOnSuccessListener(aVoid -> {
                            Log.d("Trash", "Note deleted permanently");
                            Toast.makeText(trash.this, "Ghi chú đã bị xóa vĩnh viễn", Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TrashError", "Failed to delete note permanently", e);
                            Toast.makeText(trash.this, "Xóa ghi chú vĩnh viễn thất bại", Toast.LENGTH_SHORT).show();
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
        trashRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        trashRecyclerView.setAdapter(trashAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        trashAdapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        trashAdapter.stopListening();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Quay về NotesActivity
            Intent intent = new Intent(trash.this, notesactivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); // Đảm bảo không quay lại Trash
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

//fsajklfjask
//sajkflajfa
//fsajfklaj
//fmaslkghal
//fmasl;fkasf