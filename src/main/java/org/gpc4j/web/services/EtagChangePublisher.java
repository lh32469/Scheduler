package org.gpc4j.web.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class EtagChangePublisher {

  private final Map<String, List<SseEmitter>> emitterMap = new ConcurrentHashMap<>();

//  private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

  public SseEmitter subscribe(@NonNull String topic) {
    SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

    List<SseEmitter> sseEmitters = emitterMap
        .computeIfAbsent(topic, k -> new LinkedList<>());

    sseEmitters.add(emitter);

    emitter.onCompletion(() -> {
      if (sseEmitters.remove(emitter)) {
        log.debug("Emitter {} has been closed (Complete T)", emitter);
      } else {
        log.debug("Emitter {} has been closed (Complete F)", emitter);
      }
      log.debug(sseEmitters.size() + " emitters remaining for topic {}", topic);
    });

    emitter.onTimeout(() -> {
      sseEmitters.remove(emitter);
      log.debug("Emitter {} has been closed (Timeout)", emitter);
    });

//    emitter.onError((e) -> {
//      if (sseEmitters.remove(emitter)) {
//        log.info("Emitter {} has been closed (Error T)", emitter);
//      } else {
//        log.info("Emitter {} has been closed (Error F)", emitter);
//      }
//      log.info(sseEmitters.size() + " emitters remaining for topic {}", topic);
//    });

    log.debug("New SSE client subscribed to {}. Total clients: {}",
             topic, emitterMap.get(topic).size());
    return emitter;
  }

  public void publish(String topic, String etag) {

    // To avoid ConcurrentModificationException
    List<SseEmitter> emitters = new LinkedList<>(emitterMap.get(topic));

    log.debug("Publishing ETag update: {}:{}. Total clients: {}",
             topic, etag, emitters.size());

    for (SseEmitter emitter : emitters) {
      try {
        emitter.send(SseEmitter.event()
                               .name("etag-update")
                               .data(etag));

      } catch (IOException e) {
        log.debug("Failed to send ETag update to emitter " + emitter);
      }
    }

  }

  public Set<String> getTopics() {
    return emitterMap.keySet();
  }

}
