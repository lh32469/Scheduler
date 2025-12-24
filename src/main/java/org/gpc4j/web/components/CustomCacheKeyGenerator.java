package org.gpc4j.web.components;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.StringJoiner;

@Slf4j
@Component("customKeyGenerator")
public class CustomCacheKeyGenerator implements KeyGenerator {

  @Override
  public Object generate(Object target, Method method, Object... params) {

    StringJoiner joiner = new StringJoiner("-");

    joiner.add(method.getName());

    for (Object param : params) {
      joiner.add(param + "");
    }

    String key = joiner.toString();

    log.info("Key = " + joiner);

    return key;
  }

}