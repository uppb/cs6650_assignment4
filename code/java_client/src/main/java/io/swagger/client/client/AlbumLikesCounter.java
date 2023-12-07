package io.swagger.client.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.Likes;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class AlbumLikesCounter implements Runnable{

  private int success;
  private int failure;
  private AtomicInteger success_global;
  private AtomicInteger failure_global;
  private CountDownLatch completed;
  private BlockingQueue<Record> queue;
  private ApiClient client;
  private LikeApi api;
  private static final int MAX_ATTEMPTS = 5;
  public AlbumLikesCounter(String IPAddr, BlockingQueue queue, CountDownLatch completed, AtomicInteger success, AtomicInteger failure) {
    this.success = 0;
    this.failure = 0;
    this.queue = queue;
    this.completed = completed;
    this.success_global = success;
    this.failure_global = failure;
    this.client = new ApiClient();
    this.client.setBasePath(IPAddr);
    this.api = new LikeApi(client);
  }

  public Likes getLikes(String albumID) throws ApiException {
    return api.getLikes(albumID);
  }

  public void getRuntime() throws InterruptedException{
    long start_time = System.currentTimeMillis();
    for(int i = 0; i < MAX_ATTEMPTS; i++){
      try {
        getLikes("someAlbum");
        this.success++;
        i = MAX_ATTEMPTS;
      } catch (ApiException e) {
        this.failure++;
        System.out.println(e.getMessage());
        System.err.println("Exception when calling DefaultApi#newAlbum");
        e.printStackTrace();
      }
    }
    long end_time = System.currentTimeMillis();
    Record record = new Record(start_time, end_time, "Get Count", 200);
    queue.put(record);
  }

  @Override
  public void run() {
    while(completed.getCount() > 0){
      try {
        getRuntime();
      }catch (InterruptedException e){
        System.out.println(e.getMessage());
      }
    }
    this.success_global.getAndAdd(success);
    this.failure_global.getAndAdd(failure);
    this.completed.countDown();
  }
}
