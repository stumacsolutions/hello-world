package hello;

import com.fasterxml.jackson.annotation.JsonProperty;
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
        if (isEmpty(restHost) || isEmpty(serviceApiUri)) {
            return;
        }
        ServiceConfiguration currentServiceConfig = getServiceConfiguration(serviceApiUri);

        if (currentServiceConfig.getAutoRedeploy())
        {
            ServiceLink serviceLink = new ServiceLink();
            serviceLink.setName("web");
            serviceLink.setServiceUri(serviceApiUri);

            ServiceLink lbServiceLink = currentServiceConfig.getLinkedToServiceUrl("lb");
            ServiceLink otherServiceLink = currentServiceConfig.getLinkedToServiceUrl(serviceLink.getName());

            // update current service
            ServiceConfiguration newServiceConfig = new ServiceConfiguration();
            newServiceConfig.setAutoRedeploy(false);
            newServiceConfig.setLinkedToServices(new ArrayList<>());
            updateServiceConfiguration(serviceApiUri, newServiceConfig);

            // update load balancer
            ServiceConfiguration lbServiceConfig = new ServiceConfiguration();
            lbServiceConfig.addLinkedToService(serviceLink);
            updateServiceConfiguration(lbServiceLink.getServiceUri(), lbServiceConfig);

            // update other service
            ServiceConfiguration otherServiceConfig = new ServiceConfiguration();
            otherServiceConfig.addLinkedToService(lbServiceLink);
            otherServiceConfig.addLinkedToService(serviceLink);
            otherServiceConfig.setAutoRedeploy(true);
            updateServiceConfiguration(otherServiceLink.getServiceUri(), otherServiceConfig);
        }
    }

    private ServiceConfiguration getServiceConfiguration(String uri)
    {
        HttpEntity<Object> entity = new HttpEntity<>(null, httpHeaders);
        ResponseEntity<ServiceConfiguration> responseEntity =
                restTemplate.exchange(restHost + uri, GET, entity, ServiceConfiguration.class);
        return responseEntity.getBody();
    }

    private ResponseEntity<ServiceConfiguration> updateServiceConfiguration(String uri, ServiceConfiguration config)
    {
        HttpEntity<Object> entity = new HttpEntity<>(config, httpHeaders);
        return restTemplate.exchange(restHost + uri, PATCH, entity, ServiceConfiguration.class);
    }

    private static final class ServiceConfiguration
    {
        @JsonProperty("autoredeploy")
        private Boolean autoRedeploy;

        @JsonProperty("linked_to_service")
        private List<ServiceLink> linkedToServices;

        public Boolean getAutoRedeploy()
        {
            return autoRedeploy;
        }

        public void addLinkedToService(ServiceLink link)
        {
            getLinkedToServices().add(link);
        }

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

        public List<ServiceLink> getLinkedToServices()
        {
            if (linkedToServices == null)
            {
                linkedToServices = new ArrayList<>();
            }
            return linkedToServices;
        }

        public void setAutoRedeploy(Boolean autoRedeploy)
        {
            this.autoRedeploy = autoRedeploy;
        }

        public void setLinkedToServices(List<ServiceLink> linkedToServices)
        {
            this.linkedToServices = linkedToServices;
        }
    }

    private static final class ServiceLink
    {
        @JsonProperty("name")
        private String name;

        @JsonProperty("to_service")
        private String serviceUri;

        public String getName()
        {
            return name;
        }

        public String getServiceUri()
        {
            return serviceUri;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public void setServiceUri(String serviceUri)
        {
            this.serviceUri = serviceUri;
        }
    }
}
