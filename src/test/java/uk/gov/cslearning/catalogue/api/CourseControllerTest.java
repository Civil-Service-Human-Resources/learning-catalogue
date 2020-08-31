package uk.gov.cslearning.catalogue.api;

import static uk.gov.cslearning.catalogue.exception.ResourceNotFoundException.resourceNotFoundException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import uk.gov.cslearning.catalogue.config.RequestMappingConfig;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.CivilServant.OrganisationalUnit;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.Status;
import uk.gov.cslearning.catalogue.domain.Visibility;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.domain.module.DateRange;
import uk.gov.cslearning.catalogue.domain.module.Event;
import uk.gov.cslearning.catalogue.domain.module.FaceToFaceModule;
import uk.gov.cslearning.catalogue.domain.module.LinkModule;
import uk.gov.cslearning.catalogue.domain.module.Module;
import uk.gov.cslearning.catalogue.domain.module.Venue;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.service.CourseService;
import uk.gov.cslearning.catalogue.service.EventService;
import uk.gov.cslearning.catalogue.service.ModuleService;
import uk.gov.cslearning.catalogue.service.RegistryService;
import uk.gov.cslearning.catalogue.service.upload.AudienceService;

import org.glassfish.jersey.servlet.WebConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.ImmutableMap;


@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(CourseController.class)
@WithMockUser(username = "user")
@ContextConfiguration(classes = {RequestMappingConfig.class, WebConfig.class, CourseController.class})
@EnableSpringDataWebSupport
public class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseRepository courseRepository;

    @MockBean
    private CourseService courseService;

    @MockBean
    private ModuleService moduleService;

    @MockBean
    private EventService eventService;

    @MockBean
    private AudienceService audienceService;

    @MockBean
    private RegistryService registryService;

    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Test
    public void shouldCreateCourseAndRedirectToNewResource() throws Exception {
        final String newId = "newId";

        Course course = createCourse();

        when(courseService.createCourse(any(), any()))
                .thenAnswer((Answer<Course>) invocation -> {
                    course.setId(newId);
                    return course;
                });

        mockMvc.perform(
                post("/courses").with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "http://localhost/courses/" + newId));
    }

    @Test
    public void shouldFindSuggestedCourses() throws Exception {
        String areaOfWork = "area-of-work";
        String department = "department";
        String interest = "_interest";
        String status = "status";
        String grade = "G6";

        Set<String> grades = new HashSet();
        grades.add(grade);

        Set<String> organisationalUnits = new HashSet<>();
        organisationalUnits.add(department);

        Audience audience = new Audience();
        audience.setGrades(grades);
        audience.setDepartments(organisationalUnits);

        Set<Audience> audiences = new HashSet<>();
        audiences.add(audience);

        Course course = new Course();
        course.setAudiences(audiences);

        List<String> organisationParents = new ArrayList<>();
        organisationParents.add("department");

        when(courseService.getOrganisationParents(any(String.class))).thenReturn(organisationParents);

        when(courseRepository.findSuggested(any(List.class), eq(areaOfWork), eq(interest), eq(status), eq(grade), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/")
                        .param("areaOfWork", areaOfWork)
                        .param("department", department)
                        .param("interest", interest)
                        .param("status", status)
                        .param("grade", grade)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldDefaultToShowingAllPublicCourses() throws Exception {
        Course course = new Course();

        when(courseRepository.findAllByStatusIn(eq(Collections.singletonList(Status.PUBLISHED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldFindMultipleStatuses() throws Exception {
        Course course = new Course();

        when(courseRepository.findAllByStatusIn(eq(Arrays.asList(Status.DRAFT, Status.PUBLISHED, Status.ARCHIVED)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/")
                        .param("status", "Draft", "Published", "Archived")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldDefaultMissingInterestParameterToNone() throws Exception {
        String areaOfWork = "area-of-work";
        String department = "NONE";
        String interest = "NONE";
        String status = "Published";
        String grade = "G6";

        Set<String> grades = new HashSet();
        grades.add(grade);

        Set<String> organisationalUnits = new HashSet<>();
        organisationalUnits.add(areaOfWork);

        Audience audience = new Audience();
        audience.setGrades(grades);
        audience.setAreasOfWork(organisationalUnits);

        Set<Audience> audiences = new HashSet<>();
        audiences.add(audience);

        Course course = new Course();
        course.setAudiences(audiences);

        when(courseRepository.findSuggested(any(List.class), eq(areaOfWork), eq(interest), eq(status), eq(grade), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/")
                        .param("areaOfWork", areaOfWork)
                        .param("grade", grade)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldConcatenateMultipleParameters() throws Exception {
        String areaOfWork = "area-of-work1,area-of-work2";
        String department = "department1,department2";
        String interest = "interest1,interest2";
        String status = "Published";
        String grade = "G6";

        Set<String> grades = new HashSet();
        grades.add(grade);

        Set<String> organisationalUnits = new HashSet<>();
        organisationalUnits.add("department1");

        Audience audience = new Audience();
        audience.setGrades(grades);
        audience.setDepartments(organisationalUnits);

        Set<Audience> audiences = new HashSet<>();
        audiences.add(audience);

        Course course = new Course();
        course.setAudiences(audiences);

        List<String> organisationParents = new ArrayList<>();
        organisationParents.add("department1");
        organisationParents.add("department2");

        when(courseService.getOrganisationParents(any(String.class))).thenReturn(organisationParents);

        when(courseRepository.findSuggested(any(List.class), eq(areaOfWork), eq(interest), eq(status), eq(grade), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/")
                        .param("areaOfWork", "area-of-work1", "area-of-work2")
                        .param("department", "department1", "department2")
                        .param("interest", "interest1", "interest2")
                        .param("grade", grade)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldListMandatoryCourses() throws Exception {
        String department = "department1";

        Course course = new Course();

        when(courseService.fetchMandatoryCourses(any(String.class), any(String.class), any(Pageable.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(course)));
        when(courseService.getOrganisationParents(eq(department))).thenReturn(new ArrayList<>(Collections.singletonList(department)));
        mockMvc.perform(
                get("/courses/")
                        .param("department", "department1")
                        .param("mandatory", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));
    }

    @Test
    public void shouldGetRequiredLearningByOrgCodeMap() throws Exception {
        Map<String, List<String>> organisationalUnitsParentMap = new HashMap<>();
        List<String> departmentsList = Arrays.asList("dept1", "dept2");
        List<Course> courseList = Arrays.asList(new Course(), new Course());

        when(courseService.getOrganisationParentsMap()).thenReturn(organisationalUnitsParentMap);
        when(courseRepository.findMandatoryOfMultipleDepts(departmentsList, "Published", PageRequest.of(0, 10000))).thenReturn(courseList);
        mockMvc.perform(
                get("/courses/required")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldGetRequiredLearningByOrgCodeMapWithParams() throws Exception {
        Map<String, List<String>> organisationalUnitsParentMap = new HashMap<>();
        List<String> departmentsList = Arrays.asList("dept1", "dept2");
        List<Course> courseList = Arrays.asList(new Course(), new Course());

        when(courseService.getOrganisationParentsMap()).thenReturn(organisationalUnitsParentMap);
        when(courseRepository.findMandatoryOfMultipleDepts(departmentsList, "Published", PageRequest.of(0, 10000))).thenReturn(courseList);
        mockMvc.perform(
                get("/courses/required")
                        .param("days", "7")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldListMandatoryCoursesWithMultipleParameters() throws Exception {
        String department = "department1,department2";

        Course course = new Course();

        when(courseService.fetchMandatoryCourses(any(String.class), any(String.class), any(Pageable.class)))
                .thenReturn(new ArrayList<>(Collections.singletonList(course)));
        when(courseService.getOrganisationParents(eq(department))).thenReturn(new ArrayList<>(Collections.singletonList(department)));

        mockMvc.perform(
                get("/courses/")
                        .param("department", "department1", "department2")
                        .param("status", "Draft", "Published")
                        .param("mandatory", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].id", equalTo(course.getId())));

    }

    @Test
    @WithMockUser(username = "user", authorities = {"ORGANISATION_AUTHOR"})
    public void shouldListForOrganisation() throws Exception {
        Course course = new Course();

        CivilServant civilServant = new CivilServant();
        OrganisationalUnit organisationalUnit = new OrganisationalUnit();
        String code = "code";
        organisationalUnit.setCode(code);
        civilServant.setOrganisationalUnit(organisationalUnit);

        when(registryService.getCurrentCivilServant())
                .thenReturn(civilServant);
        when(courseService.findCoursesByOrganisationalUnit(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/management")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"PROFESSION_AUTHOR"})
    public void shouldListForProfession() throws Exception {
        Course course = new Course();

        CivilServant civilServant = new CivilServant();
        OrganisationalUnit organisationalUnit = new OrganisationalUnit();
        String code = "code";
        organisationalUnit.setCode(code);
        civilServant.setOrganisationalUnit(organisationalUnit);

        when(registryService.getCurrentCivilServant())
                .thenReturn(civilServant);
        when(courseService.findCoursesByProfession(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/management")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"KPMG_SUPPLIER_AUTHOR"})
    public void shouldListForSupplier() throws Exception {
        Course course = new Course();

        CivilServant civilServant = new CivilServant();
        OrganisationalUnit organisationalUnit = new OrganisationalUnit();
        String code = "code";
        organisationalUnit.setCode(code);
        civilServant.setOrganisationalUnit(organisationalUnit);

        when(registryService.getCurrentCivilServant())
                .thenReturn(civilServant);
        when(courseService.findCoursesBySupplier(any(), any())).thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/management")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"CSL_AUTHOR"})
    public void shouldListForCslAuthor() throws Exception {
        Course course = new Course();

        when(courseService.findAllCourses(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.singletonList(course)));

        mockMvc.perform(
                get("/courses/management")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"INVALID_ROLE"})
    public void shouldReturnForbiddenForCslAuthor() throws Exception {
        mockMvc.perform(
                get("/courses/management")
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    public void shouldReturnNotFoundForUnknownCourse() throws Exception {
        when(courseService.findById("1"))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                get("/courses/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }


    @Test
    public void shouldReturnCourse() throws Exception {

        Course course = createCourse();

        when(courseService.findById("1"))
                .thenReturn(Optional.of(course));

        mockMvc.perform(
                get("/courses/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("title")));
    }

    @Test
    public void shouldReturnCoursesByIds() throws Exception {
        List<String> courses = Arrays.asList("1");
        Iterable<Course> result = new ArrayList<>();

        when(courseRepository.findAllById(courses))
                .thenReturn(result);

        mockMvc.perform(
                post("/courses/getIds").with(csrf())
                        .content(objectMapper.writeValueAsString(courses))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    @Test
    public void shouldUpdateExistingCourse() throws Exception {
        Course course = createCourse();
        when(courseService.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseService.updateCourse(any(), any())).thenReturn(course);

        mockMvc.perform(
                put("/courses/" + course.getId()).with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnBadRequestIfUpdatedCourseDoesntExist() throws Exception {
        Course course = createCourse();

        when(courseService.findById(course.getId())).thenReturn(Optional.empty());

        mockMvc.perform(
                put("/courses/" + course.getId()).with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldPublishExistingCourse() throws Exception {
        Course course = createCourse();
        when(courseService.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseService.updateCourse(any(), any())).thenReturn(course);

        mockMvc.perform(
                put("/courses/" + course.getId() + "/publish").with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnBadRequestIfPublishedCourseDoesntExist() throws Exception {
        Course course = createCourse();

        when(courseService.findById(course.getId())).thenReturn(Optional.empty());

        mockMvc.perform(
                put("/courses/" + course.getId() + "/publish").with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldArchivedExistingCourse() throws Exception {
        Course course = createCourse();
        when(courseService.findById(course.getId())).thenReturn(Optional.of(course));
        when(courseService.updateCourse(any(), any())).thenReturn(course);

        mockMvc.perform(
                put("/courses/" + course.getId() + "/archive").with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldReturnBadRequestIfArchivedCourseDoesntExist() throws Exception {
        Course course = createCourse();

        when(courseService.findById(course.getId())).thenReturn(Optional.empty());

        mockMvc.perform(
                put("/courses/" + course.getId() + "/archive").with(csrf())
                        .content(objectMapper.writeValueAsString(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldCreateModule() throws Exception {
        String moduleId = "module-id";
        Module module = mock(LinkModule.class);
        when(module.getId()).thenReturn(moduleId);

        String courseId = UUID.randomUUID().toString();
        String json = objectMapper.writeValueAsString(ImmutableMap.of("type", "link", "location", "http://localhost"));

        when(moduleService.save(eq(courseId), any(Module.class))).thenReturn(module);

        mockMvc.perform(
                post(String.format("/courses/%s/modules/", courseId)).with(csrf())
                        .content(json)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", String.format("http://localhost/courses/%s/modules/%s", courseId, moduleId)));
    }

    @Test
    public void shouldFindModule() throws Exception {
        String courseId = "course-id";
        String moduleId = "module-id";
        String url = "http://example.org";

        Module module = new LinkModule(new URL(url));

        when(moduleService.find(courseId, moduleId)).thenReturn(Optional.of(module));

        mockMvc.perform(
                get(String.format("/courses/%s/modules/%s", courseId, moduleId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url", equalTo(url)));
    }

    @Test
    public void shouldReturnNotFoundIfModuleNotFound() throws Exception {
        String courseId = "course-id";
        String moduleId = "module-id";

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(moduleService.find(courseId, moduleId)).thenReturn(Optional.empty());

        mockMvc.perform(
                get(String.format("/courses/%s/modules/%s", courseId, moduleId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldDeleteModule() throws Exception {
        String courseId = "course-id";
        String moduleId = "module-id";

        doNothing().when(moduleService).deleteModule(courseId, moduleId);

        mockMvc.perform(
                delete(String.format("/courses/%s/modules/%s", courseId, moduleId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    public void shouldUpdateModule() throws Exception {
        String courseId = "course-id";
        String moduleId = "id-123";
        String title = "old-title";
        String updatedTitle = "updated-title";
        String url = "https://www.example.org";

        Course course = new Course();
        Course updatedCourse = new Course();

        Module module = new LinkModule(new URL(url));
        module.setId(moduleId);
        module.setTitle(title);

        Module updatedModule = new LinkModule(new URL(url));
        updatedModule.setId(moduleId);
        updatedModule.setTitle(updatedTitle);

        List<Module> modules = new ArrayList<>();
        modules.add(module);
        course.setModules(modules);

        List<Module> updatedModules = new ArrayList<>();
        updatedModules.add(updatedModule);
        updatedCourse.setModules(updatedModules);

        when(moduleService.find(any(), any())).thenReturn(Optional.of(module));
        when(moduleService.updateModule(courseId, updatedModule)).thenReturn(updatedCourse);

        mockMvc.perform(
                put(String.format("/courses/%s/modules/%s", courseId, module.getId())).with(csrf())
                        .content(objectMapper.writeValueAsString(updatedModule))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());

        assertThat(updatedCourse.getModules().isEmpty(), is(false));
        assertThat(updatedCourse.getModules().size(), is(1));
        assertThat(updatedCourse.getModules().get(0).getTitle(), is(updatedTitle));
    }


    @Test
    public void shouldAddEventToModule() throws Exception {
        Event event = new Event();
        event.setJoiningInstructions("");
        event.setDateRanges(new ArrayList<>());
        event.setVenue(new Venue("location"));

        String courseId = "course-id";
        String moduleId = "module-id";

        when(courseRepository.existsById(courseId)).thenReturn(true);

        when(eventService.save(eq(courseId), eq(moduleId), any(Event.class))).thenReturn(event);

        mockMvc.perform(
                post(String.format("/courses/%s/modules/%s/events", courseId, moduleId)).with(csrf())
                        .content(objectMapper.writeValueAsString(event))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());
    }

    @Test
    public void shouldReturnEvent() throws Exception {
        String courseId = "course-id";
        String moduleId = "module-id";
        String eventId = "event-id";

        Event event = new Event();

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(eventService.find(courseId, moduleId, eventId)).thenReturn(Optional.of(event));

        mockMvc.perform(
                get(String.format("/courses/%s/modules/%s/events/%s", courseId, moduleId, eventId)).with(csrf())
                        .content(objectMapper.writeValueAsString(event))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    public void shouldReturnNotFoundIfEventNotFound() throws Exception {
        String courseId = "course-id";
        String moduleId = "module-id";
        String eventId = "event-id";

        when(eventService.find(courseId, moduleId, eventId)).thenReturn(Optional.empty());

        mockMvc.perform(
                get(String.format("/courses/%s/modules/%s/events/%s", courseId, moduleId, eventId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldUpdateEvent() throws Exception {

        LocalDate date = LocalDate.now();
        LocalTime start = LocalTime.NOON;
        LocalTime end = LocalTime.MIDNIGHT;

        DateRange dateRange = new DateRange();
        dateRange.setDate(date);
        dateRange.setStartTime(start);
        dateRange.setEndTime(end);

        List<DateRange> dateRanges = Collections.singletonList(dateRange);
        Venue venue = new Venue();
        venue.setLocation("venue-location");
        venue.setAddress("venue-address");
        venue.setCapacity(10);
        venue.setMinCapacity(5);

        Course course = new Course();

        Event oldEvent = new Event();
        Event newEvent = new Event();

        newEvent.setJoiningInstructions("new");
        newEvent.setDateRanges(dateRanges);
        newEvent.setVenue(venue);
        oldEvent.setJoiningInstructions("old");

        FaceToFaceModule module = new FaceToFaceModule("product-code");

        HashSet<Event> events = new HashSet<>();
        events.add(oldEvent);
        module.setEvents(events);

        List<Module> modules = new ArrayList<>();
        modules.add(module);
        course.setModules(modules);

        when(courseRepository.existsById(course.getId())).thenReturn(true);

        Optional<Course> result = Optional.of(course);
        when(courseRepository.findById(course.getId())).thenReturn(result);

        when(courseRepository.save(course)).thenReturn(course);

        mockMvc.perform(
                put(String.format("/courses/%s/modules/%s/events/%s", course.getId(), module.getId(), oldEvent.getId())).with(csrf())
                        .content(objectMapper.writeValueAsString(newEvent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        Event savedEvent = module.getEvents().stream().filter(e -> e.getId().equals(oldEvent.getId())).findFirst().get();

        assert (module.getEvents().size() == 1);
        assertEquals(savedEvent.getId(), oldEvent.getId());
        assertEquals("new", savedEvent.getJoiningInstructions());
        assertEquals(venue, savedEvent.getVenue());
        assertEquals(dateRange, savedEvent.getDateRanges().get(0));
    }

    @Test
    public void shouldDeleteEvent() throws Exception {
        Course course = new Course();
        FaceToFaceModule module = new FaceToFaceModule("product-code");
        Event event = new Event();

        HashSet<Event> events = new HashSet<>();
        events.add(event);
        module.setEvents(events);

        List<Module> modules = new ArrayList<>();
        modules.add(module);
        course.setModules(modules);

        when(courseRepository.existsById(course.getId())).thenReturn(true);

        Optional<Course> result = Optional.of(course);
        when(courseRepository.findById(course.getId())).thenReturn(result);

        when(courseRepository.save(course)).thenReturn(course);

        mockMvc.perform(
                delete(String.format("/courses/%s/modules/%s/events/%s", course.getId(), module.getId(), event.getId())).with(csrf()))
                .andExpect(status().isNoContent());

        assert (module.getEvents().isEmpty());
    }

    @Test
    public void shouldCreateAudience() throws Exception {
        String audienceId = "audience-id";
        Audience audience = mock(Audience.class);
        when(audience.getId()).thenReturn(audienceId);

        String courseId = UUID.randomUUID().toString();

        when(audienceService.isPermitted(any(), any())).thenReturn(true);
        when(audienceService.setDefaults(any(), any())).thenReturn(audience);
        when(audienceService.save(any(), any())).thenReturn(new Course());

        mockMvc.perform(
                post(String.format("/courses/%s/audiences/", courseId)).with(csrf())
                        .content(objectMapper.writeValueAsString(ImmutableMap.of("id", audienceId, "name", "Audience name")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", String.format("http://localhost/courses/%s/audiences/%s", courseId, audienceId)));
    }

    @Test
    public void shouldFindAudience() throws Exception {
        String courseId = "course-id";
        String audienceId = "audience-id";

        Audience audience = new Audience();
        audience.setId(audienceId);

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(audienceService.find(courseId, audienceId)).thenReturn(Optional.of(audience));

        mockMvc.perform(
                get(String.format("/courses/%s/audiences/%s", courseId, audienceId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(audienceId)));
    }

    @Test
    public void shouldReturnNotFoundIfAudienceNotFound() throws Exception {
        String courseId = "course-id";
        String audienceId = "audience-id";

        when(courseRepository.existsById(courseId)).thenReturn(true);
        when(audienceService.find(courseId, audienceId)).thenReturn(Optional.empty());

        mockMvc.perform(
                get(String.format("/courses/%s/audiences/%s", courseId, audienceId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldReturnNotFoundIfCourseNotFoundWhenFindingAudience() throws Exception {
        String courseId = "course-id";
        String audienceId = "audience-id";

        when(audienceService.isPermitted(any(), any())).thenReturn(true);
        Audience audience = new Audience();
        when(audienceService.setDefaults(any(), any())).thenReturn(audience);

        doThrow(resourceNotFoundException()).when(audienceService).save(courseId, audience);

        mockMvc.perform(
                get(String.format("/courses/%s/audiences/%s", courseId, audienceId)).with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    public void shouldDeleteAudience() throws Exception {
        Course course = new Course();
        Audience audience = new Audience();
        Set<Audience> audiences = new HashSet<>();
        audiences.add(audience);
        course.setAudiences(audiences);

        when(courseRepository.findById(course.getId())).thenReturn(Optional.of(course));
        when(audienceService.find(course, audience.getId())).thenReturn(Optional.of(audience));
        when(audienceService.isPermitted(any(), any())).thenReturn(true);
        when(courseRepository.save(course)).thenReturn(course);

        assertThat(course.getAudiences().isEmpty(), is(not(true)));

        mockMvc.perform(
                delete(String.format("/courses/%s/audiences/%s", course.getId(), audience.getId())).with(csrf()))
                .andExpect(status().isNoContent());

        assertThat(course.getAudiences().isEmpty(), is(true));
    }

    private Course createCourse() {
        return new Course("title", "shortDescription", "description",
                Visibility.PUBLIC);
    }
}
