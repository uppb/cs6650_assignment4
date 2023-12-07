package com.example.java_servlet;

import redis.clients.jedis.JedisPool;

public class CacheConfig {
  private static JedisPool pool = new JedisPool(Constants.REDIS_HOST, 6379);

  public static JedisPool getPool(){return pool;}
}
