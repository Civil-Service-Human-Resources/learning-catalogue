package uk.gov.cslearning.catalogue.api;

import uk.gov.cslearning.catalogue.domain.Course;

import static com.google.common.base.Preconditions.checkArgument;

public class CourseSummary {

    private String id;

    private String title;

    public CourseSummary(Course course) {
        checkArgument(course != null);
        this.id = course.getId();
        this.title = course.getTitle();
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
