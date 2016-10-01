package com.example;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.util.StringUtils.isEmpty;

@Component
public class ApplicationReadyListener implements ApplicationListener<ApplicationReadyEvent>
{
    private static final String SERVICE_NAME_LB = "lb";
    private static final String SERVICE_NAME_WEB = "web";

    @Value("${dockercloud.rest.host:}")
    private String restHost;

    @Value("${dockercloud.service.api.uri:}")
    private String serviceApiUri;

    private HttpHeaders httpHeaders;
    private RestTemplate restTemplate;

    @Autowired
    public ApplicationReadyListener(RestTemplate restTemplate)
    {
        this.restTemplate = restTemplate;

        httpHeaders = new HttpHeaders();
        httpHeaders.set(AUTHORIZATION, "Basic c3R1bWFjc29sdXRpb25zOjhjZjVlZjcwLWY2MDctNDNkMi04NDkwLTFjNWUyZDBlY2I4ZQ==");
        httpHeaders.set(ACCEPT, APPLICATION_JSON_VALUE);
        httpHeaders.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event)
    {
        if (isEmpty(restHost) || isEmpty(serviceApiUri))
        {
            return;
        }
        Service serviceConfiguration = getService(serviceApiUri);

        if (serviceConfiguration.getAutoRedeploy())
        {
            ServiceLink lbServiceLink = serviceConfiguration.getLinkedToServiceUrl(SERVICE_NAME_LB);
            ServiceLink otherServiceLink = serviceConfiguration.getLinkedToServiceUrl(SERVICE_NAME_WEB);

            updateService(serviceApiUri,
                    Service.builder().
                            autoRedeploy(false).
                            linkedToServices(new ArrayList<>()).
                            targetNumberOfContainers(
                                    getService(otherServiceLink.getToServiceUri()).
                                            getTargetNumberOfContainers()).
                            build());

            scaleService(serviceApiUri);

            updateService(lbServiceLink.getToServiceUri(),
                    Service.builder().
                            linkedToService(ServiceLink.builder().
                                    fromServiceUri(lbServiceLink.getToServiceUri()).
                                    name(SERVICE_NAME_WEB).
                                    toServiceUri(lbServiceLink.getFromServiceUri()).
                                    build()).
                            targetNumberOfContainers(1).
                            build());

            updateService(otherServiceLink.getToServiceUri(),
                    Service.builder().
                            autoRedeploy(true).
                            linkedToService(ServiceLink.builder().
                                    fromServiceUri(otherServiceLink.getToServiceUri()).
                                    name(lbServiceLink.getName()).
                                    toServiceUri(lbServiceLink.getToServiceUri()).
                                    build()).
                            linkedToService(ServiceLink.builder().
                                    fromServiceUri(otherServiceLink.getToServiceUri()).
                                    name(otherServiceLink.getName()).
                                    toServiceUri(otherServiceLink.getFromServiceUri()).
                                    build()).
                            targetNumberOfContainers(1).
                            build());

            scaleService(otherServiceLink.getToServiceUri());
        }
    }

    private Service getService(String uri)
    {
        HttpEntity<Object> entity = new HttpEntity<>(null, httpHeaders);
        ResponseEntity<Service> responseEntity =
                restTemplate.exchange(restHost + uri, GET, entity, Service.class);
        return responseEntity.getBody();
    }

    private void scaleService(String uri)
    {
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);
        restTemplate.exchange(restHost + uri + "/scale/", POST, entity, Void.class);
    }

    private void updateService(String uri, Service config)
    {
        HttpEntity<Object> entity = new HttpEntity<>(config, httpHeaders);
        restTemplate.exchange(restHost + uri, PATCH, entity, Void.class);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class Service
    {
        @JsonProperty("autoredeploy")
        private Boolean autoRedeploy;

        @Singular
        @JsonProperty("linked_to_service")
        private List<ServiceLink> linkedToServices;

        @JsonProperty("target_num_containers")
        private Integer targetNumberOfContainers;

        public ServiceLink getLinkedToServiceUrl(String name)
        {
            for (ServiceLink link : getLinkedToServices())
            {
                if (link.getName().equals(name))
                {
                    return link;
                }
            }
            throw new IllegalStateException("Service link not found.");
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class ServiceLink
    {
        @JsonProperty("from_service")
        private String fromServiceUri;

        @JsonProperty("name")
        private String name;

        @JsonProperty("to_service")
        private String toServiceUri;
    }
}
