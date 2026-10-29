package com.factorypulse.kpi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Paramètres du service, lus dans application.yml sous la clé "kpi".
 *
 * @param topic                  topic Kafka à lire
 * @param idealCycleSeconds      temps de cycle idéal de chaque machine, ex. M1 → 2.0
 *                               (indispensable pour calculer la performance du TRS)
 */
@ConfigurationProperties(prefix = "kpi")
public record KpiProperties(
        String topic,
        Map<String, Double> idealCycleSeconds) {
}
