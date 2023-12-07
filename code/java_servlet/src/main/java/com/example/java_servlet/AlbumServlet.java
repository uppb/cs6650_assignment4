package com.example.java_servlet;

import com.example.java_servlet.model.AlbumInfo;
import com.example.java_servlet.model.AlbumsProfile;
import com.example.java_servlet.model.ErrorMsg;
import com.example.java_servlet.model.ImageMetaData;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.sql.Connection;
import java.sql.PreparedStatement;
import org.apache.commons.dbcp2.BasicDataSource;

@WebServlet(urlPatterns = "/albums/*")
@MultipartConfig
public class AlbumServlet extends HttpServlet {
  private Gson gson;

  private BasicDataSource dataSource;

  private String retrieval_sql;
  private String insert_sql;


  private void sendResponse(HttpServletResponse resp, String message) throws IOException{
    PrintWriter out = resp.getWriter();
    out.print(message);
    out.flush();
  }

  private void sendErrorResponse(HttpServletResponse resp, String message) throws IOException {
    ErrorMsg msg = new ErrorMsg();
    msg.setMsg(message);
    sendResponse(resp, gson.toJson(msg));
  }

  private String extractJSON(String s){
    int startIndex = s.indexOf('{');
    int endIndex = s.lastIndexOf('}');
    if (!(startIndex != -1 && endIndex != -1 && endIndex > startIndex)) {
      return "";
    }
    String trimmed = s.substring(startIndex+1, endIndex).trim();
    Pattern pattern = Pattern.compile("(\\w+):\\s*([^:]+)(?:\\s|$)");
    Matcher matcher = pattern.matcher(trimmed);
    StringBuilder jsonSb = new StringBuilder("{");
    while(matcher.find()) {
      String key = matcher.group(1).trim();
      String value = matcher.group(2).trim();

      jsonSb.append(String.format("\"%s\": \"%s\"", key, value));

      if (trimmed.indexOf(matcher.group(0)) + matcher.group(0).length() < trimmed.length()) {
        jsonSb.append(", ");
      }
    }
    jsonSb.append("}");

    return jsonSb.toString();
  }

  private AlbumsProfile handleProfile(Part req, HttpServletResponse resp) throws IOException{
    BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()));
    try{
      StringBuilder sb = new StringBuilder();
      String s;
      while((s=br.readLine()) != null){
        sb.append(s);
      }
      String json = extractJSON(sb.toString());
      if(json.isEmpty()){
        throw new Exception("Cannot parse Json Request");
      }
      AlbumsProfile profile = (AlbumsProfile) gson.fromJson(json, AlbumsProfile.class);
      return profile;
    }catch (Exception ex){
      sendErrorResponse(resp, ex.getMessage());
    }
    return null;
  }

  @Override
  public void init() {
    gson = new Gson();
    dataSource = DatabaseConfig.getDataSource();
    insert_sql = "INSERT INTO albums (image_content, profile_data) VALUES (?, ?)";
    retrieval_sql = "SELECT profile_data FROM albums WHERE id = ?";
  }

  @Override
  public void destroy() {
    try{
      dataSource.close();
    }catch (SQLException e){
      System.err.println("Error when closing connection pool");
      e.printStackTrace();
    }
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");

    //Handle parts
    Part profile_request = req.getPart("profile");
    AlbumsProfile profile = handleProfile(profile_request, resp);
    Part image_request = req.getPart("image");
    long image_size = image_request.getSize();

    InputStream image_stream = image_request.getInputStream();
    String profile_json = gson.toJson(profile);
    long generatedKey = -1;
    //Insert into database
    long start = System.currentTimeMillis();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insert_sql, Statement.RETURN_GENERATED_KEYS);){
      pstmt.setBinaryStream(1, image_stream, image_size);
      pstmt.setString(2, profile_json);
      pstmt.executeUpdate();
      //Get Generated Key
      try(ResultSet tableKeys = pstmt.getGeneratedKeys();) {
        if (tableKeys.next()) {
          generatedKey = tableKeys.getLong(1);
        }else{
          sendErrorResponse(resp, "Error when inserting to Database");
        }
      }
    }catch(SQLException e){
      System.err.println("POST ERROR:" + e.getMessage());

    }
    long end = System.currentTimeMillis();
    //System.out.println("Post time: " + (end-start));
    image_stream.close();
    //Send Response
    if(generatedKey == -1){
      sendErrorResponse(resp, "Database Error");
      return;
    }
    ImageMetaData message = new ImageMetaData();
    message.setAlbumID(String.valueOf(generatedKey));
    message.setImageSize(String.valueOf(image_size));
    sendResponse(resp, gson.toJson(message));
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    String pathInfo = req.getPathInfo();
    if(pathInfo == null){
      sendErrorResponse(resp, "Invalid Request");
    }else{
      String[] parts = pathInfo.substring(1).split("/");
      if(parts.length > 1){
        sendErrorResponse(resp, "Invalid Request");
      }else{
        try(Connection conn = dataSource.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(retrieval_sql);){
          int albumID = Integer.parseInt(parts[0]);

          pstmt.setInt(1, albumID);
          try (ResultSet rs = pstmt.executeQuery();){
            if (rs.next()) {
              // Deserialize the AlbumsProfile object from JSON or any other format
              AlbumInfo retrievedInfo = gson.fromJson(rs.getString("profile_data"), AlbumInfo.class);
              sendResponse(resp, gson.toJson(retrievedInfo));
            }else{
              sendErrorResponse(resp, "Album ID: " + albumID + " Not Found");
            }
          }
        }catch (NumberFormatException ex){
          System.err.println(ex.getMessage());
        }catch (SQLException ex){
          System.err.println("GET ERROR: " + ex.getMessage());
        }
      }
    }
  }
}
