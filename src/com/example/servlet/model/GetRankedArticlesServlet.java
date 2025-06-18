package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class GetRankedArticlesServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        List<ArticleDTO> rankedArticles = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            // 查询点赞数最高的前 N 篇文章 (例如前5篇)
            // 同时按发布日期降序作为次要排序条件，保证点赞数相同时新文章靠前
            String sql = "SELECT id, title, likes_count " +
                    "FROM articles " +
                    "ORDER BY likes_count DESC, publish_date DESC " +
                    "LIMIT 5"; // 你可以调整这个 LIMIT 数量
            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                ArticleDTO article = new ArticleDTO();
                article.setId(rs.getInt("id"));
                article.setTitle(rs.getString("title"));
                article.setLikesCount(rs.getInt("likes_count"));
                // 对于排行榜，我们可能不需要其他所有字段
                rankedArticles.add(article);
            }
            out.write(gson.toJson(rankedArticles));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("获取热门文章榜失败: " + e.getMessage())));
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