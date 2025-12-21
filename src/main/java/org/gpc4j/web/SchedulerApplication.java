package org.gpc4j.web;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

@SpringBootApplication
@EnableCaching
public class SchedulerApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchedulerApplication.class, args);
  }

  @Bean
  public CacheManager cacheManager() {

    CaffeineCacheManager cacheManager =
        new CaffeineCacheManager("documentStore");

    cacheManager.setCaffeine(Caffeine.newBuilder()
                                     .expireAfterWrite(10, TimeUnit.MINUTES)
                                     .maximumSize(100));
    return cacheManager;
  }

}
