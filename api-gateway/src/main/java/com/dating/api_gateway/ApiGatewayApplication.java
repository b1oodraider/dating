package com.dating.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}



	@Bean
	KeyResolver userKeyResolver() {
		return exchange -> {
			var addr = exchange.getRequest().getRemoteAddress();
			String key = (addr != null && addr.getAddress() != null)
					? addr.getAddress().getHostAddress()
					: "unknown";
			return Mono.just(key);
		};
	}
}
