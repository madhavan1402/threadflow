package com.threadflow.health;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.springframework.stereotype.Service;

@Service
public class DatabaseHealthService {

    private final DataSource dataSource;

    public DatabaseHealthService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean isUp() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            return resultSet.next();
        } catch (Exception ignored) {
            return false;
        }
    }

    public String vendor() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.getMetaData().getDatabaseProductName();
        } catch (Exception ignored) {
            return "Unknown";
        }
    }
}
