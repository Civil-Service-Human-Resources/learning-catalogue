package uk.gov.cslearning.catalogue.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.elasticsearch.common.UUIDs;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import uk.gov.cslearning.catalogue.domain.module.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

@Document(indexName = "lpg-courses", type = "course")
public class Course {

    @Id
    private String id = UUIDs.randomBase64UUID();

    private String title;

    private String shortDescription;

    private String description;

    private String learningOutcomes;

    private List<Module> modules = new ArrayList<>();

    public Course() {
    }

    public Course(String title, String shortDescription, String description, String learningOutcomes) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.learningOutcomes = learningOutcomes;
    }


    public List<Module> getModules() {
        return unmodifiableList(modules);
    }

    public void setModules(List<Module> modules) {
        this.modules.clear();
        if (modules != null) {
            this.modules.addAll(modules);
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLearningOutcomes() {
        return learningOutcomes;
    }

    public void setLearningOutcomes(String learningOutcomes) {
        this.learningOutcomes = learningOutcomes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course course = (Course) o;
        return Objects.equals(id, course.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("title", title)
                .toString();
    }
}
