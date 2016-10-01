package com.example;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.boot.actuate.info.Info;
import org.springframework.stereotype.Component;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class ApplicationInfoContributorTest
{
    private ApplicationInfoContributor contributor;

    @Mock
    private Info.Builder mockBuilder;

    @Before
    public void setUp()
    {
        contributor = new ApplicationInfoContributor();
        initMocks(this);
    }

    @Test
    public void shouldBeDeclaredAsComponent()
    {
        assertNotNull(contributor.getClass().getAnnotation(Component.class));
    }

    @Test
    public void shouldContributeServiceNameToApplicationInfo()
    {
        setField(contributor, "serviceHostname", "example");
        contributor.contribute(mockBuilder);
        verify(mockBuilder).withDetail(eq("service"), eq("example"));
    }
}
