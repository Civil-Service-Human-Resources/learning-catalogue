package uk.gov.cslearning.catalogue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cslearning.catalogue.domain.Resource;
import uk.gov.cslearning.catalogue.repository.ResourceRepository;

@RestController
@RequestMapping("/resources")
public class ResourceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private ResourceRepository resourceRepository;

    @Autowired
    public ResourceController(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    @GetMapping
    public Iterable<Resource> findAllResources(){
        Iterable<Resource> resources = resourceRepository.findAll();

        return resources;
    }
}
