package com.example;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import static org.junit.Assert.assertNotNull;

public class ApplicationTest
{
    private Application application;

    @Before
    public void setUp()
    {
        application = new Application();
    }

    @Test
    public void shouldBeAnnotatedAsSpringBootApplication()
    {
        Class<? extends Application> applicationClass = application.getClass();
        SpringBootApplication annotation = applicationClass.getAnnotation(SpringBootApplication.class);
        assertNotNull(annotation);
    }
}
