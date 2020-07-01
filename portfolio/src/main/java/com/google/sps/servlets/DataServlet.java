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

/** Servlet that returns some example content. TODO: modify this file to handle comments data */
@WebServlet("/data")
public class DataServlet extends HttpServlet {

  private HashMap<String, HashMap<String,Object>> information;  
  private List<String> messages;

  @Override
  public void init(){
      messages = new ArrayList<>();
      information = new HashMap<String, HashMap<String,Object>>();
  }

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    String json = convertToJsonUsingGson(information);
    response.setContentType("application/json;");
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
      
      information = checkValidity(request);

      if (((Boolean) information.get("nameInfo").get("error") == true) || 
          ((Boolean) information.get("messageInfo").get("error") == true)){
          information.get("messageInfo").put("history", messages);
          response.sendRedirect("/index.html#comment-page");
      } else {
          String message = (String) information.get("messageInfo").get("message");
          messages.add(message);
          information.get("messageInfo").put("history", messages);
          response.sendRedirect("/index.html#comment-page");
      }
  }

  private String convertToJsonUsingGson(HashMap<String, HashMap<String,Object>> messages) {
    Gson gson = new Gson();
    String json = gson.toJson(messages);
    return json;
  }
  private HashMap<String, HashMap<String,Object>> checkValidity(HttpServletRequest request){

    HashMap<String, HashMap<String,Object>> info = new HashMap<>();
    HashMap<String, Object> message = new HashMap<>();
    HashMap<String, Object> name = new HashMap<>();

    String nameStr = request.getParameter("name");
    String messageStr = request.getParameter("message-content");
    
    name.put("name", nameStr);
    message.put("message", messageStr);
    System.out.println(info);
    if (!validateName(nameStr)){
        name.put("error", true);
    } else {
        name.put("error", false);
    }
    if (validateMessage(messageStr)){
        message.put("error", true);
    } else {
        message.put("error", false);
    }

    info.put("nameInfo", name);
    info.put("messageInfo", message);

    return info;
  }
  private boolean validateName(String text){
      String regex = "^[a-zA-Z ]+$";
      Pattern pattern =  Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(text); 
      return matcher.find(); 
  }
  private boolean validateMessage(String text){
      return text == null || text.trim().length() == 0;
  }
}
