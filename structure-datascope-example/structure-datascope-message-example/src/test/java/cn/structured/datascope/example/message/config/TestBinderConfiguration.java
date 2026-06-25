package cn.structured.datascope.example.message.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;

@TestConfiguration
@Import(TestChannelBinderConfiguration.class)
public class TestBinderConfiguration {
}