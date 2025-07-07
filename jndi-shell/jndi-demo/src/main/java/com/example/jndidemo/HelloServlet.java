package com.example.jndidemo;

import javax.naming.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;

@WebServlet(name = "helloServlet", value = "/hello")
public class HelloServlet extends HttpServlet {
    public void doGet(HttpServletRequest request, HttpServletResponse response) {
        String name = request.getParameter("name");
        try {
            InitialContext context = new InitialContext();
            context.lookup(name);
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }
}