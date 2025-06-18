package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;

public class PublishArticleServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*"); // 允许跨域

        String title = request.getParameter("title");
        String content = request.getParameter("content");
        String category = request.getParameter("category");

        // TODO: 获取当前登录用户的手机号 (author_phone)
        // 这通常需要从 HTTP Session 中获取。例如:
         HttpSession session = request.getSession(false); // false表示不创建新session
         String authorPhone = null;
         if (session != null && session.getAttribute("userPhone") != null) {
             authorPhone = (String) session.getAttribute("userPhone");
         } else {
             // 用户未登录或会话超时，可以拒绝发布或作为匿名文章处理
             response.getWriter().write("{\"success\": false, \"message\": \"用户未登录，无法发布文章\"}");
             return;
         }


        PrintWriter out = response.getWriter();

        if (title == null || title.trim().isEmpty() || content == null || content.trim().isEmpty()) {
            out.write("{\"success\": false, \"message\": \"文章标题和内容不能为空\"}");
            out.flush();
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            String sql = "INSERT INTO articles (title, content, author_phone, category, publish_date) VALUES (?, ?, ?, ?, NOW())";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, title);
            pstmt.setString(2, content);

            if (authorPhone != null && !authorPhone.isEmpty()) {
                pstmt.setString(3, authorPhone);
            } else {
                pstmt.setNull(3, Types.VARCHAR); // 如果允许匿名文章
            }

            if (category != null && !category.trim().isEmpty()) {
                pstmt.setString(4, category);
            } else {
                pstmt.setNull(4, Types.VARCHAR);
            }

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                out.write("{\"success\": true, \"message\": \"文章发布成功！\"}");
            } else {
                out.write("{\"success\": false, \"message\": \"文章发布失败，请稍后重试。\"}");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            out.write("{\"success\": false, \"message\": \"数据库操作失败：" + e.getMessage() + "\"}");
        } finally {
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (out != null) {
                out.flush();
            }
        }
    }
}