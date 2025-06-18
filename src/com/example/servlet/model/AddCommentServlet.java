package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AddCommentServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录，无法发表评论。")));
            out.flush();
            out.close();
            return;
        }

        String userPhone = (String) session.getAttribute("userPhone");
        String userNickname = (String) session.getAttribute("userNickname"); // 从会话获取昵称
        if (userNickname == null) userNickname = "匿名用户"; // 如果会话中没有昵称，给个默认值

        String articleIdParam = request.getParameter("articleId");
        String content = request.getParameter("commentContent"); // 与前端对应

        if (articleIdParam == null || content == null || content.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("文章ID和评论内容不能为空。")));
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
        PreparedStatement pstmt = null;
        ResultSet generatedKeys = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            String sql = "INSERT INTO comments (article_id, user_phone, user_nickname, content, comment_date) VALUES (?, ?, ?, ?, NOW())";
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS); // 返回生成的键

            pstmt.setInt(1, articleId);
            pstmt.setString(2, userPhone);
            pstmt.setString(3, userNickname);
            pstmt.setString(4, content);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                generatedKeys = pstmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int commentId = generatedKeys.getInt(1);
                    // 获取当前时间并格式化
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                    String commentDate = sdf.format(new Date());

                    CommentDTO newComment = new CommentDTO(commentId, articleId, userPhone, userNickname, content, commentDate);
                    // 返回成功信息和新评论的数据
                    out.write(gson.toJson(new CommentResponse(true, "评论发表成功！", newComment)));
                } else {
                    out.write(gson.toJson(new CommentResponse(true, "评论发表成功，但无法获取评论ID。", null)));
                }
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                out.write(gson.toJson(new ErrorResponse("评论发表失败，请稍后重试。")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库操作失败：" + e.getMessage())));
        } finally {
            try { if (generatedKeys != null) generatedKeys.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }
}

// 辅助类用于统一 AddCommentServlet 的响应格式
class CommentResponse {
    boolean success;
    String message;
    CommentDTO comment; // 可选，用于返回新添加的评论

    public CommentResponse(boolean success, String message, CommentDTO comment) {
        this.success = success;
        this.message = message;
        this.comment = comment;
    }
}