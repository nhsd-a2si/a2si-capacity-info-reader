package com.nhsd.a2si.capacityinforeader;

import com.nhsd.a2si.capacityinforeader.providers.dhu.DhuJobQuartzConfiguration;
import com.nhsd.a2si.capacityinforeader.scheduler.QuartzConfiguration;
import com.nhsd.a2si.capacityserviceclient.CapacityServiceClient;
import com.nhsd.a2si.capacityserviceclient.CapacityServiceRestClient;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Import({ QuartzConfiguration.class, DhuJobQuartzConfiguration.class} )
public class CapacityInfoReaderConfiguration {

    // Pooling Client Connection Manager maintains a pool of HTTP connections, this saves time and resource
    // as creating an HTTP connection is considered a heavyweight process
    @Bean
    public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {

        PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(20);
        return poolingHttpClientConnectionManager;

    }

    // Note: socketTimeout() (or SO_TIMEOUT) refers to the timeout for waiting for data,
    // connectTimeout() refers to the timeout until a connection is established and
    // connectionRequestTimeout() refers to the timeout when requesting a connection from the connection manager.

    // RequestConfig defines the wait times before time outs occur
    @Bean
    public RequestConfig requestConfig() {

        return RequestConfig.custom()
                .setSocketTimeout(1000)
                .setConnectTimeout(200)
                .setConnectionRequestTimeout(200)
                .build();

    }

    // an HTTP client is extracted from the connection manager and uses the request configuration to define the timeouts
    @Bean
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager,
                                          RequestConfig requestConfig) {

        return HttpClientBuilder
                .create()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

    }

    // The Rest Template is configured to use the components defined in the http client, which include
    // the pooling connection manager and the request configuration
    @Bean
    public RestTemplate restTemplate(HttpClient httpClient) {

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);

    }

    // The http client objects and rest templates are all built in order to support the Capacity Service Client,
    // which allows a job to communicate with the Capacity Service, mainly to add new information on the current
    // capacity of services
    @Bean(name = "capacityServiceClient")
    public CapacityServiceClient capacityServiceClient() {

        return new CapacityServiceRestClient(restTemplate(httpClient(poolingHttpClientConnectionManager(),
                requestConfig())));

    }


}
