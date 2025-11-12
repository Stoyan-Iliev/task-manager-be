package com.gradproject.taskmanager.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@ConfigurationProperties(prefix = "security.jwt")
public class SecurityJwtProperties {
    private String issuer = "http://localhost:8080";
    private List<String> audience = new ArrayList<>(List.of("web"));
    private Duration accessTokenTtl = Duration.parse("PT15M");
    private Duration refreshTokenTtl = Duration.parse("P14D");
    private Duration clockSkew = Duration.parse("PT60S");
    private String refreshHmacSecret; 
    private KeyProperties key = new KeyProperties();

    @Setter
    @Getter
    public static class KeyProperties {
        private String type = "pem"; 
        
        private List<String> pemPublic = new ArrayList<>();
        private List<String> pemPrivate = new ArrayList<>();
        
        private String keystoreLocation; 
        private String keystoreAlias;
        private String keystorePassword;
        
        private String currentKid;

    }
}
