package uk.gov.cslearning.catalogue.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cslearning.catalogue.domain.Course;

@Repository
public interface CourseRepository extends ElasticsearchRepository<Course, String>, CourseSearchRepository {

    @Query("{ \"bool\": { \"must\": [{ \"match\": { \"modules.audiences.mandatory\": \"true\" } }, { \"term\": { \"modules.audiences.departments\": \"?0\" }}] }}")
    Page<Course> findMandatory(String department, Pageable pageable);

    @Query("{ \"bool\": { " +
            "\"should\": [{ \"match\": { \"modules.audiences.departments\": \"?0\" }}, { \"match\": { \"modules.audiences.areasOfWork\": \"?1\" }}], " +
            "\"must_not\": [ { \"match\": { \"modules.audiences.mandatory\": \"true\" } }]}}")
    Page<Course> findSuggested(String department, String areaOfWork, Pageable pageable);
}