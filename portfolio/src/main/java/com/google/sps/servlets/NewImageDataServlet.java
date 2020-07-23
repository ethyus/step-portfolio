package com.google.sps.servlets;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.blobstore.BlobInfo;
import com.google.appengine.api.blobstore.BlobInfoFactory;
import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.images.ImagesService;
import com.google.appengine.api.images.ImagesServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.sps.data.Comment;
import com.google.sps.data.Image;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
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

/** **/
@WebServlet("/newimage")
public class NewImageDataServlet extends HttpServlet {

  private HashMap<String, List<Image>> usersImages;

  @Override
  public void init() {
    usersImages = new HashMap<>();
  }
  
  // Get users' images from datastore
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    UserService userService = UserServiceFactory.getUserService();
    
    // Only query for users' images if logged in
    List<Image> images = new ArrayList<>();
    if (userService.isUserLoggedIn()) {
        String userEmail = userService.getCurrentUser().getEmail();
        
        Query query = new Query("Image").setFilter(new FilterPredicate("email", FilterOperator.EQUAL, userEmail)).addSort("timestamp", SortDirection.DESCENDING);

        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        PreparedQuery results = datastore.prepare(query);

        for (Entity entity: results.asIterable()) {
            long id = entity.getKey().getId();
            String name = (String) entity.getProperty("name");
            String blobKey = (String) entity.getProperty("blobKey");
            long timestamp = (long) entity.getProperty("timestamp");
            String email = (String) entity.getProperty("email");

            Image image = new Image(id, name, blobKey, timestamp, email);
            images.add(image);
        }

        usersImages.put("userImages", images);
        String json = convertToJsonUsingGson(usersImages);
        response.setContentType("application/json;");
        response.getWriter().println(json);
    }
  }
  
  // users post their own images
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
    PrintWriter out = response.getWriter();
    UserService userService = UserServiceFactory.getUserService();

    // Only logged-in users can post messages
    if (!userService.isUserLoggedIn()) {
      response.sendRedirect("/imageGallery.html");
      return;
    }

    String blobKey = getBlobKey(request, "image");
    String email = userService.getCurrentUser().getEmail();

    if (blobKey == null) {
    out.println("Please upload an image file.");
    return;
    }

    //Store in DataStore
    String user = request.getParameter("user");
    storeImage("Image", user, blobKey, email);

    response.sendRedirect("/imageGallery.html");
  } 

  private void storeImage(String kind, String user, String blobKey, String email) {
    long timestamp= System.currentTimeMillis();

    Entity imageEntity = new Entity(kind);
    imageEntity.setProperty("name", user);
    imageEntity.setProperty("blobKey", blobKey);
    imageEntity.setProperty("timestamp", timestamp);
    imageEntity.setProperty("email", email);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(imageEntity);
  }

  private String getBlobKey(HttpServletRequest request, String formInputElementName) {
    BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(request);
    List<BlobKey> blobKeys = blobs.get("image");

    // User submitted form without selecting a file, so we can't get a BlobKey. (dev server)
    if (blobKeys == null || blobKeys.isEmpty()) {
      return null;
    }

    // Our form only contains a single file input, so get the first index.
    BlobKey blobKey = blobKeys.get(0);

    // User submitted form without selecting a file, so the BlobKey is empty. (live server)
    BlobInfo blobInfo = new BlobInfoFactory().loadBlobInfo(blobKey);
    if (blobInfo.getSize() == 0) {
      blobstoreService.delete(blobKey);
      return null;
    }
    return blobKey.getKeyString();
  }

  private String convertToJsonUsingGson(HashMap<String, List<Image>> messages) {
    Gson gson = new Gson();
    String json = gson.toJson(messages);
    return json;
  }


}