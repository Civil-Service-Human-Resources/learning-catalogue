package uk.gov.cslearning.catalogue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.CivilServant.Interest;
import uk.gov.cslearning.catalogue.domain.CivilServant.Profession;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Status;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.Event;
import uk.gov.cslearning.catalogue.domain.module.FaceToFaceModule;
import uk.gov.cslearning.catalogue.domain.module.Module;
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

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.containsAny;
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

    public static final String COURSE_STATUS = "Published";

    public static final String ELASTIC_EMPTY_PARAM = "NONE";

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
    public ResponseEntity<PageResults<Course>> list(@RequestParam(name = "areaOfWork", defaultValue = ELASTIC_EMPTY_PARAM) String areasOfWork,
                                                    @RequestParam(name = "department", defaultValue = ELASTIC_EMPTY_PARAM) String departments,
                                                    @RequestParam(name = "interest", defaultValue = ELASTIC_EMPTY_PARAM) String interests,
                                                    @RequestParam(name = "status", defaultValue = COURSE_STATUS) String status,
                                                    @RequestParam(name = "grade", defaultValue = ELASTIC_EMPTY_PARAM) String grade,
                                                    Pageable pageable) {
        Page<Course> results;
        if (areasOfWork.equals(ELASTIC_EMPTY_PARAM) && departments.equals(ELASTIC_EMPTY_PARAM) && interests.equals(ELASTIC_EMPTY_PARAM)) {
            results = courseRepository.findAllByStatusIn(
                    Arrays.stream(status.split(",")).map(Status::forValue).collect(Collectors.toList()), pageable);
        } else {
            List<String> organisationParents = courseService.getOrganisationParents(departments);
            results = courseRepository.findSuggested(organisationParents, areasOfWork, interests, status, grade, pageable);

            CivilServant civilServant = registryService.getCurrentCivilServant();
            String organisationCode = civilServant.getOrganisationalUnitCode().get();
            List<Profession> otherAreasOfWork = civilServant.getOtherAreasOfWork();
            String professionName = civilServant.getProfessionName().get();

            List<String> otherAreasOfWorkNames = otherAreasOfWork.stream()
                    .map(Profession::getName)
                    .collect(collectingAndThen(toList(), this::listWithNone));

            List<String> organisationParentChild = courseService.getOrganisationParents(organisationCode);
            List<Interest> interests_cs = civilServant.getInterests();

            List<String> interestNames = interests_cs.stream()
                    .map(Interest::getName)
                    .collect(collectingAndThen(toList(), this::listWithNone));

            ArrayList<Course> filteredCourses = new ArrayList<>();

            for (Course course : results) {
                for (Audience audience : course.getAudiences()) {
                    for (String organisation : organisationParents) {
                        // any course that has a dept defined (check if AOW and Interests are also part of audience).
                        if (audience.getDepartments().contains(organisation) && audience.getGrades().contains(grade)
                                && isAreaOfWorkValid(audience, otherAreasOfWorkNames, professionName)
                                && (audience.getInterests().isEmpty() || containsAny(audience.getInterests(), interestNames))
                                && audience.getType().equals(Audience.Type.OPEN)) {
                            filteredCourses.add(course);
                        }
                    }

                    // Show any courses with AOW (audience could also have a dept/interest so filter it if it does ie it could be Required for CO but Open for HMRC
                    // causing it to appear in both req and suggested for same user)
                    if (audience.getAreasOfWork().contains(areasOfWork) && audience.getGrades().contains(grade)
                            && (audience.getDepartments().isEmpty() || containsAny(audience.getDepartments(),organisationParentChild))
                            && (audience.getInterests().isEmpty() || containsAny(audience.getInterests(), interestNames))
                            && audience.getType().equals(Audience.Type.OPEN)) {
                        filteredCourses.add(course);
                    }

                    // Show any courses with Interest (audience could also have a dept/aow so filter it if it does ie it could be Required for CO but Open for HMRC
                    // causing it to appear in both req and suggested for same user)
                    if (audience.getInterests().contains(interests) && audience.getGrades().contains(grade)
                            && (audience.getDepartments().isEmpty() || containsAny(audience.getDepartments(),organisationParentChild))
                            && isAreaOfWorkValid(audience, otherAreasOfWorkNames, professionName)
                            && audience.getType().equals(Audience.Type.OPEN)) {
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


    @GetMapping(value = "/getrequiredlearning")
    public ResponseEntity<PageResults<Course>> listMandatory(@PageableDefault(size = 100) Pageable pageable) {

        CivilServant civilServant = registryService.getCurrentCivilServant();

        String userName = ELASTIC_EMPTY_PARAM;
        String professionName = ELASTIC_EMPTY_PARAM;
        String grade = ELASTIC_EMPTY_PARAM;
        String organisationCode = ELASTIC_EMPTY_PARAM;

        if (civilServant.getFullName().isPresent()) {
            userName = civilServant.getFullName().get();
        }

        if (civilServant.getProfessionName().isPresent()) {
            professionName = civilServant.getProfessionName().get();
        }

        if (civilServant.getGrade().isPresent()) {
            grade = civilServant.getGradeCode().get();
        }

        if (civilServant.getOrganisationalUnitCode().isPresent()) {
            organisationCode = civilServant.getOrganisationalUnitCode().get();
        }

        List<Profession> otherAreasOfWork = (civilServant.getOtherAreasOfWork());
        List<Interest> interests = civilServant.getInterests();

        List<String> organisationParentandChild = courseService.getOrganisationParents(organisationCode);

        List<String> interestNames = interests.stream()
                .map(Interest::getName)
                .collect(collectingAndThen(toList(), this::listWithNone));

        List<String> otherAreasOfWorkNames = otherAreasOfWork.stream()
                .map(Profession::getName)
                .collect(collectingAndThen(toList(), this::listWithNone));

        LOGGER.debug("Listing Required Learning courses for user {}", userName);

        Page<Course> results = courseService.getRequiredCourses(professionName, grade, organisationParentandChild, otherAreasOfWorkNames, interestNames, COURSE_STATUS, pageable);

        ArrayList<Course> filteredCourses = new ArrayList<>();

        for (Course course : results) {
            for (Audience audience : course.getAudiences()) {
                for (String organisation : organisationParentandChild) {
                    if (audience.getDepartments().contains(organisation)
                            && doesCourseAudienceMatchUserProfile(audience, grade, interestNames, otherAreasOfWorkNames, professionName)) {
                        filteredCourses.add(course);
                    }
                }
            }
        }

        Set<Course> set = new LinkedHashSet<>();
        set.addAll(filteredCourses);
        filteredCourses.clear();
        filteredCourses.addAll(set);
        results = new PageImpl<>(filteredCourses, pageable, filteredCourses.size());

        return ResponseEntity.ok(new PageResults<>(results, pageable));
    }

    private boolean doesCourseAudienceMatchUserProfile(Audience audience, String grade, List<String> interestNames, List<String> otherAreasOfWorkNames, String professionName) {
        return (isGradeValid(audience, grade)
                && isInterestsValid(audience, interestNames)
                && isAreaOfWorkValid(audience, otherAreasOfWorkNames, professionName)
                && isCourseLearningTypeValid(audience));
    }

    private boolean isGradeValid(Audience audience, String grade) {
        return (audience.getGrades().isEmpty() || audience.getGrades().contains(grade));
    }

    private boolean isInterestsValid(Audience audience, List<String> interestNames) {
        return (audience.getInterests().isEmpty() || containsAny(audience.getInterests(), interestNames));
    }

    private boolean isAreaOfWorkValid(Audience audience, List<String> otherAreasOfWorkNames, String professionName) {
        return (audience.getAreasOfWork().isEmpty() || ((containsAny(audience.getAreasOfWork(), otherAreasOfWorkNames)) || audience.getAreasOfWork().contains(professionName)));
    }

    private boolean isCourseLearningTypeValid(Audience audience) {
        return (audience.getType() != null && audience.getType().equals(Audience.Type.REQUIRED_LEARNING));
    }

    private List<String> listWithNone(List<String> list) {
        if (list.isEmpty()) {
            return Arrays.asList(ELASTIC_EMPTY_PARAM);
        }
        return list;
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
