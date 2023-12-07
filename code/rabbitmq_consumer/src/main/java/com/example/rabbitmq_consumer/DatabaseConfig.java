package com.example.rabbitmq_consumer;
import org.apache.commons.dbcp2.BasicDataSource;

public class DatabaseConfig {
  private static BasicDataSource dataSource = new BasicDataSource();

  private static void init() {
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
    dataSource.setUrl(Constants.DB_URL);
    dataSource.setUsername(Constants.DB_USERNAME);
    dataSource.setPassword(Constants.DB_PASSWORD);

    dataSource.setInitialSize(15); // initial number of connections
    dataSource.setMaxTotal(45);   // max number of connections
    dataSource.setMaxIdle(30);     // max number of idle connections
    dataSource.setMinIdle(15);     // min number of idle connections
    dataSource.setMaxWaitMillis(30000);  // timeout 30 seconds
  }

  static{
    init();
  }

  public static BasicDataSource getDataSource(){
    return dataSource;
  }
}





