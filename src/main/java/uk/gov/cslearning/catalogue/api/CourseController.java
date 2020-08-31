package uk.gov.cslearning.catalogue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Status;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.Event;
import uk.gov.cslearning.catalogue.domain.module.FaceToFaceModule;
import uk.gov.cslearning.catalogue.domain.module.Module;
import uk.gov.cslearning.catalogue.mapping.DaysMapper;
import uk.gov.cslearning.catalogue.mapping.RoleMapping;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.service.CourseService;
import uk.gov.cslearning.catalogue.service.EventService;
import uk.gov.cslearning.catalogue.service.ModuleService;
import uk.gov.cslearning.catalogue.service.RegistryService;
import uk.gov.cslearning.catalogue.service.upload.AudienceService;

import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.*;
import static uk.gov.cslearning.catalogue.exception.ResourceNotFoundException.resourceNotFoundException;

@RestController
@RequestMapping("/courses")
public class CourseController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CourseController.class);

    private final CourseRepository courseRepository;

    private final CourseService courseService;

    private final ModuleService moduleService;

    private final EventService eventService;

    private final AudienceService audienceService;

    private final RegistryService registryService;

    @Autowired
    public CourseController(CourseRepository courseRepository, CourseService courseService, ModuleService moduleService,
                            EventService eventService, AudienceService audienceService, RegistryService registryService) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.moduleService = moduleService;
        this.eventService = eventService;
        this.audienceService = audienceService;
        this.registryService = registryService;
    }

    @PostMapping
    @PreAuthorize("(hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_CREATE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity<Void> create(@RequestBody Course course, UriComponentsBuilder builder, Authentication authentication) {
        LOGGER.debug("Creating course {}", course);

        Course newCourse = courseService.createCourse(course, authentication);

        return ResponseEntity.created(builder.path("/courses/{courseId}").build(newCourse.getId())).build();
    }

    @GetMapping
    public ResponseEntity<PageResults<Course>> list(@RequestParam(name = "areaOfWork", defaultValue = "NONE") String areasOfWork,
                                                    @RequestParam(name = "department", defaultValue = "NONE") String departments,
                                                    @RequestParam(name = "interest", defaultValue = "NONE") String interests,
                                                    @RequestParam(name = "status", defaultValue = "Published") String status,
                                                    @RequestParam(name = "grade", defaultValue = "NONE") String grade,
                                                    Pageable pageable) {
        Page<Course> results;
        if (areasOfWork.equals("NONE") && departments.equals("NONE") && interests.equals("NONE")) {
            results = courseRepository.findAllByStatusIn(
                    Arrays.stream(status.split(",")).map(Status::forValue).collect(Collectors.toList()), pageable);
        } else {
            List<String> organisationParents = courseService.getOrganisationParents(departments);
            results = courseRepository.findSuggested(organisationParents, areasOfWork, interests, status, grade, pageable);

            ArrayList<Course> filteredCourses = new ArrayList<>();

            for (Course course : results) {
                for (Audience audience : course.getAudiences()) {
                    for (String organisation : organisationParents) {
                        if (audience.getDepartments().contains(organisation) && audience.getGrades().contains(grade)) {
                            filteredCourses.add(course);
                        }
                    }
                    if (audience.getAreasOfWork().contains(areasOfWork) && audience.getGrades().contains(grade)) {
                        filteredCourses.add(course);
                    }
                    if (audience.getInterests().contains(interests) && audience.getGrades().contains(grade)) {
                        filteredCourses.add(course);
                    }
                }
            }

            Set<Course> set = new LinkedHashSet<>();
            set.addAll(filteredCourses);
            filteredCourses.clear();
            filteredCourses.addAll(set);

            results = new PageImpl<>(filteredCourses, pageable, filteredCourses.size());
        }

        return ResponseEntity.ok(new PageResults<>(results, pageable));
    }

    @GetMapping(params = {"mandatory", "department"})
    public ResponseEntity<PageResults<Course>> listMandatory(@RequestParam("department") String department,
                                                             @RequestParam(value = "status", defaultValue = "Published") String status,
                                                             Pageable pageable) {
        LOGGER.debug("Listing mandatory courses for department {}", department);
        List<String> organisationParents = courseService.getOrganisationParents(department);

        List<Course> courses = new ArrayList<>();
        for (String d : organisationParents) {
            courses.addAll(courseRepository.findMandatory(d, status, pageable));
        }

        Set<String> courseSet = new HashSet<>();
        List<Course> filteredCourses = courses.stream()
                .filter(e -> courseSet.add(e.getId()))
                .collect(Collectors.toList());

        Page<Course> page = new PageImpl<>(filteredCourses, pageable, courses.size());

        return ResponseEntity.ok(new PageResults<>(page, pageable));
    }

    @GetMapping(params = {"mandatory", "days"})
    public ResponseEntity<Map<String, List<Course>>> listMandatoryByDueDays(@RequestParam(value = "status", defaultValue = "Published") String status,
        @RequestParam(value = "days", defaultValue = "1") String days) {
        LOGGER.debug("Listing mandatory courses");
        List<Course> courses = courseService.fetchMandatoryCoursesByDueDate(status, DaysMapper.convertDaysFromTextToNumeric(days));

        return ResponseEntity.ok(courseService.groupByOrganisationCode(courses));
    }

    @GetMapping(value = "/required")
    public ResponseEntity<Map<String, List<Course>>> getRequiredLearningByOrgCodeMap() {
        Map<String, List<Course>> requiredCoursesByOrgCode = new HashMap<>();

        Map<String, List<String>> organisationParentsMap = courseService.getOrganisationParentsMap();
        organisationParentsMap.forEach((s, organisationalUnitList) -> {
            LOGGER.info("Getting required courses for {}", s);

            List<Course> courses = courseRepository.findMandatoryOfMultipleDepts(organisationalUnitList, "Published", PageRequest.of(0, 10000));

            Set<String> courseSet = new HashSet<>();
            List<Course> filteredCourses = courses
                    .stream()
                    .filter(e -> courseSet.add(e.getId()))
                    .collect(Collectors.toList());

            requiredCoursesByOrgCode.put(s, filteredCourses);
        });
        return ResponseEntity.ok(requiredCoursesByOrgCode);
    }

    @GetMapping(value = "/required", params = {"from", "to"})
    public ResponseEntity<Map<String, List<Course>>> getRequiredLearningByOrgCodeMapDueWithinRange(@RequestParam("from") long from, @RequestParam("to") long to) {
        Map<String, List<Course>> requiredCoursesByOrgCode = new HashMap<>();

        Map<String, List<String>> organisationParentsMap = courseService.getOrganisationParentsMap();
        organisationParentsMap.forEach((s, organisationalUnitList) -> {
            LOGGER.info("Getting required courses for {}", s);

            List<Course> courses = courseRepository.findMandatoryOfMultipleDepts(organisationalUnitList, "Published", PageRequest.of(0, 10000));

            Set<String> courseSet = new HashSet<>();
            List<Course> filteredCourses = courses
                    .stream()
                    .filter(e -> courseSet.add(e.getId()))
                    .filter(course -> courseService.isCourseRequiredWithinRangeForOrg(course, organisationalUnitList, from, to))
                    .collect(Collectors.toList());

            if (!filteredCourses.isEmpty()) {
                requiredCoursesByOrgCode.put(s, filteredCourses);
            }
        });
        return ResponseEntity.ok(requiredCoursesByOrgCode);
    }


    @RoleMapping("ORGANISATION_AUTHOR")
    @GetMapping(value = "/management")
    public ResponseEntity<PageResults<Course>> listForOrganisation(Pageable pageable) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        return civilServant.getOrganisationalUnitCode()
                .map(organisationalUnitCode -> {
                    Page<Course> results = courseService.findCoursesByOrganisationalUnit(organisationalUnitCode, pageable);
                    return new ResponseEntity<>(new PageResults<>(results, pageable), OK);
                }).orElseGet(() -> new ResponseEntity<>(new PageResults<>(Page.empty(), pageable), OK));
    }

    @RoleMapping("PROFESSION_AUTHOR")
    @GetMapping(value = "/management")
    public ResponseEntity<PageResults<Course>> listForProfession(Pageable pageable) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        return civilServant.getProfessionId()
                .map(professionId -> {
                    Page<Course> results = courseService.findCoursesByProfession(professionId.toString(), pageable);
                    return new ResponseEntity<>(new PageResults<>(results, pageable), OK);
                }).orElseGet(() -> new ResponseEntity<>(new PageResults<>(Page.empty(), pageable), OK));
    }

    @RoleMapping({"KPMG_SUPPLIER_AUTHOR", "KORNFERRY_SUPPLIER_AUTHOR", "KNOWLEDGEPOOL_SUPPLIER_AUTHOR"})
    @GetMapping(value = "/management")
    public ResponseEntity<PageResults<Course>> listForSupplier(Pageable pageable, Authentication authentication) {
        Page<Course> results = courseService.findCoursesBySupplier(authentication, pageable);
        return new ResponseEntity<>(new PageResults<>(results, pageable), OK);
    }

    @RoleMapping({"CSL_AUTHOR", "LEARNING_MANAGER"})
    @GetMapping(value = "/management")
    public ResponseEntity<PageResults<Course>> listForCslAuthorOrLearningManager(Pageable pageable) {
        Page<Course> results = courseService.findAllCourses(pageable);

        return new ResponseEntity<>(new PageResults<>(results, pageable), OK);
    }

    @GetMapping(value = "/management")
    public ResponseEntity<PageResults<Course>> unauth(Principal principal) {
        LOGGER.debug("Unauthorised. Required role not found in %s", principal);
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    @GetMapping(params = "courseId")
    public ResponseEntity<Iterable<Course>> get(@RequestParam("courseId") List<String> courseIds) {
        LOGGER.debug("Getting courses with IDs {}", courseIds);
        Iterable<Course> result = courseRepository.findAllById(courseIds);
        return new ResponseEntity<>(result, OK);
    }


    @PostMapping(value = "/getIds")
    public ResponseEntity<Iterable<Course>> getIds(@RequestBody List<String> courseIds) {
        LOGGER.debug("Getting courses with IDs {}", courseIds);
        Iterable<Course> result = courseRepository.findAllById(courseIds);
        return new ResponseEntity<>(result, OK);
    }


    @GetMapping("/{courseId}")
    public ResponseEntity<Course> get(@PathVariable("courseId") String courseId) {
        LOGGER.debug("Getting course with ID {}", courseId);

        Optional<Course> result = courseService.findById(courseId);

        return result
                .map(course -> new ResponseEntity<>(course, OK))
                .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }

    /**
     * Adding individual endpoints for edit, publish and archive to ensure we can preauthorise publish, archive and edit separately.
     * This edit endpoint will be used for updating course title and details.
     */
    @PutMapping(path = "/{courseId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_EDIT, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity update(@PathVariable("courseId") String courseId, @RequestBody Course newCourse) {
        return updateCourse(courseId, newCourse);
    }

    /**
     * This endpoint will be used for publishing courses.
     */
    @PutMapping(path = "/{courseId}/publish")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_PUBLISH, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity publishCourse(@PathVariable("courseId") String courseId, @RequestBody Course newCourse) {
        return updateCourse(courseId, newCourse);
    }

    /**
     * This endpoint will be used for archiving courses.
     */
    @PutMapping(path = "/{courseId}/archive")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_ARCHIVE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity archiveCourse(@PathVariable("courseId") String courseId, @RequestBody Course newCourse) {
        return updateCourse(courseId, newCourse);
    }


    @PostMapping("/{courseId}/modules")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_CREATE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity<Void> createModule(@PathVariable String courseId, @RequestBody Module module, UriComponentsBuilder builder) {
        LOGGER.debug("Adding module to course with ID {}", courseId);

        Module saved = moduleService.save(courseId, module);

        LOGGER.info("Saved module {}", saved);

        return ResponseEntity.created(builder.path("/courses/{courseId}/modules/{moduleId}").build(courseId, saved.getId())).build();
    }

    @GetMapping("/{courseId}/modules/{moduleId}")
    public ResponseEntity<Module> getModule(@PathVariable String courseId, @PathVariable String moduleId) {
        LOGGER.debug("Getting module {} of course {}", moduleId, courseId);

        Optional<Module> result = moduleService.find(courseId, moduleId);

        return result.map(module -> new ResponseEntity<>(module, OK))
                .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_DELETE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity deleteModule(@PathVariable String courseId, @PathVariable String moduleId) {
        LOGGER.debug("Deleting module, course ID {}, module ID {}", courseId, moduleId);

        moduleService.deleteModule(courseId, moduleId);

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{courseId}/modules/{moduleId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_EDIT, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity updateModule(@PathVariable String courseId, @PathVariable String moduleId, @RequestBody Module module) {
        LOGGER.debug("Updating module {} in course {}", moduleId, courseId);

        Optional<Module> optionalModule = moduleService.find(courseId, moduleId);

        return optionalModule
                .map(m -> {
                    moduleService.updateModule(courseId, module);
                    return ResponseEntity.noContent().build();
                }).orElseGet(() -> new ResponseEntity<>(BAD_REQUEST));
    }

    @PostMapping("/{courseId}/modules/{moduleId}/events")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_CREATE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity createEvent(@PathVariable String courseId, @PathVariable String moduleId, @RequestBody Event event, UriComponentsBuilder builder) {
        LOGGER.debug("Adding event to module with ID {}", moduleId);

        Event saved = eventService.save(courseId, moduleId, event);

        LOGGER.info("Saved event {}", saved);

        return ResponseEntity.created(builder.path("/courses/{courseId}/modules/{moduleId}/events/{eventId}").build(courseId, moduleId, saved.getId())).build();
    }

    @GetMapping("/{courseId}/modules/{moduleId}/events/{eventId}")
    public ResponseEntity<Event> getEvent(@PathVariable String courseId, @PathVariable String moduleId, @PathVariable String eventId) {
        LOGGER.debug("Getting event {} of module {} of course {}", eventId, moduleId, courseId);

        Optional<Event> result = eventService.find(courseId, moduleId, eventId);

        return result.map(event -> new ResponseEntity<>(event, OK)).orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }

    @PutMapping("/{courseId}/modules/{moduleId}/events/{eventId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_EDIT, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity<Event> updateEvent(@PathVariable String courseId, @PathVariable String moduleId, @PathVariable String eventId, @RequestBody Event newEvent) {
        LOGGER.debug("Updating event with ID {}", eventId);

        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.badRequest().build();
        }
        if (courseRepository.findById(courseId).get().getModuleById(moduleId) == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Course> result = courseRepository.findById(courseId);

        return result.map(course -> {
            Module module = course.getModuleById(moduleId);

            if (module instanceof FaceToFaceModule) {
                FaceToFaceModule faceToFaceModule = (FaceToFaceModule) module;

                Event event = faceToFaceModule.getEventById(eventId);

                event.setDateRanges(newEvent.getDateRanges());
                event.setJoiningInstructions(newEvent.getJoiningInstructions());

                Optional.ofNullable(newEvent.getVenue()).ifPresent(event::setVenue);

                courseRepository.save(course);

                return ResponseEntity.ok().body(event);
            }

            return ResponseEntity.badRequest().body(newEvent);
        }).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @DeleteMapping("/{courseId}/modules/{moduleId}/events/{eventId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_DELETE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity deleteEvent(@PathVariable String courseId, @PathVariable String moduleId, @PathVariable String eventId) {
        LOGGER.debug("Deleting event with id {}", eventId);
        if (!courseRepository.existsById(courseId)) {
            return ResponseEntity.badRequest().build();
        }
        if (courseRepository.findById(courseId).get().getModuleById(moduleId) == null) {
            return ResponseEntity.badRequest().build();
        }

        Optional<Course> result = courseRepository.findById(courseId);

        return result.map(course -> {
            Module module = course.getModuleById(moduleId);

            if (module instanceof FaceToFaceModule) {
                FaceToFaceModule faceToFaceModule = (FaceToFaceModule) module;

                Event event = faceToFaceModule.getEventById(eventId);

                faceToFaceModule.removeEvent(event);

                courseRepository.save(course);

                return ResponseEntity.noContent().build();
            }

            return ResponseEntity.badRequest().build();
        }).orElseGet(() -> ResponseEntity.badRequest().build());
    }

    @PostMapping("/{courseId}/audiences")
    @PreAuthorize("(hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_CREATE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity<Void> createAudience(@PathVariable String courseId, @RequestBody Audience audience, UriComponentsBuilder builder, Authentication authentication) {
        LOGGER.debug("Adding audience to course with ID {}", courseId);

        if (!audienceService.isPermitted(courseId, authentication)) {
            return ResponseEntity.status(403).build();
        }

        audience = audienceService.setDefaults(authentication, audience);

        audienceService.save(courseId, audience);

        LOGGER.info("Saved audience {}", audience.toString());

        return ResponseEntity.created(builder.path("/courses/{courseId}/audiences/{audienceId}").build(courseId, audience.getId())).build();
    }

    @GetMapping("/{courseId}/audiences/{audienceId}")
    public ResponseEntity<Audience> getAudience(@PathVariable String courseId, @PathVariable String audienceId) {
        LOGGER.debug("Getting audience {} of course {}", audienceId, courseId);

        Optional<Audience> result = audienceService.find(courseId, audienceId);

        return result.map(audience -> new ResponseEntity<>(audience, OK))
                .orElseGet(() -> new ResponseEntity<>(NOT_FOUND));
    }

    @PutMapping("/{courseId}/audiences/{audienceId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_EDIT, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity updateAudience(@PathVariable String courseId, @PathVariable String audienceId, @RequestBody Audience newAudience) {
        LOGGER.debug("Updating audience {} in course {}", audienceId, courseId);

        return courseService.findById(courseId)
                .map(course -> audienceService.find(course.getId(), audienceId)
                        .map(audience -> {
                            audienceService.updateAudience(course, newAudience, audience);
                            return new ResponseEntity<>(NO_CONTENT);
                        }).orElseGet(() -> new ResponseEntity<>(BAD_REQUEST)))
                .orElseGet(() -> new ResponseEntity<>(BAD_REQUEST));
    }

    @DeleteMapping("/{courseId}/audiences/{audienceId}")
    @PreAuthorize("(hasPermission(#courseId, 'write') and hasAnyAuthority(T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_DELETE, T(uk.gov.cslearning.catalogue.domain.Roles).LEARNING_MANAGER, T(uk.gov.cslearning.catalogue.domain.Roles).CSL_AUTHOR))")
    public ResponseEntity deleteAudience(@PathVariable String courseId, @PathVariable String audienceId, Authentication authentication) {
        LOGGER.debug("Deleting audience, course ID {}, audience ID {}", courseId, audienceId);

        courseRepository.findById(courseId)
                .map(course -> audienceService.find(course, audienceId)
                        .map(audience -> {
                            if (!audienceService.isPermitted(courseId, authentication)) {
                                return ResponseEntity.badRequest().build();
                            }
                            course.deleteAudience(audience);
                            return courseRepository.save(course);
                        })
                        .orElseThrow(() -> resourceNotFoundException())
                )
                .orElseThrow(() -> resourceNotFoundException());

        return ResponseEntity.noContent().build();
    }

    private ResponseEntity updateCourse(@PathVariable("courseId") String courseId, @RequestBody Course newCourse) {
        LOGGER.debug("Updating course {}", newCourse);

        return courseService.findById(courseId)
                .map(course -> {
                    courseService.updateCourse(course, newCourse);
                    return new ResponseEntity<>(NO_CONTENT);
                })
                .orElseGet(() -> new ResponseEntity<>(BAD_REQUEST));
    }
}
