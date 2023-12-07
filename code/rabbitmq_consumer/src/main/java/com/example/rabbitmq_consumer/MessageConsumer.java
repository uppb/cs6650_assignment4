package com.example.rabbitmq_consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import org.apache.commons.dbcp2.BasicDataSource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class MessageConsumer extends DefaultConsumer{

  private final String insert_sql = "INSERT INTO album_reactions (albumID, reaction) VALUES (?, ?)";

  private Gson gson = new Gson();

  public MessageConsumer(Channel channel) {
    super(channel);
  }
  @Override
  public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties,
      byte[] body) throws IOException {
    String messageJson = new String(body, "UTF-8");
    Map<String, String> messageMap = gson.fromJson(messageJson, Map.class);
    String albumId = messageMap.get("albumId");
    String reaction = messageMap.get("reaction");
    //saveToDB(albumId, reaction);
    saveToCache(albumId, reaction);
  }

  private void saveToCache(String albumID, String reaction){
    JedisPool pool = CacheConfig.getPool();
    try (Jedis jedis = pool.getResource()) {
      jedis.hincrBy(albumID, reaction, 1);
    }catch(Exception e){
      System.out.println(e.getMessage());
      e.printStackTrace();
    }
  }

  private void saveToDB(String albumID, String reaction){
    BasicDataSource dataSource = DatabaseConfig.getDataSource();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement pstmt = conn.prepareStatement(insert_sql)){
      pstmt.setString(1, albumID);
      pstmt.setString(2, reaction);
      pstmt.executeUpdate();
    }catch(SQLException e){
      System.err.println("ERROR INSERTING TO Database:" + e.getMessage());

    }
  }

}
