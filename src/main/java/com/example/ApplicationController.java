package com.example;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationController
{
    @RequestMapping("/")
    public String home()
    {
        return "Hello, World.";
    }
}
