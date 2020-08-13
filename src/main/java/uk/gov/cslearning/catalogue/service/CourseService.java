package uk.gov.cslearning.catalogue.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.CivilServant.OrganisationalUnit;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Owner.OwnerFactory;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.FaceToFaceModule;
import uk.gov.cslearning.catalogue.repository.CourseRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class CourseService {

    private final CourseRepository courseRepository;

    private final EventService eventService;

    private final RegistryService registryService;

    private final OwnerFactory ownerFactory;

    private final AuthoritiesService authoritiesService;

    private RequiredByService requiredByService;

    public CourseService(CourseRepository courseRepository, EventService eventService, RegistryService registryService, OwnerFactory ownerFactory, AuthoritiesService authoritiesService, RequiredByService requiredByService) {
        this.courseRepository = courseRepository;
        this.eventService = eventService;
        this.registryService = registryService;
        this.ownerFactory = ownerFactory;
        this.authoritiesService = authoritiesService;
        this.requiredByService = requiredByService;
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

    public List<Course> fetchMandatoryCourses(String status, String department, Pageable pageable) { ;
        Set<Course> mandatoryCoursesWithValidAudience = new HashSet<>();

        courseRepository.findAllRequiredLearning(status, pageable)
            .forEach(course -> course.getAudiences()
                .forEach(audience -> addCourseIfAudienceIsRequired(course, audience, department, mandatoryCoursesWithValidAudience)));

        return new ArrayList(mandatoryCoursesWithValidAudience);
    }

    public List<Course> fetchMandatoryCoursesByDueDate(String status, Collection<Long> days, Instant now) { ;
        Set<Course> mandatoryCoursesWithValidAudience = new HashSet<>();

        courseRepository.findAllRequiredLearning(status)
            .forEach(course -> course.getAudiences()
                .forEach(audience -> addCourseIfAudienceIsRequired(course, audience, mandatoryCoursesWithValidAudience, days, now)));

        return new ArrayList(mandatoryCoursesWithValidAudience);
    }

    public Page<Course> prepareCoursePage(Pageable pageable, List<Course> courses) {
        Set<String> courseSet = new HashSet<>();
        List<Course> filteredCourses = courses.stream()
            .filter(course -> courseSet.add(course.getId()))
            .collect(Collectors.toList());

        return new PageImpl<>(filteredCourses, pageable, courses.size());
    }

    public Map<String, List<Course>> groupByOrganisationCode(List<Course> courses) {
        Map<String, List<Course>> groupedCourses = new HashMap<>();

        for (Course course : courses) {
            for (Audience audience : course.getAudiences()) {
                addToGroupedCourses(courses, groupedCourses, audience);
            }
        }

        return groupedCourses;
    }

    private void addToGroupedCourses(List<Course> courses, Map<String, List<Course>> groupedCourses, Audience audience) {
        for (String department : audience.getDepartments()) {
            if (!groupedCourses.containsKey(department)) {
                groupedCourses.putIfAbsent(department, new ArrayList<>(courses));
            } else {
                groupedCourses.get(department)
                    .addAll(courses);
            }
        }
    }

    private void addCourseIfAudienceIsRequired(Course course, Audience audience, String department, Set<Course> mandatoryCoursesWithValidAudience) {
        if (isAudienceRequired(audience, department)) {
            mandatoryCoursesWithValidAudience.add(course);
        }
    }

    private void addCourseIfAudienceIsRequired(Course course,
            Audience audience,
            Set<Course> mandatoryCoursesWithValidAudience,
            Collection<Long> days,
            Instant now) {
        if (isAudienceRequired(audience, days, now)) {
            mandatoryCoursesWithValidAudience.add(course);
        }
    }

    private boolean isAudienceRequired(Audience audience, String department) {
        return audience.getRequiredBy() != null
            && audience.getDepartments() != null
            && audience.getDepartments().contains(department);
    }

    private boolean isAudienceRequired(Audience audience, Collection<Long> days, Instant now) {
        return audience.getRequiredBy() != null
            && audience.getDepartments() != null
            && isRequiredDateDue(audience.getRequiredBy(), days, now);
    }

    private boolean isRequiredDateDue(Instant requiredBy, Collection<Long> days, Instant now) {
        return days.contains(ChronoUnit.DAYS.between(now, requiredBy));
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
}
