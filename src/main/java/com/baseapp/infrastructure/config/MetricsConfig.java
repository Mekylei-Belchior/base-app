package com.baseapp.infrastructure.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura tags comuns aplicadas a todas as métricas Prometheus.
 *
 * Tags presentes em todas as séries:
 *  - app         : nome da aplicação (spring.application.name)
 *  - environment : ambiente de execução (app.environment — dev/staging/prod)
 *  - version     : versão do build (build.version gerado pelo springBoot.buildInfo())
 *
 * Com essas tags é possível filtrar painéis do Grafana por ambiente sem
 * precisar de instâncias separadas do Prometheus.
 */
@Configuration
public class MetricsConfig {

    /**
     * Configura o MeterRegistry para adicionar tags comuns a todas as métricas.
     * 
     * @param appName     nome da aplicação
     * @param environment ambiente de execução (dev/staging/prod)
     * @param version     versão do build (build.version vem do
     *                    META-INF/build-info.properties (springBoot.buildInfo()).
     *                    O fallback 'unknown' protege execuções de teste onde o
     *                    arquivo pode não existir.
     * @return customizador do MeterRegistry
     */
    @Bean
    MeterRegistryCustomizer<MeterRegistry> commonTags(
            @Value("${spring.application.name}") String appName,
            @Value("${app.environment:dev}") String environment,
            @Value("${build.version:unknown}") String version) {

        return registry -> registry.config()
                .commonTags(
                        "app", appName,
                        "environment", environment,
                        "version", version);
    }
}
