package uk.gov.cslearning.catalogue.mapping;


import static org.junit.Assert.assertEquals;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

public class DaysMapperTest {
    private static final String PROPER_VALUES = "1,7,30";
    private static final String ONE_VALUE = "1";
    private static final String TEXT_VALUES = "One,Seven,Thirty";
    private static final String PARTIAL_TEXT_VALUES = "1,Seven,30";

    @Test
    public void shouldMapStringValuesToLongValues() {
        List<Long> values = DaysMapper.convertDaysFromTextToNumeric(PROPER_VALUES);
        assertEquals(values.size(), 3);
        assertEquals(values.get(0), new Long(1));
        assertEquals(values.get(1), new Long(7));
        assertEquals(values.get(2), new Long(30));
    }

    @Test
    public void shouldMapOneStringValueToLongValue() {
        List<Long> values = DaysMapper.convertDaysFromTextToNumeric(ONE_VALUE);
        assertEquals(values.size(), 1);
        assertEquals(values.get(0), new Long(1));
    }

    @Test
    public void shouldMapEmptyStringToEmptyList() {
        List<Long> values = DaysMapper.convertDaysFromTextToNumeric(StringUtils.EMPTY);
        assertEquals(values.size(), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotMapTextValues() {
        DaysMapper.convertDaysFromTextToNumeric(TEXT_VALUES);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotMapPartialTextValues() {
        DaysMapper.convertDaysFromTextToNumeric(PARTIAL_TEXT_VALUES);
    }
}