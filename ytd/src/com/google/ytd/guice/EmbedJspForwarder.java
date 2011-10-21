package com.google.ytd.guice;

import java.io.IOException;

import javax.jdo.PersistenceManagerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class EmbedJspForwarder extends HttpServlet {
  @Inject
  private PersistenceManagerFactory pmf = null;

  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    try {
      getServletContext().setAttribute("pmf", pmf);
      getServletContext().getRequestDispatcher(req.getRequestURI() + ".jsp").forward(req, resp);
    } catch (ServletException e) {
      e.printStackTrace();
    }
  }
}