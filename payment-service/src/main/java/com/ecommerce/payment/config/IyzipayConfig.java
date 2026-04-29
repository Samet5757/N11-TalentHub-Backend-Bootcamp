package com.ecommerce.payment.config;

import com.iyzipay.Options;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IyzipayConfig {

    @Value("${iyzico.base-url}")
    private String baseUrl;

    @Value("${iyzico.api-key}")
    private String apiKey;

    @Value("${iyzico.secret-key}")
    private String secretKey;

    @Bean
    public Options iyzipayOptions() {
        Options options = new Options();
        options.setBaseUrl(baseUrl);
        options.setApiKey(apiKey);
        options.setSecretKey(secretKey);
        return options;
    }
}
