package cn.structured.datascope.example.message.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

@Configuration
public class StreamConfig {

    @Bean
    public MessageConverter jackson2MessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setSerializedPayloadClass(byte[].class);
        converter.setStrictContentTypeMatch(false);
        return converter;
    }
}