package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson; // 引入Gson用于更方便地构建JSON

public class LoginServlet extends HttpServlet {

    // 使用 doPost 方法来处理表单提交
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        // 设置请求编码为 UTF-8，以正确处理中文等字符
        request.setCharacterEncoding("UTF-8"); //

        // 设置响应编码和 CORS 头
        response.setContentType("application/json;charset=UTF-8"); //
        response.setHeader("Access-Control-Allow-Origin", "*"); //

        String username = request.getParameter("username"); // 手机号 //
        String password = request.getParameter("password"); //

        System.out.println("LoginServlet - Received username: " + username); //
        System.out.println("LoginServlet - Received password: " + (password != null && !password.isEmpty() ? "******" : "EMPTY")); // 不打印完整密码 //

        PrintWriter out = response.getWriter(); //
        Gson gson = new Gson(); // 用于构建JSON响应 //

        Connection conn = null; //
        PreparedStatement stmt = null; //
        ResultSet rs = null; //

        try {
            // JDBC 数据库连接 (数据库名已改为 sch)
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@"); //
            System.out.println("LoginServlet - Connected to MySQL database 'sch'"); //

            // SQL 查询语句，查找用户名和密码匹配的记录，并获取昵称和头像URL
            String sql = "SELECT phone, nickname, password, avatar_url FROM users WHERE phone = ?"; //
            stmt = conn.prepareStatement(sql); //
            stmt.setString(1, username); //

            rs = stmt.executeQuery(); //

            if (rs.next()) { //
                String dbPassword = rs.getString("password"); //
                // !!! 重要安全提示：实际生产环境必须比较哈希密码，而不是明文 !!!
                if (password != null && password.equals(dbPassword)) { // 假设当前是明文比较 //
                    String nickname = rs.getString("nickname"); //
                    String avatarUrl = rs.getString("avatar_url"); // 获取头像URL

                    // 登录成功，设置 Session
                    HttpSession session = request.getSession(true); // true 表示如果不存在则创建新session //
                    session.setAttribute("userPhone", username);    // 存储用户手机号 //
                    session.setAttribute("userNickname", nickname); // 存储用户昵称 //
                    session.setAttribute("isLoggedIn", true);     // 标记已登录 //
                    if (avatarUrl != null && !avatarUrl.isEmpty()) {
                        session.setAttribute("userAvatarUrl", avatarUrl);
                    } else {
                        session.removeAttribute("userAvatarUrl");
                    }


                    System.out.println("LoginServlet - User '" + username + "' logged in successfully. Nickname: " + nickname + ", AvatarURL: " + avatarUrl); //

                    // 构建成功的JSON响应，包含昵称和头像URL
                    LoginResponse successResponse = new LoginResponse(true, "登录成功", nickname, avatarUrl); //
                    out.write(gson.toJson(successResponse)); //

                } else {
                    System.out.println("LoginServlet - Invalid password for user: " + username); //
                    // 登录失败，传递4个参数，nickname 和 avatarUrl 为 null
                    out.write(gson.toJson(new LoginResponse(false, "用户名或密码错误", null, null))); //
                }
            } else {
                System.out.println("LoginServlet - User not found: " + username); //
                // 用户未找到，传递4个参数，nickname 和 avatarUrl 为 null
                out.write(gson.toJson(new LoginResponse(false, "用户名或密码错误", null, null))); //
            }
        } catch (SQLException e) {
            e.printStackTrace(); // 在服务器控制台打印详细错误 //
            System.err.println("LoginServlet - SQL Error: " + e.getMessage()); //
            // 数据库错误，传递4个参数，nickname 和 avatarUrl 为 null
            out.write(gson.toJson(new LoginResponse(false, "数据库错误，请稍后重试。", null, null))); //
        } catch (Exception e) {
            e.printStackTrace(); //
            System.err.println("LoginServlet - General Error: " + e.getMessage()); //
            // 其他错误，传递4个参数，nickname 和 avatarUrl 为 null
            out.write(gson.toJson(new LoginResponse(false, "登录时发生未知错误。", null, null))); //
        }
        finally {
            // 关闭数据库资源
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); } //
            try { if (stmt != null) stmt.close(); } catch (SQLException e) { e.printStackTrace(); } //
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); } //
            // 关闭输出流
            if (out != null) { //
                out.flush(); //
                out.close(); //
            }
        }
    }

    // 辅助内部类，用于构建JSON响应体
    private static class LoginResponse {
        boolean success;
        String message;
        String nickname; // 添加昵称字段 //
        String avatarUrl; // 添加头像URL字段

        public LoginResponse(boolean success, String message, String nickname, String avatarUrl) { //
            this.success = success; //
            this.message = message; //
            this.nickname = nickname; //
            this.avatarUrl = avatarUrl;
        }
    }

    @Override
    public void init() throws ServletException { //
        super.init(); //
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // 确保驱动已加载 (对于较新的MySQL Connector/J) //
            System.out.println("LoginServlet - MySQL JDBC Driver loaded."); //
        } catch (ClassNotFoundException e) {
            System.err.println("LoginServlet - MySQL JDBC Driver not found."); //
            e.printStackTrace(); //
        }
    }

    @Override
    public void destroy() { //
        super.destroy(); //
    }
}