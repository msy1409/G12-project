package com.g12.visitor.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一 LocalDateTime 的 JSON 格式为 yyyy-MM-dd HH:mm:ss。
 * 只追加这两个序列化器，不动其余默认模块。
 */
@Configuration
public class JacksonConfig {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(FMT));
            builder.deserializers(new LocalDateTimeDeserializer(FMT));
            builder.simpleDateFormat("yyyy-MM-dd HH:mm:ss");
        };
    }
}
