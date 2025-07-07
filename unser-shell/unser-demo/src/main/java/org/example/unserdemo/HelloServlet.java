package org.example.unserdemo;

import java.io.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = "/hello-servlet")
@MultipartConfig
public class HelloServlet extends HttpServlet {
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        try {
            Part filePart = request.getPart("file");  // "file"对应表单中input的name
            InputStream fileContent = filePart.getInputStream();
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[1024];
            while ((nRead = fileContent.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(buffer.toByteArray()));
            response.setContentType("text/plain");
            response.getWriter().println("success");
            ois.readObject();
            ois.close();
        } catch (Exception e) {
            throw new IOException("文件上传失败: " + e.getMessage());
        }
    }

    public void destroy() {
    }
}