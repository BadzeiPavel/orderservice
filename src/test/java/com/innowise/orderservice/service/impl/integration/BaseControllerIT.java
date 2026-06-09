package com.innowise.orderservice.service.impl.integration;

import com.innowise.orderservice.service.impl.integration.config.RestTemplateTestConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

@Import(RestTemplateTestConfig.class)
public abstract class BaseControllerIT extends BaseIntegrationTest {

  @LocalServerPort
  protected int port;

  @Autowired
  protected TestRestTemplate restTemplate;

  protected String baseUrl() {
    return "http://localhost:" + port;
  }

  protected <T> HttpEntity<T> createEntity(T body) {
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    return new HttpEntity<>(body, headers);
  }
}