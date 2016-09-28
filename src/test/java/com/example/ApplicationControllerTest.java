package com.example;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ApplicationControllerTest
{
    private ApplicationController controller;

    @Before
    public void setUp()
    {
        controller = new ApplicationController();
    }

    @Test
    public void shouldSayHelloWhenHomeIsAccessed()
    {
        assertEquals("Hello, World.", controller.home());
    }
}
