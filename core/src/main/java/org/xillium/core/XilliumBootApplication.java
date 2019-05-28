package org.xillium.core;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;


/**
 * Base class of Spring Boot Application that uses xillium-core platform.
 */
public class XilliumBootApplication {
    private final HttpServiceDispatcher _dispatcher = new HttpServiceDispatcher();
    private final ModuleManager _modules = new ModuleManager();

    @Bean
    public ServletRegistrationBean<HttpServiceDispatcher> dispatcher() {
        return new ServletRegistrationBean<>(_dispatcher, _modules.mappings());
    }

    @Bean
    public ServicePlatform initializer() {
        return new ServicePlatform(_modules);
    }
}
