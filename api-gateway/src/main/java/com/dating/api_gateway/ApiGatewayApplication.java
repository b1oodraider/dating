package com.dating.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.support.ipresolver.XForwardedRemoteAddressResolver;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiGatewayApplication.class, args);
	}



	/**
	 * Ключ rate-limiter'а — IP клиента. За прокси/LB реальный IP лежит
	 * в X-Forwarded-For; maxTrustedIndex(1) означает "доверяем ровно одному
	 * прокси перед нами" — берём последний IP, добавленный этим прокси,
	 * а не первый попавшийся (первый клиент может подделать сам).
	 * Без заголовка резолвер отдаёт обычный remote address.
	 */
	@Bean
	// TODO(security): maxTrustedIndex(1) берёт ПОСЛЕДНИЙ X-Forwarded-For, но перед gateway нет
	//  доверенного прокси — порт 8088 проброшен наружу напрямую. Значит заголовок целиком под
	//  контролем клиента: новый XFF на каждый запрос = новое ведро, rate-limit не работает вовсе
	//  (ровно этим приёмом GatewayRateLimiterTest получает "своё ведро"). До появления nginx
	//  резолвить по remoteAddress; XFF вернуть вместе с реальным прокси.
	// TODO(security): ключ лимита — IP; за мобильным NAT это одно ведро на район. Для
	//  аутентифицированных маршрутов ключом должен быть sub из JWT.
	KeyResolver userKeyResolver() {
		var ipResolver = XForwardedRemoteAddressResolver.maxTrustedIndex(1);
		return exchange -> {
			InetSocketAddress addr = ipResolver.resolve(exchange);
			String key = (addr != null && addr.getAddress() != null)
					? addr.getAddress().getHostAddress()
					: "unknown";
			return Mono.just(key);
		};
	}
}
