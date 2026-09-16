package com.dating.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.converter.JacksonJsonMessageConverter;
import org.springframework.kafka.support.converter.RecordMessageConverter;

@SpringBootApplication
public class NotificationApplication {

	public static void main(String[] args) {
		SpringApplication.run(NotificationApplication.class, args);
	}


	@Bean
    // TODO(bug): no-arg JacksonJsonMessageConverter строит СВОЙ ObjectMapper мимо spring.jackson.*.
    //  Проверить FAIL_ON_UNKNOWN_PROPERTIES: если включён, первое же расширение event-рекорда в core
    //  положит консюмера на всех сообщениях, а DLT нет. Передавать бин маппера явно.
    // TODO(debt): контракт события — копипаста рекордов между core и notification, тест собирает JSON
    //  руками. Нужен тест "core сериализует -> notification десериализует".
    RecordMessageConverter converter() {
		return new JacksonJsonMessageConverter();
	}

}
