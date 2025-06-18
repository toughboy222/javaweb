package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class GetArticleByIdServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        String idParam = request.getParameter("id");
        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false); // 获取会话，不创建新的
        String currentUserPhone = (session != null) ? (String) session.getAttribute("userPhone") : null;


        if (idParam == null || idParam.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("文章ID不能为空")));
            out.flush();
            out.close();
            return;
        }

        int articleId;
        try {
            articleId = Integer.parseInt(idParam);
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("无效的文章ID格式")));
            out.flush();
            out.close();
            return;
        }

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ArticleDTO article = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            // 查询时获取完整 content, 作者昵称, 点赞数
            String sql = "SELECT a.id, a.title, a.content, a.likes_count, " +
                    "a.author_phone, u.nickname as author_nickname, " +
                    "DATE_FORMAT(a.publish_date, '%Y-%m-%d %H:%i') as publish_date, a.category " +
                    "FROM articles a " +
                    "LEFT JOIN users u ON a.author_phone = u.phone " +
                    "WHERE a.id = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setInt(1, articleId);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                String authorNickname = rs.getString("author_nickname");
                String authorPhone = rs.getString("author_phone");
                String authorDisplay = (authorNickname != null && !authorNickname.isEmpty()) ? authorNickname : authorPhone;
                if (authorDisplay == null) authorDisplay = "匿名";

                article = new ArticleDTO(
                        rs.getInt("id"),
                        rs.getString("title"),
                        null,
                        rs.getString("content"),
                        authorDisplay,
                        rs.getString("publish_date"),
                        rs.getString("category"),
                        rs.getInt("likes_count") // 获取点赞数
                );

                // 检查当前用户是否点赞了该文章
                if (currentUserPhone != null) {
                    String checkLikedSql = "SELECT COUNT(*) FROM article_likes WHERE article_id = ? AND user_phone = ?";
                    PreparedStatement checkLikedStmt = conn.prepareStatement(checkLikedSql);
                    checkLikedStmt.setInt(1, articleId);
                    checkLikedStmt.setString(2, currentUserPhone);
                    ResultSet likedRs = checkLikedStmt.executeQuery();
                    if (likedRs.next() && likedRs.getInt(1) > 0) {
                        article.setLikedByCurrentUser(true);
                    }
                    likedRs.close();
                    checkLikedStmt.close();
                }

                out.write(gson.toJson(article));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(gson.toJson(new ErrorResponse("未找到指定ID的文章")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库查询失败: " + e.getMessage())));
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