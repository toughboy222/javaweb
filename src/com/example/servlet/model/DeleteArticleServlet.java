package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class DeleteArticleServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录，无法删除文章。")));
            out.flush();
            out.close();
            return;
        }
        String currentUserPhone = (String) session.getAttribute("userPhone");
        String articleIdParam = request.getParameter("articleId");

        if (articleIdParam == null || articleIdParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("文章ID不能为空。")));
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
        PreparedStatement deleteStmt = null;
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
                    out.write(gson.toJson(new ErrorResponse("您无权删除此文章。")));
                    out.flush();
                    out.close();
                    return;
                }
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 404
                out.write(gson.toJson(new ErrorResponse("要删除的文章不存在。")));
                out.flush();
                out.close();
                return;
            }
            rs.close();
            checkAuthorStmt.close();

            // 2. 删除文章
            String deleteSql = "DELETE FROM articles WHERE id = ?";
            deleteStmt = conn.prepareStatement(deleteSql);
            deleteStmt.setInt(1, articleId);

            int rowsAffected = deleteStmt.executeUpdate();

            if (rowsAffected > 0) {
                out.write("{\"success\": true, \"message\": \"文章删除成功！\"}");
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND); // 可能已被删除或ID错误
                out.write(gson.toJson(new ErrorResponse("文章删除失败，可能已被删除或ID无效。")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库操作失败：" + e.getMessage())));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (checkAuthorStmt != null) checkAuthorStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (deleteStmt != null) deleteStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}