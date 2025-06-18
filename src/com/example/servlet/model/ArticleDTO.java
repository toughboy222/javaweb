package com.example.servlet.model;

public class ArticleDTO {
    private int id;
    private String title;
    private String contentSnippet; // 文章摘要，用于列表显示
    private String fullContent;    // 完整的文章内容，用于单篇文章显示
    private String authorIdentifier; // 用于显示作者昵称或手机号
    private String publishDate;
    private String category;
    private int likesCount; // 新增：点赞总数
    private boolean likedByCurrentUser; // 新增：当前用户是否已点赞 (主要用于文章详情)

    // 空构造函数
    public ArticleDTO() {}

    // 更新构造函数以包含新字段 (如果需要，或者主要通过 setters)
    public ArticleDTO(int id, String title, String contentSnippet, String fullContent, String authorIdentifier, String publishDate, String category, int likesCount) {
        this.id = id;
        this.title = title;
        this.contentSnippet = contentSnippet;
        this.fullContent = fullContent;
        this.authorIdentifier = authorIdentifier;
        this.publishDate = publishDate;
        this.category = category;
        this.likesCount = likesCount;
        this.likedByCurrentUser = false; // 默认为false
    }


    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getContentSnippet() { return contentSnippet; }
    public String getFullContent() { return fullContent; }
    public String getAuthorIdentifier() { return authorIdentifier; }
    public String getPublishDate() { return publishDate; }
    public String getCategory() { return category; }
    public int getLikesCount() { return likesCount; } // 新增
    public boolean isLikedByCurrentUser() { return likedByCurrentUser; } // 新增


    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setContentSnippet(String contentSnippet) { this.contentSnippet = contentSnippet; }
    public void setFullContent(String fullContent) { this.fullContent = fullContent; }
    public void setAuthorIdentifier(String authorIdentifier) { this.authorIdentifier = authorIdentifier; }
    public void setPublishDate(String publishDate) { this.publishDate = publishDate; }
    public void setCategory(String category) { this.category = category; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; } // 新增
    public void setLikedByCurrentUser(boolean likedByCurrentUser) { this.likedByCurrentUser = likedByCurrentUser; } // 新增
}