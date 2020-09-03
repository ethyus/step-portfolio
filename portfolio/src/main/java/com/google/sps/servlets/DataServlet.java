// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps.servlets;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.sps.data.Comment;
import com.google.gson.Gson;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.HashMap;
import java.util.Map;

@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private HashMap<String, HashMap<String, Object>> commentInfo;

  // Used in validateName() function
  private static final String regex = "^[a-zA-Z ]+$";
  private static final Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);

  @Override
  public void init() {
    commentInfo = new HashMap<String, HashMap<String, Object>>();
    commentInfo.put("messageInfo", new HashMap<String, Object>());
    commentInfo.put("nameInfo", new HashMap<String, Object>());
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Query comments from DataStore as entities
    Query query = new Query("Comment").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    List<Comment> comments = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String name = (String) entity.getProperty("name");
      String message = (String) entity.getProperty("message");
      long timestamp = (long) entity.getProperty("timestamp");

      Comment comment = new Comment(id, name, message, timestamp);
      comments.add(comment);
    }

    commentInfo.get("messageInfo").put("history", comments);

    String json = convertToJsonUsingGson(commentInfo);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    commentInfo = checkValidity(request);

    // Send invalid input to client for correction
    if (((Boolean) commentInfo.get("nameInfo").get("error"))
        || ((Boolean) commentInfo.get("messageInfo").get("error"))) {
    } else {
      // Store valid input in datastore
      String message = (String) commentInfo.get("messageInfo").get("message");
      String name = (String) commentInfo.get("nameInfo").get("name");

      storeComment("Comment", name, message);
    }

    response.sendRedirect("/index.html#comment-page");
  }

  private void storeComment(String kind, String name, String message) {
    long timestamp = System.currentTimeMillis();

    Entity commentEntity = new Entity(kind);
    commentEntity.setProperty("name", name);
    commentEntity.setProperty("message", message);
    commentEntity.setProperty("timestamp", timestamp);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(commentEntity);
  }

  private String convertToJsonUsingGson(HashMap<String, HashMap<String, Object>> messages) {
    Gson gson = new Gson();
    String json = gson.toJson(messages);
    return json;
  }

  private HashMap<String, HashMap<String, Object>> checkValidity(HttpServletRequest request) {
    HashMap<String, HashMap<String, Object>> info = new HashMap<>();
    HashMap<String, Object> message = new HashMap<>();
    HashMap<String, Object> name = new HashMap<>();

    String nameStr = request.getParameter("name");
    String messageStr = request.getParameter("message-content");

    name.put("name", nameStr);
    message.put("message", messageStr);
    name.put("error", !validateName(nameStr, pattern));
    message.put("error", validateMessage(messageStr));

    info.put("nameInfo", name);
    info.put("messageInfo", message);
    return info;
  }

  private boolean validateName(String text, Pattern pattern) {
    Matcher matcher = pattern.matcher(text);
    return matcher.find();
  }

  private boolean validateMessage(String text) {
    return text == null || text.trim().length() == 0;
  }
}
