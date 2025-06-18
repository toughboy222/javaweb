package com.example.servlet.model;

public class AuthorRankDTO {
    private String authorIdentifier; // 作者的昵称或手机号
    private int totalLikes;

    public AuthorRankDTO(String authorIdentifier, int totalLikes) {
        this.authorIdentifier = authorIdentifier;
        this.totalLikes = totalLikes;
    }

    // Getters (根据需要可以添加 Setters)
    public String getAuthorIdentifier() {
        return authorIdentifier;
    }

    public int getTotalLikes() {
        return totalLikes;
    }
}