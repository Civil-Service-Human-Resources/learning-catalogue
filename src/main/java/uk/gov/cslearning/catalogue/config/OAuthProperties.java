package uk.gov.cslearning.catalogue.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "oauth")
public class OAuthProperties implements Serializable {

    private String serviceUrl;

    private String clientId;

    private String clientSecret;

    private String tokenUrl;

    private String checkTokenUrl;

    private int maxTotalConnections;

    private int defaultMaxConnectionsPerRoute;

    private int maxPerServiceUrl;

    private String jwtKey;
}
