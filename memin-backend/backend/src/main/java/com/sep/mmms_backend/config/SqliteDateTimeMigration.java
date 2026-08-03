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
            connection.setAutoCommit(false);
            try {
                for (Column column : COLUMNS) {
                    migrateColumn(connection, column);
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
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
