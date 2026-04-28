package com.finsphere.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ensures that /js/**, /css/**, and /assets/** are served correctly
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
