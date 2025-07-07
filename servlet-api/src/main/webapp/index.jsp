<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html>
<head>
    <title>JSP - Hello World</title>
    <style>a{display: block;}</style>
</head>
<body>
<h1><%= "Hello World!" %>
</h1>
<a href="hello-servlet">Hello-Servlet</a>
<a href="servlet-inject.jsp">注入Servlet内存马</a>
<a href="servlet-shell?cmd=whoami">测试Servlet内存马</a>
<a href="filter-inject.jsp">注入Filter内存马</a>
<a href="hello-servlet?cmd=whoami">测试Filter内存马</a>
<a href="listener-inject.jsp">注入Listener内存马</a>
<a href="hello-servlet?cmd=whoami">测试Listener内存马</a>
<a href="listener-inject.jsp">注入Valve内存马</a>
<a href="hello-servlet?cmd=whoami">测试Valve内存马</a>
</body>
</html>