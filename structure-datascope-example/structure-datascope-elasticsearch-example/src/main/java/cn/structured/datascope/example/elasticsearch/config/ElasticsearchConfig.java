package cn.structured.datascope.example.elasticsearch.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Elasticsearch 客户端配置
 * <p>
 * 手动配置 ElasticsearchClient，使用 8.11.0 版本以兼容 ES 8.x 服务器
 * </p>
 */
@Configuration
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:}")
    private String username;

    @Value("${spring.elasticsearch.password:}")
    private String password;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        // 解析 URI
        String host = "localhost";
        int port = 9200;
        String scheme = "http";

        if (elasticsearchUri != null && !elasticsearchUri.isEmpty()) {
            String uri = elasticsearchUri.replace("http://", "").replace("https://", "");
            if (elasticsearchUri.startsWith("https")) {
                scheme = "https";
            }
            String[] parts = uri.split(":");
            host = parts[0];
            if (parts.length > 1) {
                port = Integer.parseInt(parts[1]);
            }
        }

        // 创建 RestClient
        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));

        // 设置认证
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(AuthScope.ANY,
                    new UsernamePasswordCredentials(username, password));
            builder.setHttpClientConfigCallback(httpClientBuilder ->
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        }

        RestClient restClient = builder.build();

        // 创建配置好的 ObjectMapper，支持 Java 8 时间类型
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 创建 JacksonJsonpMapper
        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);

        // 创建 Transport
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, jsonpMapper);

        // 返回 ElasticsearchClient
        return new ElasticsearchClient(transport);
    }
}