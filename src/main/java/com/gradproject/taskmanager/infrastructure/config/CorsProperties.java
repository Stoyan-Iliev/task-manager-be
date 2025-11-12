package com.gradproject.taskmanager.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "cors")
public class CorsProperties {
    private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
    private List<String> allowedMethods = new ArrayList<>(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
    private List<String> allowedHeaders = new ArrayList<>(List.of("Authorization","Content-Type"));

}
