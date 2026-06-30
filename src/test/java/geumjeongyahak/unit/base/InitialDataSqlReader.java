package geumjeongyahak.unit.base;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class InitialDataSqlReader {

    private static final Pattern INSERT_PATTERN = Pattern.compile(
        "INSERT\\s+INTO\\s+(\\w+)\\s*\\((.*?)\\)\\s*VALUES\\s*(.*?);",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    private InitialDataSqlReader() {
    }

    static List<Map<String, String>> readTable(Path initialDataFile, String tableName) throws IOException {
        String sql = Files.readString(initialDataFile);
        List<Map<String, String>> rows = new ArrayList<>();

        Matcher matcher = INSERT_PATTERN.matcher(sql);
        while (matcher.find()) {
            if (!matcher.group(1).equalsIgnoreCase(tableName)) {
                continue;
            }

            List<String> columns = Arrays.stream(matcher.group(2).split(","))
                .map(String::trim)
                .toList();
            for (List<String> values : parseRows(matcher.group(3))) {
                rows.add(mapRow(columns, values));
            }
        }

        return rows;
    }

    static Long longValue(Map<String, String> row, String column) {
        String value = row.get(column);
        return value == null ? null : Long.valueOf(value);
    }

    static Integer integerValue(Map<String, String> row, String column) {
        String value = row.get(column);
        return value == null ? null : Integer.valueOf(value);
    }

    private static Map<String, String> mapRow(List<String> columns, List<String> values) {
        if (columns.size() != values.size()) {
            throw new IllegalArgumentException(
                "초기 데이터 INSERT 컬럼 수와 값 수가 다릅니다: " + columns.size() + " != " + values.size()
            );
        }

        Map<String, String> row = new LinkedHashMap<>();
        for (int index = 0; index < columns.size(); index++) {
            row.put(columns.get(index), normalize(values.get(index)));
        }
        return row;
    }

    private static List<List<String>> parseRows(String valuesClause) {
        List<List<String>> rows = new ArrayList<>();
        int rowStart = -1;
        int depth = 0;
        boolean quoted = false;

        for (int index = 0; index < valuesClause.length(); index++) {
            char current = valuesClause.charAt(index);
            if (current == '\'' && (index + 1 >= valuesClause.length() || valuesClause.charAt(index + 1) != '\'')) {
                quoted = !quoted;
            } else if (current == '\'' && quoted) {
                index++;
            } else if (!quoted && current == '(') {
                if (depth++ == 0) {
                    rowStart = index + 1;
                }
            } else if (!quoted && current == ')' && --depth == 0) {
                rows.add(splitValues(valuesClause.substring(rowStart, index)));
            }
        }
        return rows;
    }

    private static List<String> splitValues(String row) {
        List<String> values = new ArrayList<>();
        int valueStart = 0;
        boolean quoted = false;

        for (int index = 0; index < row.length(); index++) {
            char current = row.charAt(index);
            if (current == '\'' && (index + 1 >= row.length() || row.charAt(index + 1) != '\'')) {
                quoted = !quoted;
            } else if (current == '\'' && quoted) {
                index++;
            } else if (current == ',' && !quoted) {
                values.add(row.substring(valueStart, index).trim());
                valueStart = index + 1;
            }
        }
        values.add(row.substring(valueStart).trim());
        return values;
    }

    private static String normalize(String value) {
        String trimmed = value.trim();
        if ("NULL".equalsIgnoreCase(trimmed)) {
            return null;
        }
        if (trimmed.length() >= 2 && trimmed.startsWith("'") && trimmed.endsWith("'")) {
            return trimmed.substring(1, trimmed.length() - 1).replace("''", "'");
        }
        return trimmed;
    }
}
