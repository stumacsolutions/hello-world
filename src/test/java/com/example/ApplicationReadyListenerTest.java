package com.example;

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

import static com.example.ApplicationReadyListener.Service;
import static com.example.ApplicationReadyListener.ServiceLink;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ApplicationReadyListenerTest
{
    private Service service;
    private ApplicationReadyListener listener;

    @Mock
    private RestTemplate mockRestTemplate;

    @Before
    public void setUp()
    {
        initMocks(this);

        service = Service.builder().
                autoRedeploy(true).
                linkedToService(ServiceLink.builder().
                        fromServiceUri("test").
                        name("lb").
                        toServiceUri("lb").
                        build()).
                linkedToService(ServiceLink.builder().
                        fromServiceUri("test").
                        name("web").
                        toServiceUri("other").
                        build()).
                build();

        Service otherService = Service.builder().
                autoRedeploy(false).
                targetNumberOfContainers(3).
                build();

        listener = new ApplicationReadyListener(mockRestTemplate);
        setSystemPropertyValuesOnListener();

        when(mockRestTemplate.exchange(
                eq("http://localhost/api/test"), same(GET), any(HttpEntity.class), same(Service.class))).
                thenReturn(ResponseEntity.ok(service));

        when(mockRestTemplate.exchange(
                eq("http://localhost/api/other"), same(GET), any(HttpEntity.class), same(Service.class))).
                thenReturn(ResponseEntity.ok(otherService));
    }


    @Test
    public void shouldAutomaticallyPerformBlueGreenRelease()
    {
        listener.onApplicationEvent(null);

        InOrder inOrder = inOrder(mockRestTemplate);

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/test",
                Service.builder().
                        autoRedeploy(false).
                        linkedToServices(new ArrayList<>()).
                        targetNumberOfContainers(3).
                        build());

        verifyServiceIsScaled(inOrder, "http://localhost/api/test");

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/lb",
                Service.builder().
                        linkedToService(ServiceLink.builder().
                                fromServiceUri("lb").
                                name("web").
                                toServiceUri("test").
                                build()).
                        targetNumberOfContainers(1).
                        build());

        verifyServiceConfigurationIsUpdated(
                inOrder, "http://localhost/api/other",
                Service.builder().
                        autoRedeploy(true).
                        linkedToService(ServiceLink.builder().
                                fromServiceUri("other").
                                name("lb").
                                toServiceUri("lb").
                                build()).
                        linkedToService(ServiceLink.builder().
                                fromServiceUri("other").
                                name("web").
                                toServiceUri("test").
                                build()).
                        targetNumberOfContainers(1).
                        build());

        verifyServiceIsScaled(inOrder, "http://localhost/api/other");
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
        service.setAutoRedeploy(false);

        listener.onApplicationEvent(null);

        verify(mockRestTemplate).exchange(
                eq("http://localhost/api/test"), same(GET), any(HttpEntity.class), same(Service.class));
        verifyNoMoreInteractions(mockRestTemplate);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldValidateThatServiceHasLinkToLoadBalancer()
    {
        List<ServiceLink> linkedToServices = new ArrayList<>();
        linkedToServices.add(service.getLinkedToServices().get(0));
        service.setLinkedToServices(linkedToServices);

        listener.onApplicationEvent(null);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldValidateThatServiceHasLinkToOtherService()
    {
        List<ServiceLink> linkedToServices = new ArrayList<>();
        linkedToServices.add(service.getLinkedToServices().get(1));
        service.setLinkedToServices(linkedToServices);

        listener.onApplicationEvent(null);
    }

    private void setSystemPropertyValuesOnListener()
    {
        setField(listener, "restHost", "http://localhost/api/");
        setField(listener, "serviceApiUri", "test");
    }

    private void verifyServiceConfigurationIsUpdated(InOrder inOrder, String url, Service body)
    {
        HttpHeaders httpHeaders = constructExpectedHttpHeaders();
        HttpEntity<Service> entity = new HttpEntity<>(body, httpHeaders);
        inOrder.verify(mockRestTemplate, times(1)).exchange(
                eq(url), same(PATCH), refEq(entity), same(Void.class));
    }

    private void verifyServiceIsScaled(InOrder inOrder, String url)
    {
        HttpHeaders httpHeaders = constructExpectedHttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(httpHeaders);
        inOrder.verify(mockRestTemplate, times(1)).exchange(
                eq(url + "/scale/"), same(POST), refEq(entity), same(Void.class));
    }

    private HttpHeaders constructExpectedHttpHeaders()
    {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set(AUTHORIZATION, "Basic c3R1bWFjc29sdXRpb25zOjhjZjVlZjcwLWY2MDctNDNkMi04NDkwLTFjNWUyZDBlY2I4ZQ==");
        httpHeaders.set(ACCEPT, APPLICATION_JSON_VALUE);
        httpHeaders.set(CONTENT_TYPE, APPLICATION_JSON_VALUE);
        return httpHeaders;
    }
}
