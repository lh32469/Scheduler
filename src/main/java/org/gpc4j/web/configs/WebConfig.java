package org.gpc4j.web.configs;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/styles.css")
        .addResourceLocations("classpath:/static/")
        .setCacheControl(CacheControl.maxAge(1, TimeUnit.DAYS)
                                     .cachePublic());
  }

//  @Bean
//  public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
//
//    FilterRegistrationBean<ShallowEtagHeaderFilter>
//        filterRegistrationBean =
//        new FilterRegistrationBean<>(
//            new ShallowEtagHeaderFilter());
//
//    filterRegistrationBean.addUrlPatterns("/*");
//
//    return filterRegistrationBean;
//  }

}