package uk.gov.cslearning.catalogue.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.cslearning.catalogue.api.FilterParameters;
import uk.gov.cslearning.catalogue.api.PageParameters;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.SearchPage;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * UserRepository integration test.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class CourseSearchRepositoryIT {

    private PageRequest all = PageRequest.of(0, 1000);

    @Autowired
    private CourseRepository repository;

  /*  @Test
    public void shouldReturnAccurateSuggestionAndCourseWithMisspelledSearchQuery() {
        PageParameters pageParameters = new PageParameters();
        FilterParameters filterParameters = new FilterParameters();
        Pageable pageable = pageParameters.getPageRequest();

        SearchPage actualSearchPage = repository.search("Wirking with Budgets", pageable,filterParameters);

        String actualSuggestionText = actualSearchPage.getTopScoringSuggestion().getText().toString();
        Page<Course> coursePage = actualSearchPage.getCourses();
        List<Course> courseList = coursePage.getContent();
        String actualTitle = courseList.get(0).getTitle();

        assertThat(actualSuggestionText, is("working with budgets"));
        assertThat(actualTitle, is("Working with budgets"));
    }

    @Test
    public void shouldReturnCorrectPageForSearchQuery() {
        PageParameters pageParameters = new PageParameters();
        Pageable pageable = pageParameters.getPageRequest();
        FilterParameters filterParameters = new FilterParameters();

        SearchPage actualSearchPage = repository.search("Budgets", pageable,filterParameters);
        List<Course> actualCourses = actualSearchPage.getCourses().getContent();

        assertThat(actualCourses.size(), is(4));
        assertThat(actualCourses.get(0).getTitle(), is("Working with budgets"));
        assertThat(actualCourses.get(0).getId(), is("BUfZwRaWQrKAhSSjlJ7lCg"));
        assertThat(actualCourses.get(0).getShortDescription(), is("This topic introduces you to the fundamental principles of budget management and governance processes. "));
    }*/
/*
    @Test
    public void shouldReturnCorrectPageForSearchQueryWithMissingField() {
        PageParameters pageParameters = new PageParameters();
        Pageable pageable = pageParameters.getPageRequest();
        FilterParameters filterParameters = new FilterParameters();

        SearchPage actualSearchPage = repository.search("Spotify engineering culture: part 1", pageable, filterParameters );
        List<Course> actualCourses = actualSearchPage.getCourses().getContent();

        assertThat(actualCourses.get(0).getTitle(), is("Spotify engineering culture: part 1"));
        assertThat(actualCourses.get(0).getLearningOutcomes(), is(""));
    }

    @Test
    public void shouldReturnFilteredResultsCorrectlyForType() {
        PageParameters pageParameters = new PageParameters();
        Pageable pageable = pageParameters.getPageRequest();
        FilterParameters filterParameters = new FilterParameters();
        filterParameters.setType("face to face");

        SearchPage actualSearchPage = repository.search("why", pageable, filterParameters );
        List<Course> actualCourses = actualSearchPage.getCourses().getContent();

        assertThat(actualCourses.get(0).getTitle(), is("Understanding and using business cases"));
    }*/
}
