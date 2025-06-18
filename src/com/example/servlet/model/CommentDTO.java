package com.example.servlet.model;

public class CommentDTO {
    private int id;
    private int articleId;
    private String userPhone;
    private String userNickname;
    private String content;
    private String commentDate; // 格式化后的日期字符串

    public CommentDTO() {}

    public CommentDTO(int id, int articleId, String userPhone, String userNickname, String content, String commentDate) {
        this.id = id;
        this.articleId = articleId;
        this.userPhone = userPhone;
        this.userNickname = userNickname;
        this.content = content;
        this.commentDate = commentDate;
    }

    // Getters
    public int getId() { return id; }
    public int getArticleId() { return articleId; }
    public String getUserPhone() { return userPhone; }
    public String getUserNickname() { return userNickname; }
    public String getContent() { return content; }
    public String getCommentDate() { return commentDate; }

    // Setters (可选)
    public void setId(int id) { this.id = id; }
    public void setArticleId(int articleId) { this.articleId = articleId; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public void setUserNickname(String userNickname) { this.userNickname = userNickname; }
    public void setContent(String content) { this.content = content; }
    public void setCommentDate(String commentDate) { this.commentDate = commentDate; }
}