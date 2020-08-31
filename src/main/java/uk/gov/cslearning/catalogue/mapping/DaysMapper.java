package uk.gov.cslearning.catalogue.mapping;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class DaysMapper {
    private static final String DAYS_SEPARATOR = ",";

    private DaysMapper() {
    }

    public static List<Long> convertDaysFromTextToNumeric(String days) {
        String[] textDays = days.split(DAYS_SEPARATOR);
        List<Long> numericDays = new ArrayList<>();

        if (StringUtils.isNoneBlank(days)) {
            addDaysToList(textDays, numericDays);
        }

        return numericDays;
    }

    private static void addDaysToList(String[] textDays, List<Long> numericDays) {
        for (String textDay : textDays) {
            addParsedTextDay(textDay, numericDays);
        }
    }

    private static void addParsedTextDay(String textDay, List<Long> numericDays) {
        if (!StringUtils.isNumeric(textDay)) {
            throw new IllegalArgumentException(String.format("Invalid due days value: %s", textDay));
        } else {
            numericDays.add(Long.valueOf(textDay));
        }
    }
}