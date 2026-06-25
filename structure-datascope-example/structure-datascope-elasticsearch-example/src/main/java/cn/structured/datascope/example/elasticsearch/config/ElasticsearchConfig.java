package cn.structured.datascope.example.elasticsearch.config;

//import co.elastic.clients.elasticsearch.ElasticsearchClient;
//import co.elastic.clients.json.jackson.JacksonJsonpMapper;
//import co.elastic.clients.transport.ElasticsearchTransport;
//import co.elastic.clients.transport.rest_client.RestClientTransport;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
//import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
//import org.apache.http.HttpHost;
//import org.apache.http.auth.AuthScope;
//import org.apache.http.auth.UsernamePasswordCredentials;
//import org.apache.http.impl.client.BasicCredentialsProvider;
//import org.elasticsearch.client.RestClient;
//import org.elasticsearch.client.RestClientBuilder;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch 配置
 * <p>
 * 注释中的配置为兼容低版本或其他版本的Elasticsearch 需要直接使用 ElasticsearchClient 来读写数据
 * </p>
 *
 * @author chen.ma
 * @date 2022/03/05 21:07
 */
@Configuration
@EnableElasticsearchRepositories
public class ElasticsearchConfig {
//
//    @Value("${spring.elasticsearch.uris:http://localhost:9200}")
//    private String elasticsearchUri;
//
//    @Value("${spring.elasticsearch.username:}")
//    private String username;
//
//    @Value("${spring.elasticsearch.password:}")
//    private String password;
//
//    @Bean
//    public ElasticsearchClient elasticsearchClient() {
//        // 解析 URI
//        String host = "localhost";
//        int port = 9200;
//        String scheme = "http";
//
//        if (elasticsearchUri != null && !elasticsearchUri.isEmpty()) {
//            String uri = elasticsearchUri.replace("http://", "").replace("https://", "");
//            if (elasticsearchUri.startsWith("https")) {
//                scheme = "https";
//            }
//            String[] parts = uri.split(":");
//            host = parts[0];
//            if (parts.length > 1) {
//                port = Integer.parseInt(parts[1]);
//            }
//        }
//
//        // 创建 RestClient
//        RestClientBuilder builder = RestClient.builder(new HttpHost(host, port, scheme));
//
//        // 设置认证
//        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
//            BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
//            credentialsProvider.setCredentials(AuthScope.ANY,
//                    new UsernamePasswordCredentials(username, password));
//            builder.setHttpClientConfigCallback(httpClientBuilder ->
//                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
//        }
//
//        RestClient restClient = builder.build();
//
//        // 创建配置好的 ObjectMapper，支持 Java 8 时间类型
//        ObjectMapper objectMapper = new ObjectMapper();
//        objectMapper.registerModule(new JavaTimeModule());
//        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//
//        // 创建 JacksonJsonpMapper
//        JacksonJsonpMapper jsonpMapper = new JacksonJsonpMapper(objectMapper);
//
//        // 创建 Transport
//        ElasticsearchTransport transport = new RestClientTransport(
//                restClient, jsonpMapper);
//
//        // 返回 ElasticsearchClient
//        return new ElasticsearchClient(transport);
//    }
}