package com.example;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInfoContributor implements InfoContributor
{
    @Value("${dockercloud.service.hostname:unknown}")
    private String serviceHostname;

    @Override
    public void contribute(Info.Builder builder)
    {
        builder.withDetail("service", serviceHostname);
    }
}
