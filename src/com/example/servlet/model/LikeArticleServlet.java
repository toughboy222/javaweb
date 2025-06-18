package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class LikeArticleServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录，无法点赞。")));
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
        PreparedStatement checkLikeStmt = null;
        PreparedStatement actionStmt = null;
        PreparedStatement updateCountStmt = null;
        ResultSet rs = null;
        boolean currentlyLiked = false;
        int newLikesCount = 0;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            conn.setAutoCommit(false); // 开始事务

            // 1. 检查用户是否已经点赞
            String checkLikeSql = "SELECT COUNT(*) FROM article_likes WHERE article_id = ? AND user_phone = ?";
            checkLikeStmt = conn.prepareStatement(checkLikeSql);
            checkLikeStmt.setInt(1, articleId);
            checkLikeStmt.setString(2, currentUserPhone);
            rs = checkLikeStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                currentlyLiked = true;
            }
            rs.close();
            checkLikeStmt.close();

            // 2. 执行点赞/取消点赞操作
            if (currentlyLiked) { // 取消点赞
                String deleteLikeSql = "DELETE FROM article_likes WHERE article_id = ? AND user_phone = ?";
                actionStmt = conn.prepareStatement(deleteLikeSql);
                actionStmt.setInt(1, articleId);
                actionStmt.setString(2, currentUserPhone);
                actionStmt.executeUpdate();

                String updateDecrementSql = "UPDATE articles SET likes_count = GREATEST(0, likes_count - 1) WHERE id = ?";
                updateCountStmt = conn.prepareStatement(updateDecrementSql);
                updateCountStmt.setInt(1, articleId);
                updateCountStmt.executeUpdate();
            } else { // 添加点赞
                String insertLikeSql = "INSERT INTO article_likes (article_id, user_phone, like_date) VALUES (?, ?, NOW())";
                actionStmt = conn.prepareStatement(insertLikeSql);
                actionStmt.setInt(1, articleId);
                actionStmt.setString(2, currentUserPhone);
                actionStmt.executeUpdate();

                String updateIncrementSql = "UPDATE articles SET likes_count = likes_count + 1 WHERE id = ?";
                updateCountStmt = conn.prepareStatement(updateIncrementSql);
                updateCountStmt.setInt(1, articleId);
                updateCountStmt.executeUpdate();
            }
            conn.commit(); // 提交事务

            // 3. 获取最新的点赞数
            PreparedStatement getCountStmt = conn.prepareStatement("SELECT likes_count FROM articles WHERE id = ?");
            getCountStmt.setInt(1, articleId);
            rs = getCountStmt.executeQuery();
            if (rs.next()) {
                newLikesCount = rs.getInt("likes_count");
            }
            rs.close();
            getCountStmt.close();

            out.write(gson.toJson(new LikeResponse(true, !currentlyLiked, newLikesCount, currentlyLiked ? "取消点赞成功" : "点赞成功")));

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback(); // 回滚事务
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库操作失败：" + e.getMessage())));
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (actionStmt != null) actionStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (updateCountStmt != null) updateCountStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            try { if (checkLikeStmt != null) checkLikeStmt.close(); } catch (SQLException e) { e.printStackTrace(); }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // 恢复自动提交
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    // 辅助类用于统一 LikeArticleServlet 的响应格式
    private static class LikeResponse {
        boolean success;
        boolean liked; // true表示操作后是点赞状态，false表示未点赞状态
        int likesCount;
        String message;

        public LikeResponse(boolean success, boolean liked, int likesCount, String message) {
            this.success = success;
            this.liked = liked;
            this.likesCount = likesCount;
            this.message = message;
        }
    }
}