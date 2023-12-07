package io.swagger.client.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import io.swagger.client.api.LikeApi;
import io.swagger.client.model.AlbumsProfile;
import io.swagger.client.model.ImageMetaData;
import java.io.File;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Producer implements Runnable{
  private String IPAddr;
  private String image_file;
  private BlockingQueue<Record> queue;
  private CountDownLatch completed;
  private int success;
  private int failure;

  private AtomicInteger success_global;
  private AtomicInteger failure_global;
  private ApiClient client;
  private DefaultApi apiInstance;
  private LikeApi likeApiInstance;

  private static final int MAX_ATTEMPTS = 5;
  public Producer(String IPAddr,
      String image_file, BlockingQueue queue, CountDownLatch completed, AtomicInteger success, AtomicInteger failure) {
    this.IPAddr = IPAddr;
    this.image_file = image_file;
    this.queue = queue;
    this.completed = completed;
    this.success_global = success;
    this.failure_global = failure;
    this.client = new ApiClient();
    client.setBasePath(IPAddr);
    this.apiInstance = new DefaultApi(client);
    this.likeApiInstance = new LikeApi(client);
  }

  public static String sendPost(DefaultApi api, String img) throws ApiException {
    File image = new File(img);
    AlbumsProfile profile = new AlbumsProfile();
    profile.setArtist("Me");
    profile.setTitle("Some Title");
    profile.setYear("2018");
    ImageMetaData result = api.newAlbum(image, profile);
    return result.getAlbumID();
  }

  public void sendReaction(String albumID, String reaction) throws ApiException {
    this.likeApiInstance.review(reaction, albumID);
  }


  private String getRuntime(String albumID, String reaction) throws InterruptedException{
    long start_time = System.currentTimeMillis();
    String id = "-1";
    if(albumID == null) {
      for(int i = 0; i < MAX_ATTEMPTS; i++) {
        try {
          id = sendPost(this.apiInstance, this.image_file);
          this.success++;
          i = this.MAX_ATTEMPTS;
        } catch (ApiException e) {
          this.failure++;
          System.out.println(e.getMessage());
          System.err.println("Exception when calling DefaultApi#newAlbum");
          e.printStackTrace();
        }
      }
    }else{
      for(int i = 0; i < MAX_ATTEMPTS; i++) {
        try {
          sendReaction(albumID, reaction);
          this.success++;
          i = this.MAX_ATTEMPTS;
        } catch (ApiException e) {
          this.failure++;
          System.err.println("Exception when calling LikeApi#review");
          e.printStackTrace();
        }
      }
    }
    long end_time = System.currentTimeMillis();
    Record record = new Record(start_time, end_time, albumID == null ? "New Album" : "Reaction", 200);
    this.queue.put(record);
    return id;
  }

  @Override
  public void run(){
    for(int i = 0; i < 100; i++) {
      try {
        String id = getRuntime( null, null);
        if(id == null){
          throw new InterruptedException();
        }
        getRuntime( id, "like");
        getRuntime( id, "like");
        getRuntime( id, "dislike");
      } catch (InterruptedException e) {
        System.out.println(e.getMessage());
      }
    }
    this.success_global.getAndAdd(success);
    this.failure_global.getAndAdd(failure);
    this.completed.countDown();
  }
}
