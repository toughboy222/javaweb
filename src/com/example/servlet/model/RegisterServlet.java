package com.example.servlet.model; // 请确保这个包名和您项目的结构一致

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson; // 确保您的项目中已添加 Gson 依赖库

public class RegisterServlet extends HttpServlet {

    /**
     * 在Servlet第一次被请求时加载JDBC驱动。
     */
    @Override
    public void init() throws ServletException {
        super.init();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("RegisterServlet: MySQL JDBC Driver has been loaded successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("RegisterServlet: FATAL ERROR - MySQL JDBC Driver not found in WEB-INF/lib!");
            throw new ServletException("Failed to load MySQL JDBC Driver", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String username = request.getParameter("username"); // 对应前端的手机号
        String nickname = request.getParameter("nickname");
        String password = request.getParameter("password");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

        // --- 输入参数校验 ---
        if (username == null || username.trim().isEmpty() ||
                nickname == null || nickname.trim().isEmpty() ||
                password == null || password.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); // 400 Bad Request
            out.write(gson.toJson(new ErrorResponse("手机号、昵称和密码均不能为空")));
            out.flush();
            out.close();
            return;
        }
        // ... 其他校验 ...
        if (!username.matches("^1[3-9]\\d{9}$")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("无效的手机号格式")));
            out.flush();
            out.close();
            return;
        }
        if (password.length() < 6) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("密码长度不能少于6位")));
            out.flush();
            out.close();
            return;
        }


        // --- 数据库操作 ---
        Connection conn = null;
        PreparedStatement checkUserStmt = null;
        PreparedStatement insertUserStmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");

            // 1. 检查手机号是否已存在
            String checkUserSql = "SELECT phone FROM users WHERE phone = ?";
            checkUserStmt = conn.prepareStatement(checkUserSql);
            checkUserStmt.setString(1, username);
            rs = checkUserStmt.executeQuery();

            if (rs.next()) {
                // 【核心修正】用户（手机号）已存在，返回409和包含错误信息的JSON
                response.setStatus(HttpServletResponse.SC_CONFLICT); // 409 Conflict
                out.write(gson.toJson(new ErrorResponse("该手机号已被注册")));
                // 【推荐】发送完响应后直接返回，终止后续代码执行
                return;
            } else {
                // 2. 用户不存在，插入新用户
                rs.close(); // 关闭上一个查询的结果集
                checkUserStmt.close(); // 关闭上一个查询的PreparedStatement

                String insertUserSql = "INSERT INTO users (phone, nickname, password) VALUES (?, ?, ?)";
                insertUserStmt = conn.prepareStatement(insertUserSql);
                insertUserStmt.setString(1, username);
                insertUserStmt.setString(2, nickname);
                insertUserStmt.setString(3, password);

                int rowsAffected = insertUserStmt.executeUpdate();
                if (rowsAffected > 0) {
                    response.setStatus(HttpServletResponse.SC_CREATED); // 201 Created
                    // 注意：成功的响应也应该是JSON格式，这是一种好的实践
                    out.write("{\"success\": true, \"message\": \"注册成功！将跳转到登录页面。\"}");
                } else {
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
                    out.write(gson.toJson(new ErrorResponse("注册失败，数据库未受影响，请稍后重试。")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); // 500
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) { // 唯一约束冲突
                out.write(gson.toJson(new ErrorResponse("手机号或昵称可能已被占用。")));
            } else {
                out.write(gson.toJson(new ErrorResponse("数据库操作失败，请联系管理员。")));
            }
        } finally {
            // 依次关闭数据库资源
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (checkUserStmt != null) checkUserStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (insertUserStmt != null) insertUserStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            // 最后关闭输出流
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    /**
     * 【核心修正】
     * 添加此内部类用于封装错误信息并转换为JSON。
     * Gson库会根据字段名 "error" 进行序列化，这个字段名必须与前端JS代码中期望的 `errData.error` 保持一致。
     */
    private static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }
    }
}