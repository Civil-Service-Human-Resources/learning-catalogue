package uk.gov.cslearning.catalogue.api;

import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.repository.ResourceRepository;

import java.util.Arrays;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(CourseController.class)
public class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CourseRepository courseRepository;

    @MockBean
    private ResourceRepository resourceRepository;

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldReturnNotFoundForUnknownCourse() throws Exception {
        mockMvc.perform(get("/courses/abc")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldReturnCourse() throws Exception {

        Course course = createCourse();

        when(courseRepository.findById("1"))
                .thenReturn(Optional.of(course));

        mockMvc.perform(get("/courses/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", equalTo("title")));
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldCreateCourseAndRedirectToNewResource() throws Exception {

        Gson gson = new Gson();

        final String newId = "newId";

        Course course = createCourse();

        when(courseRepository.save(any()))
                .thenAnswer((Answer<Course>) invocation -> {
                    course.setId(newId);
                    return course;
                });

        mockMvc.perform(post("/courses")
                        .with(csrf())
                        .content(gson.toJson(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "http://localhost/courses/" + newId));
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldUpdateExistingCourse() throws Exception {

        Gson gson = new Gson();

        Course course = createCourse();
        when(courseRepository.existsById(course.getId())).thenReturn(true);
        when(courseRepository.save(any())).thenReturn(course);

        mockMvc.perform(put("/courses/" + course.getId())
                        .with(csrf())
                        .content(gson.toJson(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldReturnBadRequestIfUpdatedCourseDoesntExist() throws Exception {

        Gson gson = new Gson();

        Course course = createCourse();
        when(courseRepository.existsById(course.getId())).thenReturn(false);

        mockMvc.perform(
                put("/courses/" + course.getId())
                        .with(csrf())
                        .content(gson.toJson(course))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldReturnListOfCoursesIfFoundWithSearchParams() throws Exception {
        Course course1 = new Course();
        course1.setTitle("Course 1");

        Course course2 = new Course();
        course2.setTitle("Course 2");

        Page<Course> page = new PageImpl<>(Arrays.asList(course1, course2));

        when(courseRepository.findSuggested("department1,department2",
                "area1,area2", PageRequest.of(2, 2)))
                .thenReturn(page);

        mockMvc.perform(get("/courses/")
                .param("areaOfWork", "area1", "area2")
                .param("department", "department1", "department2")
                .param("page", "2")
                .param("size", "2")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].title", equalTo("Course 1")))
                .andExpect(jsonPath("$.results[1].title", equalTo("Course 2")));
    }

    @Test
    @WithMockUser(username = "user", password = "password")
    public void shouldReturnListOfCoursesIfFoundWithoutSearchParams() throws Exception {
        Course course1 = new Course();
        course1.setTitle("Course 1");

        Course course2 = new Course();
        course2.setTitle("Course 2");

        Page<Course> page = new PageImpl<>(Arrays.asList(course1, course2));

        when(courseRepository.findAll(PageRequest.of(0, 10)))
                .thenReturn(page);

        mockMvc.perform(get("/courses/")
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.results[0].title", equalTo("Course 1")))
                .andExpect(jsonPath("$.results[1].title", equalTo("Course 2")));
    }

    private Course createCourse() {
        return new Course("title", "shortDescription", "description",
                "learningOutcomes");
    }
}
