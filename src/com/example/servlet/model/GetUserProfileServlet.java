package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.sql.*;
import com.google.gson.Gson;

public class GetUserProfileServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录")));
            out.flush();
            out.close();
            return;
        }

        String userPhone = (String) session.getAttribute("userPhone");
        UserDTO userProfile = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            String sql = "SELECT phone, nickname, avatar_url FROM users WHERE phone = ?";
            pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, userPhone);
            rs = pstmt.executeQuery();

            if (rs.next()) {
                userProfile = new UserDTO(
                        rs.getString("phone"),
                        rs.getString("nickname"),
                        rs.getString("avatar_url")
                );
                out.write(gson.toJson(userProfile));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(gson.toJson(new ErrorResponse("未找到用户信息")));
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