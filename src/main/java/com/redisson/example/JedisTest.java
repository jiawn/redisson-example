package com.redisson.example;

import redis.clients.jedis.Jedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class JedisTest {
  static CountDownLatch countDownLatch = new CountDownLatch(1);
  static AtomicInteger state = new AtomicInteger(0);
  static AtomicInteger err = new AtomicInteger(0);
  static AtomicInteger count = new AtomicInteger(0);
  
  public static void main(String[] args) throws InterruptedException {
    //连接redis服务器(在这里是连接本地的)
    Jedis jedis = new Jedis("127.0.0.1", 6379);
    System.out.println("连接成功");
    //100个线程进程竞争
    for (int i = 0; i < 100; i++) {
      test(jedis);
    }
    countDownLatch.await();
  }
  
  
  public static void test(Jedis jedis) {
    new Thread(() -> {
      System.out.println("-----");
      while (true) {
        // 测试两步上锁共同持有发生率
        if (!jedis.exists("jedis")) {
          if (state.get() == 1) {
            err.getAndAdd(1);
            System.out.println("加锁次数" + count + "失败次数" + err.get());
          }
          state.set(1);
          //// NX是不存在时才set， XX是存在时才set， EX是秒，PX是毫秒
          jedis.set("jedis", "chx", "NX", "PX", 1000L);
          System.out.println(Thread.currentThread().getName() + "设置锁");
          try {
            Thread.sleep(800L);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          System.out.println(Thread.currentThread().getName() + "释放锁");
          state.set(0);
          jedis.del("jedis");
        }
      }
    });
  }
}
