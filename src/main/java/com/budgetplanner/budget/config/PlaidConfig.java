package com.budgetplanner.budget.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlaidConfig {

    @Value("${plaid.client-id}")
    private String clientId;

    @Value("${plaid.secret}")
    private String secret;

    @Value("${plaid.environment:sandbox}")
    private String environment;
    
    @Bean
    public String plaidClientId() {
        return clientId;
    }
    
    @Bean
    public String plaidSecret() {
        return secret;
    }
    
    @Bean
    public String plaidEnvironment() {
        return environment;
    }
}
