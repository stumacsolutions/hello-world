package hello;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static hello.ApplicationReadyListener.ServiceConfiguration;
import static hello.ApplicationReadyListener.ServiceLink;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpHeaders.*;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ApplicationReadyListenerTest
{
    private ServiceConfiguration config;
    private ApplicationReadyListener listener;

    @Mock
    private RestTemplate mockRestTemplate;

    @Before
    public void setUp()
    {
        initMocks(this);

        config = ServiceConfiguration.builder().
                autoRedeploy(true).
                linkedToService(ServiceLink.builder().
                        name("lb").
                        serviceUri("lb").
                        build()).
                linkedToService(ServiceLink.builder().
                        name("web").
                        serviceUri("web").
                        build()).
                build();

        listener = new ApplicationReadyListener(mockRestTemplate);
        setSystemPropertyValuesOnListener();

        when(mockRestTemplate.exchange(
                eq("http://localhost/api/test"), same(GET), any(HttpEntity.class), same(ServiceConfiguration.class))).
                thenReturn(ResponseEntity.ok(config));
    }


    @Test
    public void shouldAutomaticallyPerformBlueGreenRelease()
    {
        listener.onApplicationEvent(null);

        InOrder inOrder = inOrder(mockRestTemplate);

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/test",
                ServiceConfiguration.builder().
                        autoRedeploy(false).
                        linkedToServices(new ArrayList<>()).
                        build());

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/lb",
                ServiceConfiguration.builder().
                        linkedToService(ServiceLink.builder().
                                name("web").
                                serviceUri("test").
                                build()).
                        build());

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/web",
                ServiceConfiguration.builder().
                        autoRedeploy(true).
                        linkedToService(ServiceLink.builder().
                                name("lb").
                                serviceUri("lb").
                                build()).
                        linkedToService(ServiceLink.builder().
                                name("web").
                                serviceUri("test").
                                build()).
                        build());
    }

    @Test
    public void shouldDoNothingIfRestHostPropertyIsNotInjected()
    {
        setField(listener, "restHost", null);
        listener.onApplicationEvent(null);
        verifyZeroInteractions(mockRestTemplate);
    }

    @Test
    public void shouldDoNothingIfServiceApiUrlPropertyIsNotInjected()
    {
        setField(listener, "serviceApiUri", null);
        listener.onApplicationEvent(null);
        verifyZeroInteractions(mockRestTemplate);
    }

    @Test
    public void shouldDoNothingIfAutoDeployIsNotEnabledOnService()
    {
        config.setAutoRedeploy(false);

        listener.onApplicationEvent(null);

        verify(mockRestTemplate).exchange(
                eq("http://localhost/api/test"), same(GET), any(HttpEntity.class), same(ServiceConfiguration.class));
        verifyNoMoreInteractions(mockRestTemplate);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldValidateThatServiceHasLinkToLoadBalancer()
    {
        List<ServiceLink> linkedToServices = new ArrayList<>();
        linkedToServices.add(config.getLinkedToServices().get(0));
        config.setLinkedToServices(linkedToServices);

        listener.onApplicationEvent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldValidateThatServiceHasLinkToOtherService()
    {
        List<ServiceLink> linkedToServices = new ArrayList<>();
        linkedToServices.add(config.getLinkedToServices().get(1));
        config.setLinkedToServices(linkedToServices);

        listener.onApplicationEvent(null);
    }

    private void setSystemPropertyValuesOnListener()
    {
        setField(listener, "restHost", "http://localhost/api/");
        setField(listener, "serviceApiUri", "test");
    }

    private void verifyServiceConfigurationIsUpdated(InOrder inOrder, String url, ServiceConfiguration body)
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(AUTHORIZATION, "Basic c3R1bWFjc29sdXRpb25zOjhjZjVlZjcwLWY2MDctNDNkMi04NDkwLTFjNWUyZDBlY2I4ZQ==");
        httpHeaders.set(ACCEPT, APPLICATION_JSON_VALUE);
        httpHeaders.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);

        HttpEntity<ServiceConfiguration> entity = new HttpEntity<>(body, httpHeaders);
        inOrder.verify(mockRestTemplate, times(1)).exchange(
                eq(url), same(PATCH), refEq(entity), same(Void.class));
    }
}
