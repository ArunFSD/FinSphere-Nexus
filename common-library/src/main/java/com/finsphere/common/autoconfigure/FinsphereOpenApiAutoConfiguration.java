package com.finsphere.common.autoconfigure;

import com.finsphere.common.docs.ApiDocsCustomizer;
import com.finsphere.common.security.CookieProperties;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Swagger UI conventions (session cookie security scheme, error envelope, standard error answers) for services that
 * ship springdoc. The services keep their own {@code OpenAPI} bean for the title and description.
 */
@AutoConfiguration(after = FinsphereSecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "org.springdoc.core.customizers.GlobalOpenApiCustomizer")
public class FinsphereOpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ApiDocsCustomizer finsphereApiDocsCustomizer(ObjectProvider<CookieProperties> cookieProperties) {
        CookieProperties cookie = cookieProperties.getIfAvailable();
        return new ApiDocsCustomizer((cookie != null) ? cookie.name() : "fsn_auth_token");
    }
}
