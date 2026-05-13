package com.finsphere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 1. Standard static mapping for CSS/JS
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // 2. Fixed Favicon mapping - point to the static folder directly
        registry.addResourceHandler("/favicon.ico")
                .addResourceLocations("classpath:/static/");

        // 3. Keep this as a fallback for root resources
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }
}
