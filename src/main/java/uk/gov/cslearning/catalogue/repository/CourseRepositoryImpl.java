package uk.gov.cslearning.catalogue.repository;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Repository;
import uk.gov.cslearning.catalogue.domain.Course;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.search.sort.SortBuilders.fieldSort;
import static org.elasticsearch.search.sort.SortOrder.ASC;
import static uk.gov.cslearning.catalogue.domain.CourseType.REQUIRED_LEARNING;
import static uk.gov.cslearning.catalogue.domain.Status.ARCHIVED;
import static uk.gov.cslearning.catalogue.domain.Status.PUBLISHED;

@Repository
public class CourseRepositoryImpl {
    private static final int PAGE_SIZE = 10000;
    private final ElasticsearchOperations operations;

    public CourseRepositoryImpl(ElasticsearchOperations operations) {
        checkArgument(operations != null);
        this.operations = operations;
    }

    public List<Course> findPublishedAndArchivedMandatoryCourses() {
        BoolQueryBuilder boolQuery = boolQuery();
        boolQuery.should(QueryBuilders.matchQuery("status", PUBLISHED));
        boolQuery.should(QueryBuilders.matchQuery("status", ARCHIVED));
        BoolQueryBuilder filterQuery = boolQuery();
        filterQuery.must(QueryBuilders.matchQuery("audiences.type", REQUIRED_LEARNING));

        int page = 0;
        int numberOfCourses;
        List<Course> allRequiredCourses = new ArrayList<>();
        do {
            PageRequest pageRequest = PageRequest.of(page, PAGE_SIZE);
            SearchQuery searchQuery = new NativeSearchQueryBuilder()
                    .withQuery(boolQuery)
                    .withFilter(filterQuery)
                    .withPageable(pageRequest)
                    .withSort(fieldSort("_id").order(ASC))
                    .build();
            List<Course> courses = operations.queryForList(searchQuery, Course.class);
            numberOfCourses = courses.size();
            page = page + 1;
            allRequiredCourses.addAll(courses);
        } while(numberOfCourses == PAGE_SIZE);

        return allRequiredCourses;
    }
}