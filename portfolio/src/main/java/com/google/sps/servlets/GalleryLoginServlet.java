package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.google.gson.Gson;

@WebServlet("/loginForGallery")
public class GalleryLoginServlet extends HttpServlet {

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    UserService userService = UserServiceFactory.getUserService();
    if (userService.isUserLoggedIn()) {
      String userEmail = userService.getCurrentUser().getEmail();
      String urlToRedirectToAfterUserLogsOut = "/imageGallery.html";
      String logoutUrl = userService.createLogoutURL(urlToRedirectToAfterUserLogsOut);
      String jsonLogOutInfo = convertToJson("null", logoutUrl, userEmail, true);
      response.setContentType("application/json;");
      
      // Return Json file with logout URL
      response.getWriter().println(jsonLogOutInfo);

    } else {
      String urlToRedirectToAfterUserLogsIn = "/imageGallery.html";
      String loginUrl = userService.createLoginURL(urlToRedirectToAfterUserLogsIn);
      String jsonLoginInfo = convertToJson(loginUrl, "null", "null", false);
      response.setContentType("application/json;");

      // Return Json file with login URL
      response.getWriter().println(jsonLoginInfo);
    }
  }

  public class User {
      private String loginUrl;
      private String logoutUrl;
      private String email;
      private boolean showForm;
      
      public User(String loginUrl, String logoutUrl, String email, boolean showForm) {
          this.loginUrl = loginUrl;
          this.logoutUrl = logoutUrl;
          this.email = email;
          this.showForm = showForm;
      }
  }

  private String convertToJson(String loginUrl, String logoutUrl, String email, boolean showForm) {
    Gson gson = new Gson();
    User user = new User(loginUrl, logoutUrl, email, showForm);
    String json = gson.toJson(user);
    return json;
  }
}