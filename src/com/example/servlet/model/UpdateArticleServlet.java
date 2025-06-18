package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class UpdateArticleServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录，无法修改文章。")));
            out.flush();
            out.close();
            return;
        }
        String currentUserPhone = (String) session.getAttribute("userPhone");

        String articleIdParam = request.getParameter("articleId");
        String title = request.getParameter("title");
        String content = request.getParameter("content"); // Markdown 内容
        String category = request.getParameter("category");

        if (articleIdParam == null || title == null || title.trim().isEmpty() || content == null /* content可以为空，但不能是null */) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("文章ID、标题和内容为必填项。")));
            out.flush();
            out.close();
            return;
        }

        int articleId;
        try {
            articleId = Integer.parseInt(articleIdParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("无效的文章ID格式。")));
            out.flush();
            out.close();
            return;
        }

        Connection conn = null;
        PreparedStatement checkAuthorStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");

            // 1. 验证当前用户是否是文章作者
            String checkAuthorSql = "SELECT author_phone FROM articles WHERE id = ?";
            checkAuthorStmt = conn.prepareStatement(checkAuthorSql);
            checkAuthorStmt.setInt(1, articleId);
            rs = checkAuthorStmt.executeQuery();

            if (rs.next()) {
                String authorPhone = rs.getString("author_phone");
                if (!currentUserPhone.equals(authorPhone)) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN); // 403 禁止
                    out.write(gson.toJson(new ErrorResponse("您无权修改此文章。")));
                    out.flush();
                    out.close();
                    return;
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                out.write(gson.toJson(new ErrorResponse("要修改的文章不存在。")));
                out.flush();
                out.close();
                return;
            }
            rs.close();
            checkAuthorStmt.close();

            // 2. 更新文章
            String updateSql = "UPDATE articles SET title = ?, content = ?, category = ? WHERE id = ?";
            updateStmt = conn.prepareStatement(updateSql);
            updateStmt.setString(1, title);
            updateStmt.setString(2, content); // 存储原始 Markdown
            if (category != null && !category.trim().isEmpty()) {
                updateStmt.setString(3, category);
            } else {
                updateStmt.setNull(3, Types.VARCHAR);
            }
            updateStmt.setInt(4, articleId);

            int rowsAffected = updateStmt.executeUpdate();

            if (rowsAffected > 0) {
                out.write("{\"success\": true, \"message\": \"文章修改成功！\"}");
            } else {
                // 理论上如果前面验证通过，这里不应该发生，除非并发删除等
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(gson.toJson(new ErrorResponse("文章修改失败，未找到对应文章或无更改。")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库操作失败：" + e.getMessage())));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (checkAuthorStmt != null) checkAuthorStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (updateStmt != null) updateStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}