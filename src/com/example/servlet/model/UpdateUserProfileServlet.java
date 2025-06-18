package com.example.servlet.model;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.MultipartConfig; // 重要：用于文件上传
import java.io.*;
import java.nio.file.Paths; // 用于安全地获取文件名
import java.sql.*;
import java.util.UUID; // 用于生成唯一文件名
import com.google.gson.Gson;

@MultipartConfig(
        fileSizeThreshold = 1024 * 1024 * 1, // 1 MB
        maxFileSize = 1024 * 1024 * 5,   // 5 MB
        maxRequestSize = 1024 * 1024 * 10 // 10 MB
)
public class UpdateUserProfileServlet extends HttpServlet {

    private static final String UPLOAD_DIR = "uploads/avatars";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");

        PrintWriter out = response.getWriter();
        Gson gson = new Gson();
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("userPhone") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            out.write(gson.toJson(new ErrorResponse("用户未登录，无法更新资料")));
            out.flush();
            out.close();
            return;
        }

        String userPhone = (String) session.getAttribute("userPhone"); // 移到这里，确保后续可用
        String newNickname = request.getParameter("nickname");

        // 昵称校验
        if (newNickname == null || newNickname.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("昵称不能为空")));
            out.flush();
            out.close();
            return;
        }
        if (newNickname.trim().length() < 2 || newNickname.trim().length() > 20) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            out.write(gson.toJson(new ErrorResponse("昵称长度应为2-20个字符")));
            out.flush();
            out.close();
            return;
        }

        Connection conn = null;
        PreparedStatement pstmtUpdate = null;
        PreparedStatement pstmtFetchOldAvatar = null;
        ResultSet rsOldAvatar = null;
        String oldAvatarRelativePath = null;
        String newAvatarRelativePath = null;
        String applicationPath = null; // 在 try 块外部声明

        try {
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/sch?useUnicode=true&characterEncoding=UTF-8", "juejin_user", "Shichenghao222@");
            conn.setAutoCommit(false); // 开始事务

            applicationPath = getServletContext().getRealPath(""); // 在 try 块开始处获取应用真实路径

            // 0. 获取旧头像路径，以便后续删除
            String fetchOldAvatarSql = "SELECT avatar_url FROM users WHERE phone = ?";
            pstmtFetchOldAvatar = conn.prepareStatement(fetchOldAvatarSql);
            pstmtFetchOldAvatar.setString(1, userPhone);
            rsOldAvatar = pstmtFetchOldAvatar.executeQuery();
            if (rsOldAvatar.next()) {
                oldAvatarRelativePath = rsOldAvatar.getString("avatar_url");
            }
            // 立即关闭以释放资源，避免在同一个 try 块中持有太久
            if (rsOldAvatar != null) {
                try {
                    rsOldAvatar.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            if (pstmtFetchOldAvatar != null) {
                try {
                    pstmtFetchOldAvatar.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }


            // 1. 处理头像文件上传
            Part filePart = request.getPart("avatarFile"); // "avatarFile" 对应前端 input file 的 name
            if (filePart != null && filePart.getSize() > 0) {
                String submittedFileName = filePart.getSubmittedFileName(); // 获取原始文件名
                // 安全地获取文件名，防止路径遍历攻击
                String fileName = Paths.get(submittedFileName).getFileName().toString();
                String fileExtension = "";
                int i = fileName.lastIndexOf('.');
                if (i > 0 && i < fileName.length() - 1) { // 确保'.'不是第一个或最后一个字符
                    fileExtension = fileName.substring(i + 1).toLowerCase();
                } else {
                    // 如果没有扩展名，或者扩展名不合法，可以拒绝或给一个默认的
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write(gson.toJson(new ErrorResponse("无效的文件名或缺少扩展名。")));
                    if (conn != null) conn.rollback();
                    out.flush();
                    out.close();
                    return;
                }


                // 简单校验文件类型
                if (!fileExtension.matches("jpg|jpeg|png|gif")) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    out.write(gson.toJson(new ErrorResponse("仅支持 JPG, PNG, GIF 格式的头像。")));
                    if (conn != null) conn.rollback();
                    out.flush();
                    out.close();
                    return;
                }

                String uniqueFileName = UUID.randomUUID().toString() + "." + fileExtension;

                String uploadFilePath = applicationPath + File.separator + UPLOAD_DIR;

                File uploadDir = new File(uploadFilePath);
                if (!uploadDir.exists()) {
                    if (!uploadDir.mkdirs()) { // 尝试创建目录
                        throw new IOException("无法创建上传目录: " + uploadFilePath);
                    }
                }

                // 保存文件
                filePart.write(uploadFilePath + File.separator + uniqueFileName);
                newAvatarRelativePath = UPLOAD_DIR.replace(File.separator, "/") + "/" + uniqueFileName; // 统一使用'/'作为路径分隔符存储到数据库
            }


            // 2. 更新数据库
            String sqlUpdate;
            if (newAvatarRelativePath != null) { // 如果上传了新头像
                sqlUpdate = "UPDATE users SET nickname = ?, avatar_url = ? WHERE phone = ?";
            } else { // 只更新昵称
                sqlUpdate = "UPDATE users SET nickname = ? WHERE phone = ?";
            }
            pstmtUpdate = conn.prepareStatement(sqlUpdate);
            pstmtUpdate.setString(1, newNickname.trim());
            if (newAvatarRelativePath != null) {
                pstmtUpdate.setString(2, newAvatarRelativePath);
                pstmtUpdate.setString(3, userPhone);
            } else {
                pstmtUpdate.setString(2, userPhone);
            }

            int rowsAffected = pstmtUpdate.executeUpdate();

            if (rowsAffected > 0) {
                conn.commit(); // 提交事务

                // 如果成功上传了新头像，并且旧头像存在，则删除旧头像文件
                if (newAvatarRelativePath != null && oldAvatarRelativePath != null && !oldAvatarRelativePath.isEmpty()) {
                    // 确保 applicationPath 不是 null
                    if (applicationPath != null) {
                        File oldAvatarFile = new File(applicationPath + File.separator + oldAvatarRelativePath.replace("/", File.separator));
                        if (oldAvatarFile.exists()) {
                            if (oldAvatarFile.delete()) {
                                System.out.println("旧头像删除成功: " + oldAvatarFile.getPath());
                            } else {
                                System.err.println("旧头像删除失败: " + oldAvatarFile.getPath());
                            }
                        }
                    } else {
                        System.err.println("applicationPath 为 null，无法删除旧头像。");
                    }
                }

                // 更新 Session 中的昵称和头像URL
                session.setAttribute("userNickname", newNickname.trim());
                if (newAvatarRelativePath != null) {
                    session.setAttribute("userAvatarUrl", newAvatarRelativePath);
                } else if (oldAvatarRelativePath != null) { // 如果没有新头像，但之前有，保持session中的旧头像
                    session.setAttribute("userAvatarUrl", oldAvatarRelativePath);
                } else {
                    session.removeAttribute("userAvatarUrl");
                }

                String effectiveAvatarUrl = (newAvatarRelativePath != null) ? newAvatarRelativePath : oldAvatarRelativePath;
                out.write(gson.toJson(new UpdateProfileResponse(true, "个人资料更新成功！", newNickname.trim(), effectiveAvatarUrl)));

            } else {
                conn.rollback();
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                out.write(gson.toJson(new ErrorResponse("更新失败，未找到用户或资料无变化")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("数据库操作失败: " + e.getMessage())));
        } catch (Exception e) {
            e.printStackTrace();
            if (conn != null) try {
                conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            out.write(gson.toJson(new ErrorResponse("更新过程中发生错误: " + e.getMessage())));
        } finally {
            // rsOldAvatar 和 pstmtFetchOldAvatar 已经在 try 块内关闭
            try {
                if (pstmtUpdate != null) pstmtUpdate.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
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

    static class UpdateProfileResponse {
        boolean success;
        String message;
        String nickname;
        String avatarUrl;

        public UpdateProfileResponse(boolean success, String message, String nickname, String avatarUrl) {
            this.success = success;
            this.message = message;
            this.nickname = nickname;
            this.avatarUrl = avatarUrl;
        }
    }
}
