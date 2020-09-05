package uk.gov.cslearning.catalogue.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cslearning.catalogue.api.FilterParameters;
import uk.gov.cslearning.catalogue.api.OwnerParameters;
import uk.gov.cslearning.catalogue.api.ProfileParameters;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.SearchPage;
import uk.gov.cslearning.catalogue.domain.Status;


import java.util.Collection;
import java.util.List;

@Repository
public interface CourseRepository extends ElasticsearchRepository<Course, String>, CourseSearchRepository, CourseSuggestionsRepository {

//    @Query("{\"bool\": {\"must\": [{\"match\": {\"audiences.type\": \"REQUIRED_LEARNING\"}},{\"match\": {\"status\": \"?1\"}},{\"match\": {\"audiences.departments\": \"?0\"}}]}}")
//    List<Course> findMandatory(String department, String status, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"match\": {\"audiences.type\": \"REQUIRED_LEARNING\"}},{\"match\": {\"status\": \"?1\"}},{\"match\": {\"audiences.departments\": \"[?0]\"}}]}}")
    List<Course> findMandatoryOfMultipleDepts(List<String> department, String status, Pageable pageable);

   // @Query("{\"bool\": {\"should\": [{\"match\": {\"audiences.departments\": {\"query\": \"?0\",\"zero_terms_query\": \"none\"}}},{\"match\": {\"audiences.areasOfWork\": {\"query\": \"?1\",\"zero_terms_query\": \"none\"}}},{\"match\": {\"audiences.interests\": {\"query\": \"?2\",\"zero_terms_query\": \"none\"}}}],\"must\": [{\"match\": {\"status\": {\"query\": \"?3\"}}}],\"must_not\": [{\"match\": {\"audiences.type\": \"REQUIRED_LEARNING\"}}]}}")
    //Page<Course> findSuggested(String department, String areaOfWork, String interest, String status, Pageable pageable);

    SearchPage search(String query, Pageable pageable, FilterParameters filterParameters, Collection<Status> status, OwnerParameters ownerParameters, ProfileParameters profileParameters, String visbility);

    Page<Course> findAllByStatusIn(Collection<Status> status, Pageable pageable);

    @Query("{\"bool\": {\"must\": [{\"exists\": {\"field\": \"modules.events\"}},{\"match\": {\"modules.type\": \"face-to-face\"}}]}}")
    List<Course> findEvents();

    Page<Course> findAllByOrganisationCode(String organisationalUnitCode, Pageable pageable);

    Page<Course> findAllByProfessionId(String professionId, Pageable pageable);

    Page<Course> findAllBySupplier(String supplier, Pageable pageable);

    List<Course> findAllByModulesExists();

    @Query("{\"bool\": {\"must\": [{\"match\": {\"audiences.type\": \"REQUIRED_LEARNING\"}},{\"match\": {\"status\": \"?0\"}}]}}")
    List<Course> findAllRequired(String status, Pageable pageable);
}
