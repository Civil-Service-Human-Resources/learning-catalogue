package uk.gov.cslearning.catalogue.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.CivilServant.OrganisationalUnit;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Owner.OwnerFactory;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.FaceToFaceModule;
import uk.gov.cslearning.catalogue.dto.CourseDto;
import uk.gov.cslearning.catalogue.dto.factory.CourseDtoFactory;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.repository.CourseRepositoryImpl;

import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    private final CourseRepositoryImpl courseRepositoryImpl;

    private final EventService eventService;

    private final RegistryService registryService;

    private final OwnerFactory ownerFactory;

    private final AuthoritiesService authoritiesService;

    private RequiredByService requiredByService;

    private final CourseDtoFactory courseDtoFactory;

    public CourseService(CourseRepository courseRepository, EventService eventService, RegistryService registryService, OwnerFactory ownerFactory, AuthoritiesService authoritiesService, RequiredByService requiredByService, CourseDtoFactory courseDtoFactory, CourseRepositoryImpl courseRepositoryImpl) {
        this.courseRepository = courseRepository;
        this.eventService = eventService;
        this.registryService = registryService;
        this.ownerFactory = ownerFactory;
        this.authoritiesService = authoritiesService;
        this.requiredByService = requiredByService;
        this.courseDtoFactory = courseDtoFactory;
        this.courseRepositoryImpl = courseRepositoryImpl;
    }

    public Course save(Course course) {
        return courseRepository.save(course);
    }

    public Course createCourse(Course course, Authentication authentication) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        civilServant.setScope(authoritiesService.getScope(authentication));

        civilServant.setSupplier(authoritiesService.getSupplier(authentication));

        course.setOwner(ownerFactory.create(civilServant, course));

        courseRepository.save(course);

        return course;
    }

    public Course updateCourse(Course course, Course newCourse) {
        course.setTitle(newCourse.getTitle());
        course.setShortDescription(newCourse.getShortDescription());
        course.setLearningOutcomes(newCourse.getLearningOutcomes());
        course.setModules(newCourse.getModules());
        course.setAudiences(newCourse.getAudiences());
        course.setPreparation(newCourse.getPreparation());
        course.setVisibility(newCourse.getVisibility());
        course.setStatus(newCourse.getStatus());
        course.setDescription(newCourse.getDescription());
        course.setTopicId(newCourse.getTopicId());
        Optional.ofNullable(newCourse.getLearningProvider()).ifPresent(course::setLearningProvider);

        courseRepository.save(course);

        return course;
    }

    public Optional<Course> findById(String courseId) {
        return courseRepository.findById(courseId)
                .map(this::getCourseEventsAvailability);
    }

    public Course getCourseById(String courseId) {
        return findById(courseId)
                .orElseThrow((Supplier<IllegalStateException>) () -> {
                    throw new IllegalStateException(
                            String.format("Unable to find course. Course does not exist: %s", courseId));
                });
    }

    private Course getCourseEventsAvailability(Course course) {
        course.getModules().forEach(module -> {
            if (module instanceof FaceToFaceModule) {
                ((FaceToFaceModule) module).getEvents().forEach(event -> {
                    eventService.getEventAvailability(event);
                    event.setStatus(eventService.getStatus(event.getId()));
                });
            }
        });

        return course;
    }

    public Page<Course> findCoursesByOrganisationalUnit(String organisationalUnitCode, Pageable pageable) {
        return courseRepository.findAllByOrganisationCode(organisationalUnitCode, pageable);
    }

    public Page<Course> findCoursesByProfession(String professionId, Pageable pageable) {
        return courseRepository.findAllByProfessionId(professionId, pageable);
    }

    public Page<Course> findCoursesBySupplier(Authentication authentication, Pageable pageable) {
        return courseRepository.findAllBySupplier(authoritiesService.getSupplier(authentication), pageable);
    }

    public Page<Course> findAllCourses(Pageable pageable) {
        return courseRepository.findAll(pageable);
    }

    public Map<String, CourseDto> getPublishedAndArchivedMandatoryCourses() {
        return courseRepositoryImpl
                .findPublishedAndArchivedMandatoryCourses()
                .stream()
                .collect(Collectors.toMap(Course::getId, courseDtoFactory::create));
    }

    public List<String> getOrganisationParents(String departments) {
        List<String> list = new ArrayList<>();
        if (departments != null && !departments.equals("NONE")) {
            List<String> collect = Arrays.stream(departments.split(",")).collect(Collectors.toList());
            collect.forEach(s -> {
                for (OrganisationalUnit organisationalUnit : registryService.getOrganisationalUnit(s)) {
                    if (organisationalUnit != null) {
                        String code = organisationalUnit.getCode();
                        list.add(code);
                    }
                }
            });
        }
        return list;
    }

    public Map<String, List<String>> getOrganisationParentsMap() {
        return registryService.getOrganisationalUnitParentsMap();
    }

    public boolean isCourseRequiredWithinRangeForOrg(Course course, List<String> organisationalUnitList, long from, long to) {
        List<Audience> orgAudiences = course.getAudiences()
                .stream()
                .filter(audience -> organisationalUnitList.stream().anyMatch(organisationalUnit -> audience.getDepartments().contains(organisationalUnit)))
                .collect(Collectors.toList());

        return orgAudiences
                .stream()
                .anyMatch(audience -> requiredByService.isAudienceRequiredWithinRange(audience, Instant.now(), from, to));
    }
}
