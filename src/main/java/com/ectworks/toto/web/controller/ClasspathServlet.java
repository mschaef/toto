package com.ectworks.toto.controller.web;

import java.io.IOException;
import java.io.PrintWriter;

import java.net.URL;
import java.net.URLClassLoader;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ClasspathServlet extends HttpServlet
{
    void showClasspath(PrintWriter out)
    {
        Class klass = this.getClass();
        ClassLoader loader = klass.getClassLoader();

        if (!(loader instanceof java.net.URLClassLoader)) {
            out.println("Unexpected class loader: " + loader);
            return;
        }

        URLClassLoader l = (URLClassLoader)loader;

        for(URL url : l.getURLs())
            out.println("* " + url);
    }

   @Override
   protected void service(HttpServletRequest request,
                          HttpServletResponse response)
       throws ServletException, IOException
    {
        response.setContentType("text/plain");

        PrintWriter out = response.getWriter();

        showClasspath(out);
    }
}
