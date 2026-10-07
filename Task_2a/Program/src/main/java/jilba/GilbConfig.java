package jilba;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурационный класс Spring Framework для пакета {@code jilba}.
 * Настраивает автоматическое сканирование компонентов, сервисов и внедрение зависимостей.
 */
@Configuration
@ComponentScan(basePackages = "jilba")
public class GilbConfig {
}
