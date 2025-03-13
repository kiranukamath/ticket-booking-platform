package org.ticketbooking.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaThreadPoolConfig {

    // @Bean
    // public ThreadPoolTaskExecutor kafkaExecutor() {
    // ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    // executor.setCorePoolSize(4);
    // executor.setMaxPoolSize(7);
    // executor.setQueueCapacity(50);
    // executor.setThreadNamePrefix("KafkaProcessor-");
    // executor.initialize();
    // return executor;
    // }

    @Bean
    public ExecutorService kafkaExecutor() {
        return Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("KafkaConsumer-", 0).factory());
    }

}
