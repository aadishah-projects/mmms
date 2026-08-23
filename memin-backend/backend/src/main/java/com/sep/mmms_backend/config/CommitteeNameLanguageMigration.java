package com.sep.mmms_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Map;

/**
 * Adds the bilingual committee-name column and fills the known seed committees.
 * Existing user-entered values are never overwritten.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommitteeNameLanguageMigration implements ApplicationRunner {

    private static final Map<String, String> KNOWN_TRANSLATIONS = Map.of(
            "Academic Committee", "\u0936\u0948\u0915\u094d\u0937\u093f\u0915 \u0938\u092e\u093f\u0924\u093f",
            "Events Committee", "\u0915\u093e\u0930\u094d\u092f\u0915\u094d\u0930\u092e \u0938\u092e\u093f\u0924\u093f",
            "Research and Development Committee", "\u0905\u0928\u0941\u0938\u0928\u094d\u0927\u093e\u0928 \u0924\u0925\u093e \u0935\u093f\u0915\u093e\u0938 \u0938\u092e\u093f\u0924\u093f",
            "Disciplinary Committee", "\u0905\u0928\u0941\u0936\u093e\u0938\u0928 \u0938\u092e\u093f\u0924\u093f",
            "Student Welfare Committee", "\u0935\u093f\u0926\u094d\u092f\u093e\u0930\u094d\u0925\u0940 \u0915\u0932\u094d\u092f\u093e\u0923 \u0938\u092e\u093f\u0924\u093f",
            "IT and Infrastructure Committee", "\u0938\u0942\u091a\u0928\u093e \u092a\u094d\u0930\u0935\u093f\u0927\u093f \u0924\u0925\u093e \u092a\u0942\u0930\u094d\u0935\u093e\u0927\u093e\u0930 \u0938\u092e\u093f\u0924\u093f"
    );

    private final DataSource dataSource;

    public CommitteeNameLanguageMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "committees")) {
                return;
            }

            addColumnIfMissing(connection);
            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT committee_id, committee_name, committee_name_nepali "
                            + "FROM committees");
                 ResultSet rows = select.executeQuery();
                 PreparedStatement update = connection.prepareStatement(
                         "UPDATE committees SET committee_name_nepali = ? "
                                 + "WHERE committee_id = ?")) {
                while (rows.next()) {
                    String currentNepaliName = rows.getString("committee_name_nepali");
                    if (currentNepaliName != null && !currentNepaliName.isBlank()) {
                        continue;
                    }

                    String englishName = rows.getString("committee_name");
                    String nepaliName = KNOWN_TRANSLATIONS.get(englishName);
                    if (nepaliName == null && containsDevanagari(englishName)) {
                        nepaliName = englishName;
                    }
                    if (nepaliName == null) {
                        continue;
                    }

                    update.setString(1, nepaliName);
                    update.setInt(2, rows.getInt("committee_id"));
                    update.addBatch();
                }
                update.executeBatch();
            }
        }
    }

    private void addColumnIfMissing(Connection connection) throws SQLException {
        if (columnExists(connection, "committees", "committee_name_nepali")) {
            return;
        }
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE committees ADD COLUMN committee_name_nepali VARCHAR(255)");
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = connection.getMetaData().getTables(null, null,
                table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
            return tables.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (ResultSet columns = connection.getMetaData().getColumns(null, null, table, column)) {
            if (columns.next()) {
                return true;
            }
        }
        try (ResultSet columns = connection.getMetaData().getColumns(null, null,
                table.toUpperCase(), column.toUpperCase())) {
            return columns.next();
        }
    }

    private boolean containsDevanagari(String value) {
        return value != null && value.codePoints()
                .anyMatch(codePoint -> codePoint >= 0x0900 && codePoint <= 0x097F);
    }
}
