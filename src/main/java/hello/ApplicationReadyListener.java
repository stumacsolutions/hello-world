package hello;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
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

import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
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
        ServiceConfiguration serviceConfiguration = getServiceConfiguration(serviceApiUri);

        if (serviceConfiguration.getAutoRedeploy())
        {
            ServiceLink serviceLink = ServiceLink.builder().
                    name(SERVICE_NAME_WEB).
                    serviceUri(serviceApiUri).
                    build();

            ServiceLink lbServiceLink = serviceConfiguration.getLinkedToServiceUrl(SERVICE_NAME_LB);
            ServiceLink otherServiceLink = serviceConfiguration.getLinkedToServiceUrl(serviceLink.getName());

            // disable auto redeploy and clear links on current service
            updateServiceConfiguration(serviceApiUri,
                    ServiceConfiguration.builder().
                            autoRedeploy(false).
                            linkedToServices(new ArrayList<>()).
                            build());

            // update load balancer link to point at current service
            updateServiceConfiguration(lbServiceLink.getServiceUri(),
                    ServiceConfiguration.builder().
                            linkedToService(serviceLink).
                            build());

            // enable auto redeploy on now inactive service and add links needed for next release
            updateServiceConfiguration(otherServiceLink.getServiceUri(),
                    ServiceConfiguration.builder().
                            autoRedeploy(true).
                            linkedToService(lbServiceLink).
                            linkedToService(serviceLink).
                            build());
        }
    }

    private ServiceConfiguration getServiceConfiguration(String uri)
    {
        HttpEntity<Object> entity = new HttpEntity<>(null, httpHeaders);
        ResponseEntity<ServiceConfiguration> responseEntity =
                restTemplate.exchange(restHost + uri, GET, entity, ServiceConfiguration.class);
        return responseEntity.getBody();
    }

    private void updateServiceConfiguration(String uri, ServiceConfiguration config)
    {
        HttpEntity<Object> entity = new HttpEntity<>(config, httpHeaders);
        restTemplate.exchange(restHost + uri, PATCH, entity, Void.class);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static final class ServiceConfiguration
    {
        @JsonProperty("autoredeploy")
        private Boolean autoRedeploy;

        @Singular
        @JsonProperty("linked_to_service")
        private List<ServiceLink> linkedToServices;

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
        @JsonProperty("name")
        private String name;

        @JsonProperty("to_service")
        private String serviceUri;
    }
}
