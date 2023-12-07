package com.example.java_servlet;

import com.example.java_servlet.model.ErrorMsg;
import com.example.java_servlet.model.Likes;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import org.apache.commons.dbcp2.BasicDataSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@WebServlet(urlPatterns = "/review/*")
public class ReviewServlet extends HttpServlet {
  private final static String QUEUE_NAME = "reaction";
  private Gson gson;
  private ConnectionFactory rmqFactory;
  private BasicDataSource dataSource;
  private JedisPool cachePool;
  private Connection connection;
  private ExecutorService execService;
  private final int NUM_THREADS = 10;
  private BlockingQueue<Likes> prefetch_queue;
  private String retrieval_sql;

  @Override
  public void init() throws ServletException {
    this.rmqFactory = new ConnectionFactory();
    try {
      this.rmqFactory.setUri(Constants.RABBITMQ_HOST);
      this.rmqFactory.setUsername(Constants.RABBITMQ_USERNAME);
      this.rmqFactory.setPassword(Constants.RABBITMQ_PASSWORD);
    } catch (Exception e) {
      System.err.println(e.getMessage());
    }
    this.execService = Executors.newFixedThreadPool(NUM_THREADS);
    this.dataSource = DatabaseConfig.getDataSource();
    this.cachePool = CacheConfig.getPool();
    this.prefetch_queue = new LinkedBlockingQueue<>(1000);
    this.retrieval_sql = "SELECT SUM(CASE WHEN reaction = 'like' THEN 1 ELSE 0 END) AS total_likes, SUM(CASE WHEN reaction = 'dislike' THEN 1 ELSE 0 END) AS total_dislikes FROM album_reactions WHERE albumID = ?;";
    this.gson = new Gson();
    try{
      this.connection = rmqFactory.newConnection();
    }catch (IOException | TimeoutException e){
      System.err.println(e.getMessage());
    }
    startPrefetch();
  }

  @Override
  public void destroy() {
    super.destroy();
    try {
      dataSource.close();
      if (this.connection != null && this.connection.isOpen()) {
        this.connection.close();
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    } catch (SQLException e){
      System.err.println("Error when closing connection pool");
      e.printStackTrace();
    }
  }

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

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    String pathInfo = req.getPathInfo();
    if (pathInfo == null || pathInfo.isEmpty()) {
      sendErrorResponse(resp, "Invalid Request");
      return;
    }
    String[] parts = pathInfo.substring(1).split("/");
    if (parts.length > 2) {
      sendErrorResponse(resp, "Invalid Request");
      return;
    }
    HashMap<String, String> messageMap = new HashMap<>();
    messageMap.put("albumId", parts[1]);
    messageMap.put("reaction", parts[0]);
    publishToQueue(gson.toJson(messageMap));
    sendResponse(resp, "Added reaction");
  }

  /**
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    String pathInfo = req.getPathInfo();
    if (pathInfo == null || pathInfo.isEmpty()) {
      sendErrorResponse(resp, "Invalid Request");
      return;
    }
    String[] parts = pathInfo.substring(1).split("/");
    if (parts.length > 1) {
      sendErrorResponse(resp, "Invalid Request");
      return;
    }
    try(java.sql.Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(retrieval_sql);){
      int albumID = Integer.parseInt(parts[0]);

      pstmt.setInt(1, albumID);
      try (ResultSet rs = pstmt.executeQuery();){
        if (rs.next()) {
          int likes = rs.getInt("total_likes");
          int dislikes = rs.getInt("total_dislikes");
          Likes response = new Likes();
          response.setLikes(String.valueOf(likes));
          response.setDislikes(String.valueOf(dislikes));
          sendResponse(resp, gson.toJson(response));
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
  **/

  /**
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    try(Jedis jedis = cachePool.getResource()){
      String randomKey = jedis.randomKey();
      Map<String, String> redis_result = jedis.hgetAll(randomKey);
      Likes response = new Likes();
      response.setLikes(redis_result.get("like"));
      response.setDislikes(redis_result.get("dislike"));
      sendResponse(resp, gson.toJson(response));
    }catch (Exception e){
      sendErrorResponse(resp, e.getMessage());
      System.out.println(e.getMessage());
    }
  }
  **/

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    resp.setContentType("application/json");
    resp.setCharacterEncoding("UTF-8");
    Likes response = prefetch_queue.poll();
    sendResponse(resp, gson.toJson(response));
  }

  private void publishToQueue(String message){
    try(Channel channel = this.connection.createChannel();){
      channel.queueDeclare(QUEUE_NAME, true, false, false, null);
      channel.basicPublish("", QUEUE_NAME, null, message.getBytes("UTF-8"));
    } catch (IOException | TimeoutException e) {
      System.out.println(e);
    }
  }

  private void startPrefetch(){
    for(int i = 0; i < NUM_THREADS; i++){
      Runnable prefetchThread = () -> {
        try(Jedis jedis = cachePool.getResource()){
          String randomKey = jedis.randomKey();
          Map<String, String> redis_result = jedis.hgetAll(randomKey);
          Likes response = new Likes();
          response.setLikes(redis_result.get("like"));
          response.setDislikes(redis_result.get("dislike"));
          prefetch_queue.put(response);
        }catch(Exception e){
          System.out.println(e.getMessage());
        }
      };
    }
  }
}
