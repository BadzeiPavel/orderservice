package com.innowise.orderservice.service.impl.integration.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.StringHttpMessageConverter;

@TestConfiguration
public class RestTemplateTestConfig {

  @Bean
  public RestTemplateCustomizer restTemplateCustomizer() {
    return restTemplate -> {
      restTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter());
    };
  }
}