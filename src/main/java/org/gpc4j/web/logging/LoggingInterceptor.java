package org.gpc4j.web.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * LoggingInterceptor: logs HTTP request and response headers and bodies.
 * <p>
 * Implemented as a OncePerRequestFilter to ensure we can wrap the request/response
 * with content-caching wrappers and safely read bodies after the downstream processing.
 */
@Component
public class LoggingInterceptor extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

  // Max bytes of body to log to avoid excessive output
  private static final int MAX_PAYLOAD_LENGTH = 8 * 1024; // 8 KB

  // Headers that should not be logged verbatim
  private static final Set<String> SENSITIVE_HEADERS = new HashSet<>(Arrays.asList(
      HttpHeaders.AUTHORIZATION.toLowerCase(),
      HttpHeaders.COOKIE.toLowerCase(),
      "set-cookie"
  ));

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    boolean isFirstRequest = !isAsyncDispatch(request);

    HttpServletRequest requestToUse = request;
    HttpServletResponse responseToUse = response;

    if (isFirstRequest && !(request instanceof ContentCachingRequestWrapper)) {
      requestToUse = new ContentCachingRequestWrapper(request);
    }
    if (!(response instanceof ContentCachingResponseWrapper)) {
      responseToUse = new ContentCachingResponseWrapper(response);
    }

    long start = System.currentTimeMillis();
    try {
      filterChain.doFilter(requestToUse, responseToUse);
    } finally {
      long durationMs = System.currentTimeMillis() - start;
      logExchange((HttpServletRequest) requestToUse, (HttpServletResponse) responseToUse,
          durationMs);
      // Important: copy body back to the real response
      if (responseToUse instanceof ContentCachingResponseWrapper ccrw) {
        ccrw.copyBodyToResponse();
      }
    }
  }

  private void logExchange(HttpServletRequest request, HttpServletResponse response,
                           long durationMs) {
    try {
      String method = request.getMethod();
      String uri = request.getRequestURI();
      String query = request.getQueryString();
      String fullUrl = uri + (query != null ? ("?" + query) : "");

      // Request headers
      Map<String, String> reqHeaders = collectHeaders(request);
      String reqBody = extractRequestBody(request);

      int status = response.getStatus();
      Map<String, String> respHeaders = collectHeaders(response);
      String respBody = extractResponseBody(response);

      log.info(
          "HTTP {} {} ({} ms)\n> Headers: {}\n> Body: {}\n< Status: {}\n< Headers: " +
              "{}\n< Body: {}",
          method, fullUrl, durationMs,
          reqHeaders,
          abbreviate(reqBody),
          status,
          respHeaders,
          abbreviate(respBody)
      );
    } catch (Exception e) {
      log.warn("Failed to log request/response: {}", e.toString());
    }
  }

  private Map<String, String> collectHeaders(HttpServletRequest request) {
    Map<String, String> map = new LinkedHashMap<>();
    Enumeration<String> names = request.getHeaderNames();
    while (names != null && names.hasMoreElements()) {
      String name = names.nextElement();
      String value = String.join(", ", Collections.list(request.getHeaders(name)));
      if (isSensitive(name)) {
        value = redact(value);
      }
      map.put(name, value);
    }
    return map;
  }

  private Map<String, String> collectHeaders(HttpServletResponse response) {
    Map<String, String> map = new LinkedHashMap<>();
    for (String name : response.getHeaderNames()) {
      String value = String.join(", ", response.getHeaders(name));
      if (isSensitive(name)) {
        value = redact(value);
      }
      map.put(name, value);
    }
    return map;
  }

  private boolean isSensitive(String headerName) {
    return headerName != null && SENSITIVE_HEADERS.contains(headerName.toLowerCase());
  }

  private String redact(String value) {
    if (!StringUtils.hasText(value)) {
      return value;
    }
    return "***redacted***";
  }

  @Nullable
  private String extractRequestBody(HttpServletRequest request) {
    if (!(request instanceof ContentCachingRequestWrapper wrapper)) {
      return null;
    }
    byte[] buf = wrapper.getContentAsByteArray();
    if (buf.length == 0) {
      return null;
    }
    if (!isTextLike(request.getContentType())) {
      return "[non-textual body omitted]";
    }
    return toString(buf, request.getCharacterEncoding());
  }

  @Nullable
  private String extractResponseBody(HttpServletResponse response) {
    if (!(response instanceof ContentCachingResponseWrapper wrapper)) {
      return null;
    }
    byte[] buf = wrapper.getContentAsByteArray();
    if (buf.length == 0) {
      return null;
    }
    String contentType = response.getContentType();
    if (!isTextLike(contentType)) {
      return "[non-textual body omitted]";
    }
    return toString(buf, response.getCharacterEncoding());
  }

  private String toString(byte[] buf, String encoding) {
    int length = Math.min(buf.length, MAX_PAYLOAD_LENGTH);
    Charset charset = null;
    if (StringUtils.hasText(encoding)) {
      try {
        charset = Charset.forName(encoding);
      } catch (Exception ignored) {
      }
    }
    if (charset == null) {
      charset = StandardCharsets.UTF_8;
    }
    return new String(buf, 0, length, charset);
  }

  private boolean isTextLike(@Nullable String contentType) {
    if (!StringUtils.hasText(contentType)) {
      return true; // assume textual if unknown
    }
    try {
      MediaType mt = MediaType.parseMediaType(contentType);
      return MediaType.TEXT_PLAIN.isCompatibleWith(mt)
          || MediaType.APPLICATION_JSON.isCompatibleWith(mt)
          || MediaType.APPLICATION_XML.isCompatibleWith(mt)
          || MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(mt)
          || ("application".equals(mt.getType()) && mt.getSubtype().endsWith("+json"))
          || ("application".equals(mt.getType()) && mt.getSubtype().endsWith("+xml"));
    } catch (Exception e) {
      return false;
    }
  }

  private String abbreviate(@Nullable String s) {
    if (s == null) {
      return null;
    }
    int max = 2000;
    return s.length() > max ? s.substring(0, max) + "...[truncated]" : s;
  }

}
