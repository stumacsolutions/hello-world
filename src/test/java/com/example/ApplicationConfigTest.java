package com.example;

import org.junit.Before;
import org.junit.Test;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ApplicationConfigTest
{
    private ApplicationConfig config;

    @Before
    public void setUp() {
        config = new ApplicationConfig();
    }

    @Test
    public void shouldCreateRestTemplateBean() {
        RestTemplate restTemplate = config.restTemplate();
        assertNotNull(restTemplate);
        ClientHttpRequestFactory factory = restTemplate.getRequestFactory();
        assertTrue(factory instanceof HttpComponentsClientHttpRequestFactory);
    }
}
