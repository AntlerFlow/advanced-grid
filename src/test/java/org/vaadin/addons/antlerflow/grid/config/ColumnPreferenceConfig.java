package org.vaadin.addons.antlerflow.grid.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.vaadin.addons.antlerflow.grid.columns.storage.JdbcColumnPreferenceStorage;
import org.vaadin.addons.antlerflow.grid.columns.storage.ServerSideColumnPreferenceStorage;

import javax.sql.DataSource;
import java.util.Optional;

@Configuration
public class ColumnPreferenceConfig {

    /** The demo has no login, so every visitor shares one demo user. */
    static final String DEMO_USER = "demo-user";

    @Bean
    JdbcColumnPreferenceStorage columnPreferenceStorage(DataSource dataSource) {
        JdbcColumnPreferenceStorage storage =
                new JdbcColumnPreferenceStorage(
                        dataSource,
                        () ->
                                Optional.ofNullable(
                                                ServerSideColumnPreferenceStorage
                                                        .requestPrincipalName()
                                                        .get())
                                        .orElse(DEMO_USER));
        storage.initializeSchema(); // a real application would use a Flyway/Liquibase migration
        return storage;
    }
}
