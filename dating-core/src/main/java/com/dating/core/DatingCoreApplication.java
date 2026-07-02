package com.dating.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа приложения dating-core (фаза 1: модули auth и profile).
 *
 * <p>{@link EnableJpaAuditing} включает автозаполнение {@code createdAt}/{@code updatedAt}.
 * {@link ConfigurationPropertiesScan} подхватывает классы {@code @ConfigurationProperties}
 * (например, {@code JwtProperties}).
 */
@SpringBootApplication
@EnableJpaAuditing
@ConfigurationPropertiesScan
@EnableScheduling
public class DatingCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(DatingCoreApplication.class, args);
	}
}