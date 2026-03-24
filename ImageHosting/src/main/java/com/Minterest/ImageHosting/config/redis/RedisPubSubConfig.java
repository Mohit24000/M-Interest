package com.Minterest.ImageHosting.config.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer(mapper));
        return template;
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
