package org.dromara.easyos.client;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.dromara.easyos.exception.EasyOsException;
import org.dromara.easyos.property.EasyOsProperties;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;

public final class OpenSearchClientFactory {
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
                        return httpClientBuilder;
                    })
                    .build();
            return new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
        } catch (Exception e) {
            throw new EasyOsException("Failed to create OpenSearchClient", e);
        }
    }
}
