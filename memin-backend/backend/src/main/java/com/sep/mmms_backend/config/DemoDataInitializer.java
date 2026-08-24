package com.sep.mmms_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Seeds the demo database once, while leaving user-created records intact on
 * later restarts and redeployments.
 */
@Component
@Profile("demo")
public class DemoDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;

    public DemoDataInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!appUsersTableExists()) {
            runScripts(true);
        } else if (!hasDemoUsers()) {
            runScripts(false);
        }
    }

    private boolean appUsersTableExists() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.executeQuery("SELECT COUNT(*) FROM app_users").close();
            return true;
        } catch (SQLException exception) {
            return false;
        }
    }

    private boolean hasDemoUsers() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM app_users")) {
            return resultSet.next() && resultSet.getInt(1) > 0;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect the demo database", exception);
        }
    }

    private void runScripts(boolean includeSchema) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        if (includeSchema) {
            populator.addScript(new ClassPathResource("schema-demo.sql"));
        }
        populator.addScript(new ClassPathResource("data.sql"));
        populator.addScript(new ClassPathResource("data-demo.sql"));
        populator.execute(dataSource);
    }
}
