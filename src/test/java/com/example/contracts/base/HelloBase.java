package com.example.contracts.base;

import com.example.ApplicationController;
import com.jayway.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.Before;

public class HelloBase
{
    @Before
    public void setup()
    {
        RestAssuredMockMvc.standaloneSetup(new ApplicationController());
    }
}
