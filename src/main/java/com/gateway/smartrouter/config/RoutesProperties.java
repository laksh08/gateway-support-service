package com.gateway.smartrouter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;

/**
 * Binds the {@code routes.*} entries from application.yml into a map.
 */
@ConfigurationProperties(prefix = "routes")
public class RoutesProperties extends LinkedHashMap<String, String> {
}
