package uk.gov.cslearning.catalogue.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.elasticsearch.common.UUIDs;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.Module;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;

@Document(indexName = "courses", type = "course")
public class Course {

    @Id
    private String id = UUIDs.randomBase64UUID();

    @NotNull
    private String title;

    @NotNull
    @Size(max = 160)
    private String shortDescription;

    @NotNull
    @Size(max = 1500)
    private String description;

    private String learningOutcomes;

    private List<Module> modules = new ArrayList<>();

    private LearningProvider learningProvider;

    private Set<Audience> audiences = new HashSet<>();

    private String preparation;

    @NotNull
    private Visibility visibility;

    private Status status = Status.DRAFT;

    public Course() {
    }

    public Course(@NotNull String title, @NotNull String shortDescription, @NotNull String description, @NotNull Visibility visibility) {
        this.title = title;
        this.shortDescription = shortDescription;
        this.description = description;
        this.visibility = visibility;
    }

    public List<Module> getModules() {
        return unmodifiableList(modules);
    }

    public Module getModuleById(String moduleId) {
        List<Module> modules = getModules();
        Optional<Module> module = modules.stream().filter(m -> m.getId().equals(moduleId)).findFirst();
        return module.get();
    }

    public void setModules(List<Module> modules) {
        this.modules.clear();
        if (modules != null) {
            this.modules.addAll(modules);
        }
    }

    public void deleteModule(Module module) {
        for (Iterator<Module> it = modules.iterator(); it.hasNext(); ) {
            Module m = it.next();
            if (m.getId().equals(module.getId())) {
                it.remove();
            }
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

    public LearningProvider getLearningProvider() {
        return learningProvider;
    }

    public void setLearningProvider(LearningProvider learningProvider) {
        this.learningProvider = learningProvider;
    }

    public Set<Audience> getAudiences() {
        return unmodifiableSet(audiences);
    }

    public void setAudiences(Set<Audience> audiences) {
        this.audiences.clear();
        if (audiences != null) {
            this.audiences.addAll(audiences);
        }
    }

    public void deleteAudience(Audience audience) {
        audiences.remove(audience);
    }

    public String getPreparation() {
        return preparation;
    }

    public void setPreparation(String preparation) {
        this.preparation = preparation;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (!(object instanceof Course)) {
            return false;
        }
        Course rhs = (Course) object;
        return this.id.equals(rhs.id);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("id", id)
                .append("title", title)
                .toString();
    }
}
