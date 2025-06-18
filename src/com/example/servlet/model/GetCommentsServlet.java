package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class GetCommentsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String articleIdParam = request.getParameter("articleId");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();

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

        List<CommentDTO> comments = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            // 如果 user_nickname 没有冗余存储，则需要 JOIN users 表获取
            // String sql = "SELECT c.id, c.article_id, c.user_phone, u.nickname as user_nickname, c.content, DATE_FORMAT(c.comment_date, '%Y-%m-%d %H:%i') as comment_date " +
            //              "FROM comments c JOIN users u ON c.user_phone = u.phone " +
            //              "WHERE c.article_id = ? ORDER BY c.comment_date DESC";
            String sql = "SELECT id, article_id, user_phone, user_nickname, content, DATE_FORMAT(comment_date, '%Y-%m-%d %H:%i') as comment_date " +
                    "FROM comments WHERE article_id = ? ORDER BY comment_date DESC";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, articleId);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                comments.add(new CommentDTO(
                        rs.getInt("id"),
                        rs.getInt("article_id"),
                        rs.getString("user_phone"),
                        rs.getString("user_nickname"),
                        rs.getString("content"),
                        rs.getString("comment_date")
                ));
            }
            out.write(gson.toJson(comments));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("获取评论失败: " + e.getMessage())));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}