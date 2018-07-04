package uk.gov.cslearning.catalogue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Resource;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.repository.ResourceRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseController.class);

    private CourseRepository courseRepository;

    private ResourceRepository resourceRepository;

    @Autowired
    public CourseController(CourseRepository courseRepository, ResourceRepository resourceRepository) {
        checkArgument(courseRepository != null);
        this.courseRepository = courseRepository;
        this.resourceRepository = resourceRepository;

    }

    @PostMapping
    public ResponseEntity<Void> create(@RequestBody Course course, UriComponentsBuilder builder) {
        LOGGER.debug("Creating course {}", course);
        Course newCourse = courseRepository.save(course);

        ArrayList<Resource> resources = Resource.fromCourse(newCourse);
        for (Resource resource : resources) {
            LOGGER.debug("Creating resource {}", resource);
            resourceRepository.save(resource);
        }

        return ResponseEntity.created(builder.path("/courses/{courseId}").build(newCourse.getId())).build();
    }

    @GetMapping(params = {"mandatory", "department"})
    public ResponseEntity<PageResults<Course>> listMandatory(@RequestParam("department") String department,
                                                             PageParameters pageParameters) {
        LOGGER.debug("Listing mandatory courses for department {}", department);
        Pageable pageable = pageParameters.getPageRequest();
        Page<Course> page = courseRepository.findMandatory(department, pageParameters.getPageRequest());
        return ResponseEntity.ok(new PageResults<>(page, pageable));
    }

    @GetMapping
    public ResponseEntity<PageResults<Course>> list(@RequestParam(name = "areaOfWork", required = false) List<String> areasOfWork,
                                                    @RequestParam(name = "department", required = false) List<String> departments,
                                                    PageParameters pageParameters) {
        LOGGER.debug("Listing courses");

        areasOfWork = defaultIfNull(areasOfWork, emptyList());
        departments = defaultIfNull(departments, emptyList());

        Pageable pageable = pageParameters.getPageRequest();
        Page<Course> page;

        if (departments.isEmpty() && areasOfWork.isEmpty()) {
            page = courseRepository.findAll(pageable);
        } else {
            page = courseRepository.findSuggested(
                    String.join(",", departments),
                    String.join(",", areasOfWork),
                    pageable);
        }
        return ResponseEntity.ok(new PageResults<>(page, pageable));
    }

    @GetMapping(params = {"interest"})
    public ResponseEntity<PageResults<Course>> listByInterest(@RequestParam(name = "interest", required = false) List<String> interests,
                                                    PageParameters pageParameters) {
        LOGGER.debug("Listing courses by interest");

        interests = defaultIfNull(interests, emptyList());

        Pageable pageable = pageParameters.getPageRequest();
        Page<Course> page;

        if (interests.isEmpty()) {
            page = courseRepository.findAll(pageable);
        } else {
            page = courseRepository.findSuggestedByInterest(
                    String.join(",", interests),
                    pageable);
        }
        return ResponseEntity.ok(new PageResults<>(page, pageable));
    }

    @PutMapping(path = "/{courseId}")
    public ResponseEntity<Void> update(@PathVariable("courseId") String courseId, @RequestBody Course course) {
        LOGGER.debug("Updating course {}", course);
        if (!courseId.equals(course.getId())) {
            return ResponseEntity.badRequest().build();
        }
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.badRequest().build();
        }
        courseRepository.save(course);

        ArrayList<Resource> resources = Resource.fromCourse(course);
        for (Resource resource : resources) {
            LOGGER.debug("Creating resource {}", resource);
            resourceRepository.save(resource);
        }

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<Course> get(@PathVariable("courseId") String courseId) {
        LOGGER.debug("Getting course with ID {}", courseId);
        Optional<Course> result = courseRepository.findById(courseId);
        return result
                .map(course -> new ResponseEntity<>(course, OK))
                .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }
}
