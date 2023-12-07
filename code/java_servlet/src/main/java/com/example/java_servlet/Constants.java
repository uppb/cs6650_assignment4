package com.example.java_servlet;

public class Constants {
  private static final boolean local = false;
  public static final String RABBITMQ_HOST;
  public static final String RABBITMQ_USERNAME;
  public static final String RABBITMQ_PASSWORD;
  public static final String DB_URL;
  public static final String DB_USERNAME;
  public static final String DB_PASSWORD;
  public static final String REDIS_HOST;

  static{
    RABBITMQ_USERNAME = "cs6650";
    RABBITMQ_PASSWORD = "cs6650cs6650";
    DB_USERNAME = "cs6650";
    DB_PASSWORD = "cs6650";
    if(local){
      RABBITMQ_HOST = "amqp://localhost:5672/%2F";
      DB_URL = "jdbc:mysql://localhost:3306/cs6650";
      REDIS_HOST = "localhost";
    }else{
      RABBITMQ_HOST = "amqps://b-eafdf15d-c0cb-4b8b-8eaa-b069f34c8e2c.mq.us-west-2.amazonaws.com:5671";
      DB_URL = "jdbc:mysql://cs6650.c6xff3divtzz.us-west-2.rds.amazonaws.com:3306/cs6650";
      REDIS_HOST = "cs6650-redis.6jfwen.ng.0001.usw2.cache.amazonaws.com:6379cs6650-redis.6jfwen.ng.0001.usw2.cache.amazonaws.com";
    }
  }
}
