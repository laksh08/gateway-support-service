package com.gateway.smartrouter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;
import static org.springframework.web.reactive.function.server.ServerResponse.ok;

@Configuration
public class PortalWebConfiguration {

    @Bean
    public RouterFunction<ServerResponse> portalRouter() {
        return route(GET("/portal").and(accept(MediaType.TEXT_HTML)),
                request -> ok().contentType(MediaType.TEXT_HTML)
                        .bodyValue(loadIndexHtml()));
    }

    private String loadIndexHtml() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            return new String(resource.getInputStream().readAllBytes());
        } catch (Exception e) {
            return "<html><body><h1>Gateway Portal</h1><p>index.html not found</p></body></html>";
        }
    }
}
