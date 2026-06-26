package com.innowise.orderservice;

import com.innowise.commonstarter.config.kafka.KafkaTopics;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableFeignClients
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableConfigurationProperties(KafkaTopics.class)
@SpringBootApplication
public class OrderserviceApplication {

  public static void main(String[] args) {
    SpringApplication.run(OrderserviceApplication.class, args);
  }

}
