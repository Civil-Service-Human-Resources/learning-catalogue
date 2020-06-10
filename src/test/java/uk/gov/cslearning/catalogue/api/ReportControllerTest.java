package uk.gov.cslearning.catalogue.api;

import com.google.common.collect.ImmutableMap;
import org.glassfish.jersey.servlet.WebConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cslearning.catalogue.config.RequestMappingConfig;
import uk.gov.cslearning.catalogue.dto.CourseDto;
import uk.gov.cslearning.catalogue.dto.EventDto;
import uk.gov.cslearning.catalogue.dto.ModuleDto;
import uk.gov.cslearning.catalogue.service.CourseService;
import uk.gov.cslearning.catalogue.service.EventService;
import uk.gov.cslearning.catalogue.service.ModuleService;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReportController.class)
@RunWith(SpringRunner.class)
@WithMockUser(username = "user")
@EnableSpringDataWebSupport
@ContextConfiguration(classes = {RequestMappingConfig.class, WebConfig.class, ReportController.class})
public class ReportControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EventService eventService;

    @MockBean
    private ModuleService moduleService;

    @MockBean
    private CourseService courseService;

    @Test
    @WithMockUser(username = "user")
    public void shouldReturnMapOfEvents() throws Exception {

        String courseId = "course-id";
        String courseTitle = "course-title";
        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        courseDto.setTitle(courseTitle);

        String moduleId = "module-id";
        String moduleTitle = "module-title";
        ModuleDto moduleDto = new ModuleDto();
        moduleDto.setId(moduleId);
        moduleDto.setTitle(moduleTitle);
        moduleDto.setCourse(courseDto);

        String eventId = "event_id";
        EventDto event = new EventDto();
        event.setId(eventId);
        event.setModule(moduleDto);

        Map<String, EventDto> events = ImmutableMap.of(eventId, event);

        when(eventService.getEventMap()).thenReturn(events);

        mockMvc.perform(
                get("/reporting/events")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.event_id.id", equalTo(eventId)))
                .andExpect(jsonPath("$.event_id.module.id", equalTo(moduleId)))
                .andExpect(jsonPath("$.event_id.module.title", equalTo(moduleTitle)))
                .andExpect(jsonPath("$.event_id.module.course.id", equalTo(courseId)))
                .andExpect(jsonPath("$.event_id.module.course.title", equalTo(courseTitle)));

    }

    @Test
    public void shouldReturnMapOfModules() throws Exception {
        String courseId = "course-id";
        String courseTitle = "course-title";
        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        courseDto.setTitle(courseTitle);

        String moduleId = "module_id";
        String moduleTitle = "module-title";
        ModuleDto moduleDto = new ModuleDto();
        moduleDto.setId(moduleId);
        moduleDto.setTitle(moduleTitle);
        moduleDto.setCourse(courseDto);

        Map<String, ModuleDto> modules = ImmutableMap.of(moduleId, moduleDto);

        when(moduleService.getModuleMap()).thenReturn(modules);

        mockMvc.perform(
                get("/reporting/modules")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.module_id.id", equalTo(moduleId)))
                .andExpect(jsonPath("$.module_id.title", equalTo(moduleTitle)))
                .andExpect(jsonPath("$.module_id.course.id", equalTo(courseId)))
                .andExpect(jsonPath("$.module_id.course.title", equalTo(courseTitle)));
    }

    @Test
    public void shouldReturnPublishedAndArchivedMandatoryCourses() throws Exception {

        String courseId = "course-id";
        String courseTitle = "course-title";
        String courseTopicId = "topic-id";

        CourseDto courseDto = new CourseDto();
        courseDto.setId(courseId);
        courseDto.setTitle(courseTitle);
        courseDto.setTopicId("topic-id");

        Map<String, CourseDto> courseDtoMap = ImmutableMap.of(courseId, courseDto);

        when(courseService.getPublishedAndArchivedMandatoryCourses()).thenReturn(courseDtoMap);

        mockMvc.perform(
                get("/reporting/mandatory-courses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.course-id.id", equalTo(courseId)))
                .andExpect(jsonPath("$.course-id.title", equalTo(courseTitle)))
                .andExpect(jsonPath("$.course-id.topicId", equalTo(courseTopicId)));
    }
}