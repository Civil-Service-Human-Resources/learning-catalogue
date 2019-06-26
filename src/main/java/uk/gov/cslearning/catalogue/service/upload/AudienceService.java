package uk.gov.cslearning.catalogue.service.upload;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import uk.gov.cslearning.catalogue.domain.CivilServant.CivilServant;
import uk.gov.cslearning.catalogue.domain.Course;
import uk.gov.cslearning.catalogue.domain.module.Audience;
import uk.gov.cslearning.catalogue.service.AuthoritiesService;
import uk.gov.cslearning.catalogue.service.CourseService;
import uk.gov.cslearning.catalogue.service.RegistryService;

import java.util.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;

@Service
public class AudienceService {
    private CourseService courseService;
    private AuthoritiesService authoritiesService;
    private RegistryService registryService;

    public AudienceService(CourseService courseService, AuthoritiesService authoritiesService, RegistryService registryService) {
        this.courseService = courseService;
        this.authoritiesService = authoritiesService;
        this.registryService = registryService;
    }

    public Course save(String courseId, Audience audience) {
        Course course = courseService.getCourseById(courseId);

        String generatedAudienceName = generateAudienceName(audience);
        audience.setName(generatedAudienceName);

        Set<Audience> audiences = new HashSet<>(course.getAudiences());
        audiences.add(audience);
        course.setAudiences(audiences);

        return courseService.save(course);
    }

    public Course updateAudience(Course course, Audience newAudience, Audience audience) {
        String generatedAudienceName = generateAudienceName(newAudience);
        newAudience.setName(generatedAudienceName);

        Set<Audience> audiences = new HashSet<>(course.getAudiences());
        audiences.remove(audience);
        audiences.add(newAudience);

        course.setAudiences(audiences);

        return courseService.save(course);
    }

    public Optional<Audience> find(String courseId, String audienceId) {
        Course course = courseService.getCourseById(courseId);

        return find(course, audienceId);
    }

    public Optional<Audience> find(Course course, String audienceId) {
        return course.getAudiences().stream()
                .filter(audience -> audience.getId().equals(audienceId))
                .findFirst();
    }

    public boolean isPermitted(String courseId, Authentication authentication) {
        if (authoritiesService.isCslAuthor(authentication) || authoritiesService.isLearningManager(authentication)) {
            return true;
        }

        Course course = courseService.getCourseById(courseId);

        if (course.getOwner() != null) {
            CivilServant civilServant = registryService.getCurrentCivilServant();

            return isOrganisationPermitted(authentication, course, civilServant) || isProfessionPermitted(authentication, course, civilServant);
        }
        return false;
    }

    private boolean isOrganisationPermitted(Authentication authentication, Course course, CivilServant civilServant) {
        return authoritiesService.isOrgAuthor(authentication) && authoritiesService.isOrganisationalUnitCodeEqual(civilServant, course.getOwner());
    }

    private boolean isProfessionPermitted(Authentication authentication, Course course, CivilServant civilServant) {
        return authoritiesService.isProfessionAuthor(authentication) && authoritiesService.isProfessionIdEqual(civilServant, course.getOwner());
    }

    public Audience setDefaults(Authentication authentication, Audience audience) {
        CivilServant civilServant = registryService.getCurrentCivilServant();

        if (authoritiesService.isOrgAuthor(authentication)) {
            civilServant.getOrganisationalUnitCode().ifPresent(organisationalUnitCode -> audience.setDepartments(new HashSet<>(Arrays.asList(organisationalUnitCode))));
        } else if (authoritiesService.isProfessionAuthor(authentication)) {
            civilServant.getProfessionName().ifPresent(professionName -> audience.setAreasOfWork(new HashSet<>(Arrays.asList(professionName))));
        }
        return audience;
    }

    private String generateAudienceName(Audience audience) {
        String organisations = String.join(", ", audience.getDepartments());
        String areasOfWork = String.join(", ", audience.getAreasOfWork());
        String interests = String.join(", ", audience.getInterests());

        ArrayList<String> audienceNameComponents = new ArrayList<>();

        if (!organisations.isEmpty()) {
            audienceNameComponents.add(organisations);
        }
        if (!areasOfWork.isEmpty()) {
            audienceNameComponents.add(areasOfWork);
        }
        if (!interests.isEmpty()) {
            audienceNameComponents.add(interests);
        }

        String audienceName = String.join(", ", audienceNameComponents);

        if (audienceName.isEmpty()) {
            String hash = randomAlphabetic(4).toUpperCase();
            audienceName = "Temp – " + hash;
        }

        return audienceName;
    }
}
