package uk.gov.cslearning.catalogue.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;
import uk.gov.cslearning.catalogue.api.FilterParameters;
import uk.gov.cslearning.catalogue.domain.Resource;
import uk.gov.cslearning.catalogue.domain.SearchPage;

@Repository
public interface ResourceRepository extends ElasticsearchRepository<Resource, String>, ResourceSearchRepository {

    SearchPage search(String query, Pageable pageable, FilterParameters filterParameters);
}
