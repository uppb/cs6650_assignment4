package com.example.rabbitmq_consumer;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
public class CacheConfig {
  private static JedisPool pool = new JedisPool(Constants.REDIS_HOST, 6379);

  public static JedisPool getPool(){return pool;}
}
