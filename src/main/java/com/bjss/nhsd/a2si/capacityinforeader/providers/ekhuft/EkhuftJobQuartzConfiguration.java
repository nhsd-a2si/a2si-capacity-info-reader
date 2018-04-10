package com.bjss.nhsd.a2si.capacityinforeader.providers.ekhuft;

import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.quartz.SimpleTrigger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLException;
import java.io.InterruptedIOException;
import java.net.UnknownHostException;

@Configuration
public class EkhuftJobQuartzConfiguration {

    // The values injected from configuration in this class relate to the scheduling of the job
    // and NOT about the job itself
    @Value("${ekhuftApiJob.key}")
    private String ekhuftApiJobKey;

    @Value("${ekhuftApiJob.description}")
    private String ekhuftApiJobDescription;

    @Value("${ekhuftApiJob.startDelay}")
    private Long ekhuftApiJobStartDelay;

    @Value("${ekhuftApiJob.repeatInterval}")
    private Long ekhuftApiJobRepeatInterval;

    /**
     * Define the EKHUFT API Job
     *
     * @return EKHUFT API Job
     */
    @Bean(name = "ekhuftJobDetails")
    public JobDetailFactoryBean ekhuftJobDetails() {

        JobDetailFactoryBean ekhuftJobDetails = new JobDetailFactoryBean();
        ekhuftJobDetails.setJobClass(EkhuftReadCapacityInformationRestApiJob.class);
        ekhuftJobDetails.setDescription(ekhuftApiJobDescription);
        ekhuftJobDetails.setDurability(true);
        ekhuftJobDetails.setName(ekhuftApiJobKey);

        return ekhuftJobDetails;

    }

    /**
     * Define the EKHUFT API Job Trigger
     *
     * @return Simple Trigger for EKHUFT API Job
     */
    @Bean(name = "ekhuftJobTrigger")
    public SimpleTriggerFactoryBean ekhuftJobTrigger() {

        SimpleTriggerFactoryBean ekhuftJobTrigger = new SimpleTriggerFactoryBean();
        ekhuftJobTrigger.setJobDetail(ekhuftJobDetails().getObject());
        ekhuftJobTrigger.setStartDelay(ekhuftApiJobStartDelay);
        ekhuftJobTrigger.setRepeatInterval(ekhuftApiJobRepeatInterval);
        ekhuftJobTrigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY);
        ekhuftJobTrigger.setMisfireInstruction(
                SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);

        return ekhuftJobTrigger;

    }

    // Pooling Client Connection Manager maintains a pool of HTTP connections, this saves time and resource
    // as creating an HTTP connection is considered a heavyweight process
    @Bean
    public PoolingHttpClientConnectionManager poolingHttpClientConnectionManager() {
        PoolingHttpClientConnectionManager result = new PoolingHttpClientConnectionManager();
        result.setMaxTotal(20);
        return result;
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
    public CloseableHttpClient httpClient(PoolingHttpClientConnectionManager poolingHttpClientConnectionManager, RequestConfig requestConfig) {
        return HttpClientBuilder
                .create()
                .setConnectionManager(poolingHttpClientConnectionManager)
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(httpRequestRetryHandler())
                .build();
    }

    // The Rest Template is configured to use the components defined in the http client, which include
    // the pooling connection manager and the request configuration
    @Bean
    public RestTemplate ekhuftApiRestTemplate(HttpClient httpClient) {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        requestFactory.setHttpClient(httpClient);
        return new RestTemplate(requestFactory);
    }

    // The Rest Template is configured to use the components defined in the http client, which include
    // the pooling connection manager and the request configuration
    @Bean
    public HttpRequestRetryHandler httpRequestRetryHandler() {
        return (exception, executionCount, context) -> {

            System.out.println("try request: " + executionCount);

            if (executionCount >= 5) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof InterruptedIOException) {
                // Timeout
                return false;
            }
            if (exception instanceof UnknownHostException) {
                // Unknown host
                return false;
            }
            if (exception instanceof SSLException) {
                // SSL handshake exception
                return false;
            }
            HttpClientContext clientContext = HttpClientContext.adapt(context);
            HttpRequest request = clientContext.getRequest();
            boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
            if (idempotent) {
                // Retry if the request is considered idempotent
                return true;
            }
            return false;
        };
    }

}
