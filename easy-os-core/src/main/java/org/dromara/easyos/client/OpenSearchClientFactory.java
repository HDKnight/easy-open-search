package org.dromara.easyos.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;

public final class OpenSearchClientFactory {
    private static final Logger log = LoggerFactory.getLogger(OpenSearchClientFactory.class);

    private OpenSearchClientFactory() {
    }

    public static OpenSearchClient create(EasyOsProperties properties) {
        try {
            String address = properties.getAddress();
            String[] parts = address.split(":");
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 9200;
            String scheme = properties.getSchema() == null ? "http" : properties.getSchema();

            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            if (properties.getUsername() != null && properties.getPassword() != null) {
                credentialsProvider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
            }

            RestClient restClient = RestClient.builder(new HttpHost(host, port, scheme))
                    .setHttpClientConfigCallback(httpClientBuilder -> {
                        if (properties.getUsername() != null && properties.getPassword() != null) {
                            httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
                        }
                        if ("https".equalsIgnoreCase(scheme)) {
                            configureSsl(httpClientBuilder, properties.isTrustSelfSigned());
                        }
                        return httpClientBuilder;
                    })
                    .build();
            log.info("[easy-os] OpenSearchClient 已创建: {}://{}:{}", scheme, host, port);
            return new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        } catch (Exception e) {
            log.error("[easy-os] 创建 OpenSearchClient 失败: address={}", properties.getAddress(), e);
            throw new EasyOsException("Failed to create OpenSearchClient", e);
        }
    }

    private static void configureSsl(HttpAsyncClientBuilder httpClientBuilder, boolean trustSelfSigned) {
        try {
            SSLContextBuilder sslContextBuilder = SSLContextBuilder.create();
            if (trustSelfSigned) {
                sslContextBuilder.loadTrustMaterial(null, (chain, authType) -> true);
                log.info("[easy-os] HTTPS 已启用 trust-self-signed（仅建议开发环境）");
            }
            SSLContext sslContext = sslContextBuilder.build();
            httpClientBuilder.setSSLContext(sslContext);
            if (trustSelfSigned) {
                httpClientBuilder.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
        } catch (Exception e) {
            throw new EasyOsException("Failed to configure HTTPS for OpenSearchClient", e);
        }
    }
}
