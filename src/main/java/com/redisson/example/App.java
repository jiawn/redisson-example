package com.redisson.example;

import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.util.concurrent.*;

/**
 * Hello world!
 */
public class App {
  static CyclicBarrier cyclicBarrier = new CyclicBarrier(2);
  static CountDownLatch countDownLatch = new CountDownLatch(1);
  public static void main(String[] args) throws Exception {
    System.out.println("Hello1");
    
    // connects to 127.0.0.1:6379 by default
    
    Config config = new Config();
    config.useSingleServer().setAddress("redis://127.0.0.1:6379");
    // 看门狗监听间隔时间
    // <b>开启看门狗后 持有锁的redisson实例关闭后锁将自动释放 但业务线程异常不会自动释放，一定要try catch 防止死锁</b>
    config.setLockWatchdogTimeout(800L);
    RedissonClient redisson = Redisson.create(config);
    // 开启两个异步线程竞争
    test(redisson);
    test(redisson);
    countDownLatch.await();
  }
  
  public static void test(RedissonClient redisson) {
    CompletableFuture.runAsync(() -> {
      RLock lock = redisson.getLock("myLock");
      try {
        while (true) {
          cyclicBarrier.await();
          if (lock.tryLock(1L, TimeUnit.SECONDS)) {
            System.out.println(Thread.currentThread().getName() + "拿到锁");
            Thread.sleep(1500L);
            System.out.println(Thread.currentThread().getName() + "释放锁");
            lock.unlock();
          }
        }
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
  
}
