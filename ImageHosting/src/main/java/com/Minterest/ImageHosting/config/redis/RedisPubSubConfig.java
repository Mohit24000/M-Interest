package com.Minterest.ImageHosting.config.redis;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisPubSubConfig {
    @Bean
    ChannelTopic likeTopic(){
        return new ChannelTopic("like");
    }

    @Bean
    ChannelTopic commentTopic(){
        return new ChannelTopic("comment");
    }

    @Bean
    ChannelTopic followTopic(){
        return new ChannelTopic("follow");
    }
    @Bean
    public RedisMessageListenerContainer config(
            RedisConnectionFactory factory,
            RedisSubscriber subscriber,
            ChannelTopic likeTopic,
            ChannelTopic commentTopic,
            ChannelTopic followTopic) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(factory);

        container.addMessageListener(subscriber, likeTopic);
        container.addMessageListener(subscriber, commentTopic);
        container.addMessageListener(subscriber, followTopic);

        return container;
    }
}
