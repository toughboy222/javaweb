package com.example.servlet.model;

public class UserDTO {
    private String phone;
    private String nickname;
    private String avatarUrl;
    // 你可以根据需要添加其他字段，如注册时间等

    public UserDTO() {}

    public UserDTO(String phone, String nickname, String avatarUrl) {
        this.phone = phone;
        this.nickname = nickname;
        this.avatarUrl = avatarUrl;
    }

    // Getters
    public String getPhone() { return phone; }
    public String getNickname() { return nickname; }
    public String getAvatarUrl() { return avatarUrl; }

    // Setters
    public void setPhone(String phone) { this.phone = phone; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}