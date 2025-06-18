package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class GetRankedAuthorsServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        List<AuthorRankDTO> rankedAuthors = new ArrayList<>();
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            // 查询作者获得的总点赞数，并按总点赞数降序排列
            // COALESCE 用于优先显示昵称，如果昵称为空则显示手机号
            // LIMIT 5 表示取排名前5的作者
            String sql = "SELECT " +
                    "    COALESCE(u.nickname, a.author_phone) as author_identifier, " +
                    "    SUM(a.likes_count) as total_article_likes " +
                    "FROM articles a " +
                    "LEFT JOIN users u ON a.author_phone = u.phone " +
                    "WHERE a.author_phone IS NOT NULL AND a.author_phone != '' " + // 确保作者存在
                    "GROUP BY a.author_phone, u.nickname " +
                    "ORDER BY total_article_likes DESC, author_identifier ASC " + // 按总点赞数降序，点赞数相同则按作者标识升序
                    "LIMIT 5"; // 你可以调整这个 LIMIT 数量

            pstmt = conn.prepareStatement(sql);
            rs = pstmt.executeQuery();

            while (rs.next()) {
                String authorIdentifier = rs.getString("author_identifier");
                int totalLikes = rs.getInt("total_article_likes");
                rankedAuthors.add(new AuthorRankDTO(authorIdentifier, totalLikes));
            }
            out.write(gson.toJson(rankedAuthors));

        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("获取作者榜失败: " + e.getMessage())));
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