package com.example.rabbitmq_consumer;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class MQConsumer {

  private final static String QUEUE_NAME = "reaction";
  private static final String consumerTag = "reactionConsumer";
  private final static int NUM_OF_CONSUMERS = 40;
  private static Connection connection;
  private static Channel[] channels;
  private static ExecutorService executorService;


  public static void main (String[] args){
    System.out.println("Starting");
    ConnectionFactory rmqFactory = new ConnectionFactory();
    try {
      rmqFactory.setUri(Constants.RABBITMQ_HOST);
      rmqFactory.setUsername(Constants.RABBITMQ_USERNAME);
      rmqFactory.setPassword(Constants.RABBITMQ_PASSWORD);
    } catch (Exception e) {
      System.err.println("MQ: "+ e.getMessage());
    }
    try{
      connection = rmqFactory.newConnection();
    }catch (IOException | TimeoutException e){
      System.err.println("MQ Connection:" + e.getMessage());
    }
    channels = new Channel[NUM_OF_CONSUMERS];
    executorService = Executors.newFixedThreadPool(NUM_OF_CONSUMERS);

    try {
      for(int i = 0; i < NUM_OF_CONSUMERS; i++){
        Channel curChannel = connection.createChannel();
        channels[i] = curChannel;
        executorService.submit(() -> {
          try {
            boolean autoAck = true;
            curChannel.basicConsume(QUEUE_NAME, autoAck, consumerTag, new MessageConsumer(curChannel));
          } catch (IOException e) {
            System.err.println(e.getMessage());
          }
        });
      }
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.out.println("Shutdown Hook is running !");
      try {
        for(int i = 0; i < channels.length; i++){
          if (channels[i] != null && channels[i].isOpen()) {
            channels[i].basicCancel(consumerTag);
            channels[i].close();
          }
        }
        if (connection != null && connection.isOpen()) {
          connection.close();
        }
      } catch (IOException | TimeoutException e) {
        System.err.println(e.getMessage());
      }
    }));
    executorService.shutdown();
  }
}