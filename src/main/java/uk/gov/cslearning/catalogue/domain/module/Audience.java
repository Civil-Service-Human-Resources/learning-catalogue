package uk.gov.cslearning.catalogue.domain.module;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import uk.gov.cslearning.catalogue.domain.Frequency;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Audience {

    private Set<String> areasOfWork = new HashSet<>();

    private Set<String> departments = new HashSet<>();

    private Set<String> grades = new HashSet<>();

    private Set<String> interests = new HashSet<>();

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    private LocalDateTime requiredBy;

    private Frequency frequency;

    private Boolean mandatory;

    public Audience() {
    }

    public Set<String> getAreasOfWork() {
        return areasOfWork;
    }

    public void setAreasOfWork(Set<String> areasOfWork) {
        this.areasOfWork = areasOfWork;
    }

    public Set<String> getDepartments() {
        return departments;
    }

    public void setDepartments(Set<String> departments) {
        this.departments = departments;
    }

    public Set<String> getGrades() {
        return grades;
    }

    public void setGrades(Set<String> grades) {
        this.grades = grades;
    }

    public Set<String> getInterests() {
        return interests;
    }

    public void setInterests(Set<String> interests) {
        this.interests = interests;
    }

    public LocalDateTime getRequiredBy() {
        return requiredBy;
    }

    public void setRequiredBy(LocalDateTime requiredBy) {
        this.requiredBy = requiredBy;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public void setFrequency(Frequency frequency) {
        this.frequency = frequency;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
    }
}
