package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

public class SearchArticleServlet extends HttpServlet {

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8"); //
        response.setContentType("application/json;charset=UTF-8"); //
        response.setHeader("Access-Control-Allow-Origin", "*"); //

        String query = request.getParameter("query"); //
        PrintWriter out = response.getWriter(); //
        Gson gson = new Gson(); //

        if (query == null || query.trim().isEmpty()) { //
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST); //
            out.write(gson.toJson(new ErrorResponse("搜索词不能为空"))); //
            out.flush(); //
            out.close(); //
            return; //
        }

        Connection conn = null; //
        PreparedStatement pstmt = null; //
        ResultSet rs = null; //
        List<ArticleDTO> articles = new ArrayList<>(); //

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@"); //
            String sql = "SELECT a.id, a.title, LEFT(a.content, 200) as content_snippet, a.likes_count, " + // 添加 a.likes_count
                    "a.author_phone, u.nickname as author_nickname, " +
                    "DATE_FORMAT(a.publish_date, '%Y-%m-%d %H:%i') as publish_date, a.category " +
                    "FROM articles a LEFT JOIN users u ON a.author_phone = u.phone " +
                    "WHERE a.title LIKE ? OR a.content LIKE ? ORDER BY a.publish_date DESC"; //
            pstmt = conn.prepareStatement(sql); //
            String searchQuery = "%" + query + "%"; //
            pstmt.setString(1, searchQuery); //
            pstmt.setString(2, searchQuery); //

            rs = pstmt.executeQuery(); //

            while (rs.next()) { //
                String snippet = rs.getString("content_snippet"); //
                String displaySnippet = (snippet == null) ? "" : (snippet + (snippet.length() == 200 ? "..." : "")); //

                String authorNickname = rs.getString("author_nickname"); //
                String authorPhone = rs.getString("author_phone"); //
                String authorDisplay = (authorNickname != null && !authorNickname.isEmpty()) ? authorNickname : authorPhone; //
                if (authorDisplay == null) authorDisplay = "匿名"; //

                articles.add(new ArticleDTO( //
                        rs.getInt("id"), //
                        rs.getString("title"), //
                        displaySnippet, // contentSnippet for list view
                        null,           // fullContent is null for list view
                        authorDisplay,  // authorIdentifier
                        rs.getString("publish_date"), //
                        rs.getString("category"), //
                        rs.getInt("likes_count") // 填充点赞数
                ));
            }
            out.write(gson.toJson(articles)); //

        } catch (SQLException e) {
            e.printStackTrace(); //
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR); //
            out.write(gson.toJson(new ErrorResponse("数据库查询失败: " + e.getMessage()))); //
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException e) { e.printStackTrace(); } //
            try { if (pstmt != null) pstmt.close(); } catch (SQLException e) { e.printStackTrace(); } //
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); } //
            if (out != null) { //
                out.flush(); //
                out.close(); //
            }
        }
    }
}