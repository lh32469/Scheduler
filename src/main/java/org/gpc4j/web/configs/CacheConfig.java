package org.gpc4j.web.configs;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

  public static final String CLASS_LIST = "classList";

  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    // Defaults
    cacheManager.setCacheSpecification("maximumSize=100,expireAfterWrite=10m");

    // Register caches with custom specs
    cacheManager.registerCustomCache("documentStore",
                                     Caffeine.newBuilder()
                                             .expireAfterWrite(5, TimeUnit.MINUTES)
                                             .maximumSize(100)
                                             .build());

    cacheManager.registerCustomCache("classList",
                                     Caffeine.newBuilder()
                                             .expireAfterWrite(15, TimeUnit.MINUTES)
                                             .maximumSize(100)
                                             .build());

    cacheManager.registerCustomCache("holidays",
                                     Caffeine.newBuilder()
                                             .expireAfterWrite(24, TimeUnit.HOURS)
                                             .maximumSize(100)
                                             .build());

    return cacheManager;
  }

}