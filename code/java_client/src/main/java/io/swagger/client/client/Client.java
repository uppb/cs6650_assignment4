package io.swagger.client.client;

import io.swagger.client.ApiClient;
import io.swagger.client.ApiException;
import io.swagger.client.api.DefaultApi;
import java.awt.SystemTray;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Client {
  private static String image;
  private static String output_path;
  private static String IPAddress;
  private static void initializate(int initialNumThreads, String IPAddr){
    ExecutorService executorService = Executors.newFixedThreadPool(initialNumThreads);
    for(int i = 0; i < initialNumThreads; i++){
      Runnable thread = () -> {
        ApiClient client = new ApiClient();
        client.setBasePath(IPAddr);
        DefaultApi apiInstance = new DefaultApi(client);
        try {
          String id = Producer.sendPost(apiInstance, image);
        }catch (ApiException e){
          System.err.println(e.getMessage());
        }
      };
      executorService.submit(thread);
    }
    executorService.shutdown();
    try {
      executorService.awaitTermination(30, TimeUnit.SECONDS);
      System.out.println("Finished Initializing");
    } catch (InterruptedException e) {
      System.err.println("Initialization Timed Out");
    }
  }
  private static void startLoading(int threadGroupSize, int numThreadGroups, int delay, String IPAddr)
      throws InterruptedException {
    BlockingQueue<Record> queue = new LinkedBlockingQueue<>(100);
    CountDownLatch completed = new CountDownLatch(threadGroupSize*numThreadGroups);
    AtomicInteger post_success = new AtomicInteger(0);
    AtomicInteger post_failure = new AtomicInteger(0);
    AtomicInteger get_success = new AtomicInteger(0);
    AtomicInteger get_failure = new AtomicInteger(0);


    ExecutorService executorService = Executors.newFixedThreadPool(200);
    long startTime = System.currentTimeMillis();
    long get_startTime = 0;

    Consumer consumer = new Consumer(output_path, queue, completed);
    executorService.submit(consumer);
    boolean started_counter = false;
    for(int i = 0; i < numThreadGroups; i++){
      if(i > 0 && !started_counter){
        get_startTime = System.currentTimeMillis();
        for(int j = 0; j < 3; j++){
          AlbumLikesCounter counter = new AlbumLikesCounter(IPAddr, queue, completed, get_success, get_failure);
          executorService.submit(counter);
        }
        started_counter = true;
      }
      for(int j = 0; j < threadGroupSize; j++) {
        Producer producer = new Producer(IPAddr, image, queue, completed, post_success, post_failure);
        executorService.submit(producer);
      }
      Thread.sleep(1000L*delay);
    }
    completed.await();
    executorService.shutdown();
    try{
      executorService.awaitTermination(30, TimeUnit.SECONDS);
    }catch (InterruptedException e){
      System.out.println("Failed to stop all threads");
    }
    long endTime = System.currentTimeMillis();
    int post_requests = post_success.get() + post_failure.get();
    int get_requests = get_success.get() + get_failure.get();
    double wallTime = (double) ((endTime - startTime) - 1000L * delay * numThreadGroups) /1000L;
    double get_wallTime = (double) ((endTime - get_startTime) - 1000L * delay * (numThreadGroups-1)) / 1000L;
    double post_throughput = post_requests / wallTime;
    double get_throughput = get_requests / get_wallTime;
    System.out.println("Successful Post Requests: " + post_success.get());
    System.out.println("Failed Post Requests: " + post_failure.get());
    System.out.println("Successful Get Requests: " + get_success.get());
    System.out.println("Failed Get Requests: " + get_failure.get());
    System.out.println("WallTime: " + wallTime);
    System.out.println("Get WallTime: " + get_wallTime);
    System.out.println("Post Throughput: " + post_throughput);
    System.out.println("Get Throughput: " + get_throughput);
  }

  private static void findStatsfromArrayList(ArrayList<Double> list){
    Collections.sort(list);
    int n = list.size();
    double min = list.get(0);
    double max = list.get(n-1);
    double mean = list.stream().mapToDouble(val -> val).average().orElse(0.0);
    double median;
    if (n % 2 == 0) {
      median = (list.get((n / 2) - 1) + list.get(n / 2)) / 2.0;
    } else {
      median = list.get(n / 2);
    }
    int index = (int) Math.ceil((99.0 / 100.0) * n) - 1;
    double p99 =  list.get(index);
    System.out.println("min: " + min);
    System.out.println("max: " + max);
    System.out.println("mean: " + mean);
    System.out.println("median: " + median);
    System.out.println("p99: " + p99);
  }

  private static void calculateStats(String csv_file){
    ArrayList<Double> post_latency = new ArrayList<>();
    ArrayList<Double> reaction_latency = new ArrayList<>();
    ArrayList<Double> reaction_count_latency = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(csv_file))) {
      String line;
      while ((line = br.readLine()) != null) {
        String[] values = line.split(",");
        double latency = Double.parseDouble(values[1]);
        String type = values[2];
        if (type.equals("New Album")) {
          post_latency.add(latency);
        } else if (type.equals("Reaction")) {
          reaction_latency.add(latency);
        } else {
          reaction_count_latency.add(latency);
        }
      }
    }catch (Exception e){
      e.printStackTrace();
    }
    System.out.println("Response Time Stats for New Album Requests: ");
    findStatsfromArrayList(post_latency);
    System.out.println("Response Time Stats for New Reaction Requests: ");
    findStatsfromArrayList(reaction_latency);
    System.out.println("Response Time Stats for Get Reaction Count Requests: ");
    findStatsfromArrayList(reaction_count_latency);
  }

  public static void main(String[] args){
    image = "/Users/sunny/Downloads/nmtb.png";
    output_path = "/Users/sunny/Downloads/resources.csv";
    //IPAddress = "http://cs6650-balancer-1431079427.us-west-2.elb.amazonaws.com:8080/java_servlet-1.0-SNAPSHOT/";
    IPAddress = "http://54.202.17.101:8080/java_servlet-1.0-SNAPSHOT/";
    //IPAddress = "http://localhost:8080/java_servlet_war_exploded/";
    try{
      initializate(10, IPAddress);
      startLoading(10,30, 2, IPAddress);
      calculateStats(output_path);
    } catch (Exception e){
      e.printStackTrace();
    }
  }
}
