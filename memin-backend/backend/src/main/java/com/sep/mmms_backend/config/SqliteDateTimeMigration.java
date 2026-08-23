package com.sep.mmms_backend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Migrates legacy SQLite text date/time values to the numeric representation
 * used by the SQLite JDBC driver when date_class=INTEGER is enabled.
 *
 * This is deliberately a no-op for PostgreSQL, H2, and other databases.
 */
@Component
public class SqliteDateTimeMigration implements ApplicationRunner {

    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final List<Column> COLUMNS = List.of(
            new Column("members", "member_created_date", Kind.DATE),
            new Column("members", "member_modified_date", Kind.DATE),
            new Column("committees", "committee_created_date", Kind.DATE),
            new Column("committees", "committee_modified_date", Kind.DATE),
            new Column("meetings", "meeting_held_date", Kind.DATE),
            new Column("meetings", "meeting_held_time", Kind.TIME),
            new Column("meetings", "created_date", Kind.DATE),
            new Column("meetings", "updated_date", Kind.DATE),
            new Column("decisions", "decision_created_date", Kind.DATE),
            new Column("decisions", "decision_modified_date", Kind.DATE),
            new Column("agendas", "agenda_created_date", Kind.DATE),
            new Column("agendas", "agenda_modified_date", Kind.DATE),
            new Column("invite_tokens", "created_at", Kind.DATE_TIME),
            new Column("invite_tokens", "expires_at", Kind.DATE_TIME)
    );

    private final DataSource dataSource;

    public SqliteDateTimeMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!connection.getMetaData().getURL().startsWith("jdbc:sqlite:")) {
                return;
            }

            boolean originalAutoCommit = connection.getAutoCommit();
            if (originalAutoCommit) {
                // The committee table rebuild below must not rewrite foreign-key
                // references while the legacy table is temporarily renamed.
                try (var statement = connection.createStatement()) {
                    statement.execute("PRAGMA foreign_keys = OFF");
                    statement.execute("PRAGMA legacy_alter_table = ON");
                }
            }
            connection.setAutoCommit(false);
            try {
                for (Column column : COLUMNS) {
                    migrateColumn(connection, column);
                }
                migrateMeetingAttendeeOrder(connection);
                migrateLegacySecretaryUniqueness(connection);
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
                if (originalAutoCommit) {
                    try (var statement = connection.createStatement()) {
                        statement.execute("PRAGMA legacy_alter_table = OFF");
                        statement.execute("PRAGMA foreign_keys = ON");
                    }
                }
            }
        }
    }

    /**
     * SQLite cannot add Hibernate's NOT NULL order column to an existing
     * join table. The entity mapping therefore allows the upgrade to add it
     * as nullable, and this migration fills deterministic positions for old
     * attendee rows before the application starts serving requests.
     */
    private void migrateMeetingAttendeeOrder(Connection connection) throws Exception {
        if (!tableHasColumn(connection, "meeting_attendees", "display_order")) {
            try (PreparedStatement statement = connection.prepareStatement(
                    "ALTER TABLE meeting_attendees ADD COLUMN display_order INTEGER DEFAULT 0")) {
                statement.executeUpdate();
            }
        }

        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE meeting_attendees AS target
                SET display_order = (
                    SELECT COUNT(*)
                    FROM meeting_attendees AS previous
                    WHERE previous.meeting_id = target.meeting_id
                      AND previous.rowid < target.rowid
                )
                WHERE target.display_order IS NULL
                """)) {
            statement.executeUpdate();
        }
    }

    private boolean tableHasColumn(Connection connection, String table, String column) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "PRAGMA table_info(\"" + table + "\")");
             ResultSet columns = statement.executeQuery()) {
            while (columns.next()) {
                if (column.equalsIgnoreCase(columns.getString("name"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Older SQLite databases were created while Committee.secretary was
     * mapped as @OneToOne, which created a UNIQUE constraint on
     * committee_secretary_id. JPA does not remove that constraint when the
     * mapping changes to @ManyToOne, so rebuild only this table when the stale
     * unique index is still present.
     */
    private void migrateLegacySecretaryUniqueness(Connection connection) throws Exception {
        if (!hasUniqueIndexOnColumn(connection, "committees", "committee_secretary_id")) {
            return;
        }

        String createSql = getCreateTableSql(connection, "committees");
        if (createSql == null) {
            return;
        }

        String replacementSql = createSql.replaceFirst(
                "(?i)(CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?)([\\\"]?committees[\\\"]?)",
                "$1committees_new");
        replacementSql = replacementSql.replaceAll(
                "(?i)(committee_secretary_id\\s+[^,\\n\\r\\)]*?)\\s+unique\\b", "$1");
        replacementSql = replacementSql.replaceAll(
                "(?i),\\s*(?:CONSTRAINT\\s+[^\\s]+\\s+)?UNIQUE\\s*\\(\\s*committee_secretary_id\\s*\\)", "");

        if (replacementSql.equals(createSql)) {
            throw new IllegalStateException("Could not remove the legacy secretary uniqueness constraint");
        }

        List<String> columns = getTableColumns(connection, "committees");
        String columnList = columns.stream().map(this::quoteIdentifier).reduce((left, right) -> left + ", " + right).orElseThrow();
        try (var statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS committees_new");
            statement.execute(replacementSql);
            statement.execute("INSERT INTO committees_new (" + columnList + ") SELECT " + columnList + " FROM committees");
            statement.execute("DROP TABLE committees");
            statement.execute("ALTER TABLE committees_new RENAME TO committees");
        }
    }

    private boolean hasUniqueIndexOnColumn(Connection connection, String table, String column) throws Exception {
        try (var indexes = connection.createStatement().executeQuery("PRAGMA index_list(\"" + table + "\")")) {
            while (indexes.next()) {
                if (indexes.getInt("unique") != 1) {
                    continue;
                }
                String indexName = indexes.getString("name");
                try (var indexColumns = connection.createStatement().executeQuery(
                        "PRAGMA index_info(" + quoteIdentifier(indexName) + ")")) {
                    while (indexColumns.next()) {
                        if (column.equalsIgnoreCase(indexColumns.getString("name"))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private String getCreateTableSql(Connection connection, String table) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private List<String> getTableColumns(Connection connection, String table) throws Exception {
        List<String> columns = new ArrayList<>();
        try (var result = connection.createStatement().executeQuery("PRAGMA table_info(\"" + table + "\")")) {
            while (result.next()) {
                columns.add(result.getString("name"));
            }
        }
        return columns;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private void migrateColumn(Connection connection, Column column) throws Exception {
        String selectSql = "SELECT rowid, \"" + column.name + "\" FROM \"" + column.table + "\" "
                + "WHERE typeof(\"" + column.name + "\") = 'text' AND \"" + column.name + "\" IS NOT NULL";
        String updateSql = "UPDATE \"" + column.table + "\" SET \"" + column.name + "\" = ? WHERE rowid = ?";

        try (PreparedStatement select = connection.prepareStatement(selectSql);
             ResultSet rows = select.executeQuery();
             PreparedStatement update = connection.prepareStatement(updateSql)) {
            while (rows.next()) {
                update.setLong(1, toEpochMillis(rows.getString(2), column.kind));
                update.setLong(2, rows.getLong(1));
                update.addBatch();
            }
            update.executeBatch();
        }
    }

    private long toEpochMillis(String value, Kind kind) {
        String normalized = value.trim();
        return switch (kind) {
            case DATE -> LocalDate.parse(normalized.substring(0, 10))
                    .atStartOfDay(SYSTEM_ZONE)
                    .toInstant()
                    .toEpochMilli();
            case TIME -> LocalDate.ofEpochDay(0)
                    .atTime(LocalTime.parse(normalized.substring(normalized.length() - 8)))
                    .atZone(SYSTEM_ZONE)
                    .toInstant()
                    .toEpochMilli();
            case DATE_TIME -> LocalDateTime.parse(normalized.replace(' ', 'T'), DATE_TIME_FORMATTER)
                    .atZone(SYSTEM_ZONE)
                    .toInstant()
                    .toEpochMilli();
        };
    }

    private enum Kind {
        DATE,
        TIME,
        DATE_TIME
    }

    private record Column(String table, String name, Kind kind) {
    }
}
