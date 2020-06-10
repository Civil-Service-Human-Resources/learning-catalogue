package uk.gov.cslearning.catalogue.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.CivilServant.OrganisationalUnit;
import uk.gov.cslearning.catalogue.domain.CivilServant.Profession;
import uk.gov.cslearning.catalogue.domain.*;
import uk.gov.cslearning.catalogue.domain.Owner.Owner;
import uk.gov.cslearning.catalogue.domain.Owner.OwnerFactory;
import uk.gov.cslearning.catalogue.domain.module.*;
import uk.gov.cslearning.catalogue.dto.CourseDto;
import uk.gov.cslearning.catalogue.dto.factory.CourseDtoFactory;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.repository.CourseRepositoryImpl;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CourseServiceTest {

    private static final String COURSE_ID = "courseId";
    private static final String ORGANISATIONAL_UNIT_CODE = "code";
    private static final PageRequest PAGEABLE = PageRequest.of(0, 10);
    private static final Long PROFESSION_ID = 1L;
    private static final Scope SCOPE = Scope.GLOBAL;
    private static final String LEARNING_PROVIDER_ID = "UUID";

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseRepositoryImpl courseRepositoryImpl;

    @Mock
    private EventService eventService;

    @Mock
    private CourseDtoFactory courseDtoFactory;

    @Mock
    private RegistryService registryService;

    @Mock
    private AuthoritiesService authoritiesService;

    @Mock
    private Authentication authentication;

    @Mock
    private OwnerFactory ownerFactory;

    @Mock
    private RequiredByService requiredByService;

    @InjectMocks
    private CourseService courseService;

    @Test
    public void shouldSave() {
        Course course = new Course();

        when(courseRepository.save(course)).thenReturn(course);

        courseService.save(course);

        verify(courseRepository).save(course);
    }

    @Test
    public void shouldFindByIdShouldNotCallEventsAvail() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        verify(eventService, times(0)).getEventAvailability(any());
        assertEquals(courseService.findById(COURSE_ID), Optional.empty());
    }

    @Test
    public void shouldFindCourseAndGetEventAvailabilitiesAndEventStatus() {
        Course course = new Course();
        FaceToFaceModule module = new FaceToFaceModule("product code");
        Event event = new Event();

        Collection<Event> events = new ArrayList<>();
        events.add(event);
        module.setEvents(events);

        List<Module> modules = new ArrayList<>();
        modules.add(module);
        course.setModules(modules);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(eventService.getEventAvailability(event)).thenReturn(event);
        when(eventService.getStatus(event.getId())).thenReturn(EventStatus.ACTIVE);

        assertEquals(courseService.findById(COURSE_ID), Optional.of(course));

        verify(courseRepository).findById(COURSE_ID);
        verify(eventService).getEventAvailability(event);
        verify(eventService).getStatus(event.getId());
    }

    @Test
    public void shouldGetCourseById() {
        Course course = new Course();
        Optional<Course> optionalCourse = Optional.of(course);

        String courseId = "abc123";
        when(courseService.findById(courseId)).thenReturn(optionalCourse);

        assertEquals(courseService.getCourseById(courseId), course);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfCourseDoesntExist() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.empty());

        courseService.getCourseById(COURSE_ID);
    }

    @Test
    public void shouldFindCoursesByOrganisationalUnit() {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course());
        courses.add(new Course());

        Page<Course> coursesPage = new PageImpl<>(courses);

        when(courseRepository.findAllByOrganisationCode(ORGANISATIONAL_UNIT_CODE, PAGEABLE)).thenReturn(coursesPage);

        assertEquals(courseService.findCoursesByOrganisationalUnit(ORGANISATIONAL_UNIT_CODE, PAGEABLE), coursesPage);

    }

    @Test
    public void shouldFindCoursesByProfession() {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course());
        courses.add(new Course());

        Page<Course> coursesPage = new PageImpl<>(courses);

        when(courseRepository.findAllByProfessionId(PROFESSION_ID.toString(), PAGEABLE)).thenReturn(coursesPage);

        assertEquals(courseService.findCoursesByProfession(PROFESSION_ID.toString(), PAGEABLE), coursesPage);
    }

    @Test
    public void shouldFindAllCourses() {
        List<Course> courses = new ArrayList<>();
        courses.add(new Course());
        courses.add(new Course());

        Page<Course> coursesPage = new PageImpl<>(courses);

        when(courseRepository.findAll(PAGEABLE)).thenReturn(coursesPage);

        assertEquals(courseService.findAllCourses(PAGEABLE), coursesPage);
    }

    @Test
    public void shouldCreateCourse() {
        Course course = new Course();

        Owner owner = new Owner();
        owner.setOrganisationalUnit(ORGANISATIONAL_UNIT_CODE);
        owner.setProfession(PROFESSION_ID);

        CivilServant civilServant = new CivilServant();
        OrganisationalUnit organisationalUnit = new OrganisationalUnit();
        organisationalUnit.setCode(ORGANISATIONAL_UNIT_CODE);
        civilServant.setOrganisationalUnit(organisationalUnit);

        Profession profession = new Profession();
        profession.setId(PROFESSION_ID);
        civilServant.setProfession(profession);

        LearningProvider learningProvider = new LearningProvider();
        learningProvider.setId(LEARNING_PROVIDER_ID);
        civilServant.setLearningProvider(learningProvider);

        when(registryService.getCurrentCivilServant()).thenReturn(civilServant);
        when(authoritiesService.getScope(authentication)).thenReturn(SCOPE);
        when(ownerFactory.create(civilServant, course)).thenReturn(owner);
        when(courseRepository.save(course)).thenReturn(course);

        Course createdCourse = courseService.createCourse(course, authentication);
        Owner createdOwner = createdCourse.getOwner();

        verify(courseRepository).save(course);
        assertEquals(createdOwner.getOrganisationalUnit(), ORGANISATIONAL_UNIT_CODE);
        assertEquals(createdOwner.getProfession(), PROFESSION_ID);
    }

    @Test
    public void shouldUpdateCourse() throws MalformedURLException {
        Course course = new Course();

        course.setTitle("title1");
        course.setShortDescription("sd1");
        course.setDescription("sd1");
        course.setLearningOutcomes("lc1");

        List<Module> modules = Arrays.asList(new FaceToFaceModule("pc"));
        course.setModules(modules);

        LearningProvider learningProvider = new LearningProvider();
        course.setLearningProvider(learningProvider);

        Audience audience = new Audience();
        Set<Audience> audiences = new HashSet<>();
        audiences.add(audience);
        course.setAudiences(audiences);

        course.setPreparation("prep");

        Owner owner = new Owner();
        course.setOwner(owner);

        course.setVisibility(Visibility.PUBLIC);
        course.setStatus(Status.PUBLISHED);

        Course newCourse = new Course();

        String newTitle = "title2";
        newCourse.setTitle(newTitle);
        String newShortDescription = "sd2";
        newCourse.setShortDescription(newShortDescription);
        String newDescription = "sd2";
        newCourse.setDescription(newDescription);
        String newLearningOutcomes = "lc2";
        newCourse.setLearningOutcomes(newLearningOutcomes);

        List<Module> modules2 = Arrays.asList(new LinkModule(new URL("https://www.example.com")));
        newCourse.setModules(modules2);

        LearningProvider learningProvider2 = new LearningProvider();
        newCourse.setLearningProvider(learningProvider2);

        Audience audience2 = new Audience();
        Set<Audience> audiences2 = new HashSet<>();
        audiences.add(audience2);
        newCourse.setAudiences(audiences2);

        String prep2 = "prep2";
        newCourse.setPreparation(prep2);

        Owner owner2 = new Owner();
        newCourse.setOwner(owner2);

        Visibility visibility = Visibility.PRIVATE;
        newCourse.setVisibility(visibility);
        Status archived = Status.ARCHIVED;
        newCourse.setStatus(archived);

        Course savedCourse = courseService.updateCourse(course, newCourse);

        verify(courseRepository).save(any());

        assertEquals(savedCourse.getTitle(), newTitle);
        assertEquals(savedCourse.getShortDescription(), newShortDescription);
        assertEquals(savedCourse.getDescription(), newDescription);
        assertEquals(savedCourse.getLearningOutcomes(), newLearningOutcomes);
        assertEquals(savedCourse.getModules(), modules2);
        assertEquals(savedCourse.getLearningProvider(), learningProvider2);
        assertEquals(savedCourse.getAudiences(), audiences2);
        assertEquals(savedCourse.getPreparation(), prep2);
        assertEquals(savedCourse.getOwner(), owner2);
        assertEquals(savedCourse.getVisibility(), visibility);
        assertEquals(savedCourse.getStatus(), archived);
    }

    @Test
    public void shouldUpdateCourseWithNoLearningProvider() {
        Course course = new Course();
        course.setTitle("title");

        Course newCourse = new Course();
        newCourse.setTitle("newtitle");

        courseService.updateCourse(course, newCourse);

        assertEquals(course.getTitle(), "newtitle");
        assertEquals(course.getLearningProvider(), null);
    }

    @Test
    public void shouldReturnOrganisationalUnitParentsMap() {
        Map<String, List<String>> organisationalUnitParentsMap = new HashMap<>();

        when(registryService.getOrganisationalUnitParentsMap()).thenReturn(organisationalUnitParentsMap);

        assertEquals(organisationalUnitParentsMap, courseService.getOrganisationParentsMap());
    }

    @Test
    public void shouldReturnTrueIfAudienceRequiredWithinDays() {
        String co = "co";
        String hmrc = "hmrc";
        Set<String> departments1 = new HashSet<>(Arrays.asList(co, hmrc));

        String moj = "moj";
        String defra = "defra";
        Set<String> departments2 = new HashSet<>(Arrays.asList(moj, defra));

        Audience audience1 = new Audience();
        audience1.setDepartments(departments1);

        Audience audience2 = new Audience();
        audience2.setDepartments(departments2);

        Set<Audience> audiences = new HashSet<>(Arrays.asList(audience1, audience2));

        Course course = new Course();
        course.setAudiences(audiences);

        List<String> codeList = Arrays.asList(co, hmrc);

        when(requiredByService.isAudienceRequiredWithinRange(any(Audience.class), any(Instant.class), any(long.class), any(long.class))).thenReturn(true);

        assertTrue(courseService.isCourseRequiredWithinRangeForOrg(course, codeList, 1L, 7L));
    }

    @Test
    public void shouldReturnFalseIfAudienceNotRequiredWithinDays() {
        String co = "co";
        String hmrc = "hmrc";
        Set<String> departments1 = new HashSet<>(Arrays.asList(co, hmrc));

        String moj = "moj";
        String defra = "defra";
        Set<String> departments2 = new HashSet<>(Arrays.asList(moj, defra));

        Audience audience1 = new Audience();
        audience1.setDepartments(departments1);

        Audience audience2 = new Audience();
        audience2.setDepartments(departments2);

        Set<Audience> audiences = new HashSet<>(Arrays.asList(audience1, audience2));

        Course course = new Course();
        course.setAudiences(audiences);

        List<String> codeList = Arrays.asList(co, hmrc);

        when(requiredByService.isAudienceRequiredWithinRange(any(Audience.class), any(Instant.class), any(long.class), any(long.class))).thenReturn(false);

        assertFalse(courseService.isCourseRequiredWithinRangeForOrg(course, codeList, 1L, 7L));
    }

    @Test
    public void shouldGetPublishedAndArchivedMandatoryCourses() {
        List<Course> courses = new ArrayList<>();
        Course course  = new Course();
        course.setId("courseId");
        course.setTitle("courseTitle");
        courses.add(course);
        List<Course> coursePage = new ArrayList<>(courses);

        CourseDto dto = new CourseDto();
        dto.setId("courseDtoId");
        dto.setTitle("courseDtoTitle"); ;

        when(courseRepositoryImpl.findPublishedAndArchivedMandatoryCourses()).thenReturn(coursePage);
        when(courseDtoFactory.create(courses.get(0))).thenReturn(dto);
        Map<String, CourseDto> courseDtoMap = courseService.getPublishedAndArchivedMandatoryCourses();

        assertEquals(dto.getTitle(), courseDtoMap.get("courseId").getTitle());
    }
}
