package org.ticketbooking.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class KafkaThreadPoolConfig {

    @Bean
    public ThreadPoolTaskExecutor kafkaExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(7);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("KafkaProcessor-");
        executor.initialize();
        return executor;
    }

}
