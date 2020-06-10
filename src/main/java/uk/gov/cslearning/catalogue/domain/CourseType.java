package uk.gov.cslearning.catalogue.domain;

public enum CourseType {

    REQUIRED_LEARNING("REQUIRED_LEARNING");

    private final String value;

    CourseType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
