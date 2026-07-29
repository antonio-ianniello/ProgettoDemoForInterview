package com.example.usermanagement.config.jwt;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.keycloak")
public class JwtConfig {

    @NotBlank
    private String openidConfigurationUri;

    @NotBlank
    private String clientId;

    private String clientSecret;

    public String getOpenidConfigurationUri() {
        return openidConfigurationUri;
    }

    public void setOpenidConfigurationUri(String openidConfigurationUri) {
        this.openidConfigurationUri = openidConfigurationUri;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
