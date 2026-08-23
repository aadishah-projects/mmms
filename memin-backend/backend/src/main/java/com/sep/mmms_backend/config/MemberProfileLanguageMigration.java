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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Moves legacy member profile values into the bilingual fields and normalizes
 * known designation values. The migration is idempotent and works with every
 * database supported by the application.
 *
 * Names cannot be translated reliably without the member's input, so only the
 * known demo names are transliterated. Unknown names are preserved and copied
 * to the Nepali field when they are already written in Devanagari.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MemberProfileLanguageMigration implements ApplicationRunner {

    private static final Map<String, String> NEPALI_TO_ENGLISH = Map.ofEntries(
            Map.entry("हरि", "Hari"),
            Map.entry("बहादुर", "Bahadur"),
            Map.entry("गिता", "Gita"),
            Map.entry("ओली", "Oli"),
            Map.entry("विकाश", "Bikash"),
            Map.entry("लामा", "Lama"),
            Map.entry("सुनिता", "Sunita"),
            Map.entry("महार्जन", "Maharjan"),
            Map.entry("कमल", "Kamal"),
            Map.entry("पाण्डे", "Pandey"),
            Map.entry("दीपा", "Deepa"),
            Map.entry("गुरुङ", "Gurung"),
            Map.entry("नविन", "Navin"),
            Map.entry("तामाङ", "Tamang"),
            Map.entry("अनिता", "Anita"),
            Map.entry("श्रेष्ठ", "Shrestha"),
            Map.entry("सरिता", "Sarita"),
            Map.entry("ढकाल", "Dhakal"),
            Map.entry("विजय", "Vijay"),
            Map.entry("रोजिना", "Rojina"),
            Map.entry("सुमन", "Suman"),
            Map.entry("बिष्ट", "Bista"),
            Map.entry("रमेश", "Ramesh"),
            Map.entry("कार्की", "Karki"),
            Map.entry("सीता", "Sita"),
            Map.entry("बस्नेत", "Basnet"),
            Map.entry("प्रकाश", "Prakash"),
            Map.entry("राणा", "Rana"),
            Map.entry("मिना", "Mina"),
            Map.entry("थापा", "Thapa"),
            Map.entry("दिपेश", "Dipesh"),
            Map.entry("के.सी", "K.C"),
            Map.entry("अस्मित", "Asmit"),
            Map.entry("खनाल", "Khanal")
    );

    private static final Map<String, String> ENGLISH_TO_NEPALI = NEPALI_TO_ENGLISH.entrySet().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    entry -> entry.getValue().toLowerCase(Locale.ROOT),
                    Map.Entry::getKey));

    private final DataSource dataSource;

    public MemberProfileLanguageMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            if (!tableExists(connection, "members")) {
                return;
            }

            ensureBilingualColumns(connection);
            List<NormalizedMember> updates = readChanges(connection);
            if (updates.isEmpty()) {
                return;
            }

            boolean originalAutoCommit = connection.getAutoCommit();
            try {
                connection.setAutoCommit(false);
                try (PreparedStatement update = connection.prepareStatement("""
                        UPDATE members
                        SET member_first_name = ?,
                            member_last_name = ?,
                            member_first_name_nepali = ?,
                            member_last_name_nepali = ?,
                            member_title = ?,
                            member_title_nepali = ?
                        WHERE member_id = ?
                        """)) {
                    for (NormalizedMember member : updates) {
                        update.setString(1, member.firstName());
                        update.setString(2, member.lastName());
                        update.setString(3, member.firstNameNepali());
                        update.setString(4, member.lastNameNepali());
                        update.setString(5, member.title());
                        update.setString(6, member.titleNepali());
                        update.setInt(7, member.id());
                        update.addBatch();
                    }
                    update.executeBatch();
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

    private List<NormalizedMember> readChanges(Connection connection) throws SQLException {
        List<NormalizedMember> updates = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement("""
                SELECT member_id,
                       member_first_name,
                       member_last_name,
                       member_first_name_nepali,
                       member_last_name_nepali,
                       member_title,
                       member_title_nepali
                FROM members
                """);
             ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                NormalizedMember normalized = normalize(
                        rows.getInt("member_id"),
                        rows.getString("member_first_name"),
                        rows.getString("member_last_name"),
                        rows.getString("member_first_name_nepali"),
                        rows.getString("member_last_name_nepali"),
                        rows.getString("member_title"),
                        rows.getString("member_title_nepali"));
                if (normalized.changedFrom(rows)) {
                    updates.add(normalized);
                }
            }
        }
        return updates;
    }

    private NormalizedMember normalize(int id,
                                       String firstName,
                                       String lastName,
                                       String firstNameNepali,
                                       String lastNameNepali,
                                       String title,
                                       String titleNepali) {
        NamePair first = normalizeName(firstName, firstNameNepali);
        NamePair last = normalizeName(lastName, lastNameNepali);
        TitlePair titles = normalizeTitle(title, titleNepali);
        return new NormalizedMember(id, first.english(), last.english(), first.nepali(), last.nepali(),
                titles.english(), titles.nepali());
    }

    private NamePair normalizeName(String english, String nepali) {
        String englishValue = trimToNull(english);
        String nepaliValue = trimToNull(nepali);

        if (containsDevanagari(englishValue)) {
            if (nepaliValue == null) {
                nepaliValue = englishValue;
            }
            englishValue = NEPALI_TO_ENGLISH.getOrDefault(englishValue, englishValue);
        } else if (nepaliValue == null && englishValue != null) {
            nepaliValue = ENGLISH_TO_NEPALI.get(englishValue.toLowerCase(Locale.ROOT));
        }

        if (containsDevanagari(nepaliValue)) {
            englishValue = NEPALI_TO_ENGLISH.getOrDefault(nepaliValue, englishValue);
        }
        return new NamePair(englishValue, nepaliValue);
    }

    private TitlePair normalizeTitle(String title, String titleNepali) {
        String englishValue = trimToNull(title);
        String nepaliValue = trimToNull(titleNepali);
        Designation designation = designationFor(englishValue);
        if (designation == null) {
            designation = designationFor(nepaliValue);
        }
        if (designation == null) {
            return new TitlePair(englishValue, nepaliValue);
        }
        return new TitlePair(designation.english(), designation.nepali());
    }

    private Designation designationFor(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[.,]", "")
                .replaceAll("\\s+", " ");
        return switch (normalized) {
            case "pra", "prof", "professor", "प्रा" -> new Designation("Prof.", "प्रा.");
            case "upra", "assistant professor", "asst professor", "asst prof", "उप्रा" ->
                    new Designation("Asst. Prof.", "उप्रा.");
            case "associate professor", "assoc professor", "assoc prof", "सहप्राध्यापक" ->
                    new Designation("Assoc. Prof.", "सहप्राध्यापक");
            case "da", "dr", "doctor", "डा" -> new Designation("Dr.", "डा.");
            case "member", "सदस्य" -> new Designation("Member", "सदस्य");
            default -> null;
        };
    }

    private void ensureBilingualColumns(Connection connection) throws SQLException {
        addColumnIfMissing(connection, "member_first_name_nepali");
        addColumnIfMissing(connection, "member_last_name_nepali");
        addColumnIfMissing(connection, "member_title_nepali");
    }

    private void addColumnIfMissing(Connection connection, String column) throws SQLException {
        if (columnExists(connection, "members", column)) {
            return;
        }
        try (var statement = connection.createStatement()) {
            statement.execute("ALTER TABLE members ADD COLUMN " + column + " VARCHAR(255)");
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (ResultSet tables = connection.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
            if (tables.next()) {
                return true;
            }
        }
        try (ResultSet tables = connection.getMetaData().getTables(null, null, table.toUpperCase(Locale.ROOT), new String[]{"TABLE"})) {
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
                table.toUpperCase(Locale.ROOT), column.toUpperCase(Locale.ROOT))) {
            return columns.next();
        }
    }

    private boolean containsDevanagari(String value) {
        if (value == null) {
            return false;
        }
        return value.codePoints().anyMatch(codePoint -> codePoint >= 0x0900 && codePoint <= 0x097F);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private record NamePair(String english, String nepali) {
    }

    private record TitlePair(String english, String nepali) {
    }

    private record Designation(String english, String nepali) {
    }

    private record NormalizedMember(int id,
                                    String firstName,
                                    String lastName,
                                    String firstNameNepali,
                                    String lastNameNepali,
                                    String title,
                                    String titleNepali) {
        private boolean changedFrom(ResultSet row) throws SQLException {
            return !same(firstName, row.getString("member_first_name"))
                    || !same(lastName, row.getString("member_last_name"))
                    || !same(firstNameNepali, row.getString("member_first_name_nepali"))
                    || !same(lastNameNepali, row.getString("member_last_name_nepali"))
                    || !same(title, row.getString("member_title"))
                    || !same(titleNepali, row.getString("member_title_nepali"));
        }

        private static boolean same(String left, String right) {
            if (left == null || left.isBlank()) {
                return right == null || right.isBlank();
            }
            return left.equals(right == null ? null : right.trim());
        }
    }
}
