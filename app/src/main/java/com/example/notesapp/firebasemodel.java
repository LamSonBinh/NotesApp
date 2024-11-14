package com.example.notesapp;

public class firebasemodel {
    private String title;
    private String content;
    private long createdAt;  // Thêm trường createdAt

    public firebasemodel() {
    }

    public firebasemodel(String title, String content, long createdAt) {
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;  // Khởi tạo với thời gian
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public long getCreatedAt() {  // Getter cho createdAt
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {  // Setter cho createdAt
        this.createdAt = createdAt;
    }
}


