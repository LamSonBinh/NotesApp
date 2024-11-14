package com.example.notesapp;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class NoteViewHolder extends RecyclerView.ViewHolder {
    TextView notetitle;
    TextView notecontent;
    LinearLayout mnote;
    ImageView menupopbutton; // Thêm ImageView cho menu button
    TextView createdAtTextView;

    public NoteViewHolder(@NonNull View itemView) {
        super(itemView);
        notetitle = itemView.findViewById(R.id.notetitle);
        notecontent = itemView.findViewById(R.id.notecontent);
        mnote = itemView.findViewById(R.id.note);
        menupopbutton = itemView.findViewById(R.id.menupopbutton); // Tìm kiếm menupopbutton
        createdAtTextView = itemView.findViewById(R.id.createdAtTextView); // Kết nối TextView với ID
    }
}
