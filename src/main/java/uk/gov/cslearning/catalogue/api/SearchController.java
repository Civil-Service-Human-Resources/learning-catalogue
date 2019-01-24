package uk.gov.cslearning.catalogue.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.SearchPage;
import uk.gov.cslearning.catalogue.domain.Status;
import uk.gov.cslearning.catalogue.mapping.RoleMapping;
import uk.gov.cslearning.catalogue.repository.CourseRepository;
import uk.gov.cslearning.catalogue.service.RegistryService;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/search")
public class SearchController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SearchController.class);

    private CourseRepository courseRepository;

    private RegistryService registryService;

    @Autowired
    public SearchController(CourseRepository courseRepository, RegistryService registryService) {
        this.courseRepository = courseRepository;
        this.registryService = registryService;
    }

    @RoleMapping("ORGANISATION_AUTHOR")
    @GetMapping("/courses")
    public ResponseEntity<SearchResults> searchForOrganisation(@RequestParam(name = "status", defaultValue = "Published") String status, String query, FilterParameters filterParameters, PageParameters pageParameters) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        OwnerParameters ownerParameters = new OwnerParameters();

        civilServant.getOrganisationalUnitCode().ifPresent(ownerParameters::setOrganisationalUnitCode);

        Pageable pageable = pageParameters.getPageRequest();
        SearchPage searchPage = courseRepository.search(query, pageable, filterParameters, Arrays.stream(status.split(",")).map(Status::forValue).collect(Collectors.toList()), ownerParameters);

        return ResponseEntity.ok(new SearchResults(searchPage, pageable));
    }

    @RoleMapping("PROFESSION_AUTHOR")
    @GetMapping("/courses")
    public ResponseEntity<SearchResults> searchForProfession(@RequestParam(name = "status", defaultValue = "Published") String status, String query, FilterParameters filterParameters, PageParameters pageParameters) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        OwnerParameters ownerParameters = new OwnerParameters();

        civilServant.getProfessionId().ifPresent(organisationalUnitCode -> ownerParameters.setProfession(organisationalUnitCode.toString()));

        Pageable pageable = pageParameters.getPageRequest();
        SearchPage searchPage = courseRepository.search(query, pageable, filterParameters, Arrays.stream(status.split(",")).map(Status::forValue).collect(Collectors.toList()), ownerParameters);

        return ResponseEntity.ok(new SearchResults(searchPage, pageable));
    }

    @RoleMapping({"CSL_AUTHOR", "LEARNING_MANAGER", "LEARNER"})
    @GetMapping("/courses")
    public ResponseEntity<SearchResults> searchForCslAuthorOrLearningManager(@RequestParam(name = "status", defaultValue = "Published") String status, String query, FilterParameters filterParameters, PageParameters pageParameters) {
        Pageable pageable = pageParameters.getPageRequest();

        OwnerParameters ownerParameters = new OwnerParameters();

        SearchPage searchPage = courseRepository.search(query, pageable, filterParameters, Arrays.stream(status.split(",")).map(Status::forValue).collect(Collectors.toList()), ownerParameters);

        return ResponseEntity.ok(new SearchResults(searchPage, pageable));
    }

    @GetMapping("/courses")
    public ResponseEntity<PageResults<Course>> unauth(Pageable pageable) {
        return ResponseEntity.ok(new PageResults<>(Page.empty(), pageable));
    }

}
