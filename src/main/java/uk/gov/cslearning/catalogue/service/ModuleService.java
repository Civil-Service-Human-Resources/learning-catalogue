package uk.gov.cslearning.catalogue.service;

import org.springframework.stereotype.Service;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.module.LinkModule;
import uk.gov.cslearning.catalogue.domain.module.Module;
import uk.gov.cslearning.catalogue.repository.CourseRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

@Service
public class ModuleService {
    private final CourseRepository courseRepository;

    public ModuleService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Module save(String courseId, Module module) {
        Course course = courseRepository.findById(courseId).orElseThrow((Supplier<IllegalStateException>) () -> {
            throw new IllegalStateException(
                    String.format("Unable to add module. Course does not exist: %s", courseId));
        });

        List<Module> modules = new ArrayList<>(course.getModules());
        modules.add(module);
        course.setModules(modules);
        courseRepository.save(course);

        return module;
    }

    public Optional<Module> find(String courseId, String moduleId) {
        Course course = courseRepository.findById(courseId).orElseThrow((Supplier<IllegalStateException>) () -> {
            throw new IllegalStateException(
                    String.format("Unable to find module: %s. Course does not exist: %s", moduleId, courseId));
        });

        return course.getModules().stream()
                .filter(m -> m.getId().equals(moduleId))
                .findFirst();
    }

    public Module update(String courseId, Module newModule) {
        Course course = courseRepository.findById(courseId).orElseThrow((Supplier<IllegalStateException>) () -> {
            throw new IllegalStateException(
                    String.format("Unable to add module. Course does not exist: %s", courseId));
        });

        List<Module> modules = new ArrayList<>(course.getModules());
        for(Module module: modules) {
            if (module.getId().equals(newModule.getId())) {
                String type = newModule.getModuleType();
                switch (type) {
                    case "link":
                        updateLinkModule((LinkModule) module, (LinkModule) newModule);
                }
            }
        }

        course.setModules(modules);
        courseRepository.save(course);

        return newModule;
    }


    private void updateLinkModule(LinkModule module, LinkModule newModule) {
        module.setTitle(newModule.getTitle());
        module.setDescription(newModule.getDescription());
        module.setCost(newModule.getCost());
        module.setDuration(newModule.getDuration());
        module.setOptional(newModule.isOptional());
        module.setUrl(newModule.getUrl());
    }

}
