package com.finsphere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Standard static mapping
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Handle root-level resources (favicon, robots.txt, etc.)
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
