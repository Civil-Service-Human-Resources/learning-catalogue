package uk.gov.cslearning.catalogue.api;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.cslearning.catalogue.dto.CourseDto;
import uk.gov.cslearning.catalogue.dto.EventDto;
import uk.gov.cslearning.catalogue.dto.ModuleDto;
import uk.gov.cslearning.catalogue.mapping.RoleMapping;
import uk.gov.cslearning.catalogue.service.CourseService;
import uk.gov.cslearning.catalogue.service.EventService;
import uk.gov.cslearning.catalogue.service.ModuleService;

import java.util.Map;

@RestController
@RequestMapping("/reporting")
public class ReportController {

    private static final PageRequest MAX_PAGEABLE = PageRequest.of(0, 10000);
    private final EventService eventService;
    private final ModuleService moduleService;
    private final CourseService courseService;

    public ReportController(EventService eventService, ModuleService moduleService, CourseService courseService) {
        this.eventService = eventService;
        this.moduleService = moduleService;
        this.courseService = courseService;
    }

    @GetMapping("/mandatory-courses")
    public ResponseEntity<Map<String, CourseDto>> getPublishedAndArchivedMandatoryCourses() {
        return ResponseEntity.ok(courseService.getPublishedAndArchivedMandatoryCourses());
    }

    @GetMapping("/modules")
    public ResponseEntity<Map<String, ModuleDto>> getModules() {
        return ResponseEntity.ok(moduleService.getModuleMap());
    }

    @RoleMapping("KPMG_SUPPLIER_REPORTER")
    @GetMapping("/modules")
    public ResponseEntity<Map<String, ModuleDto>> getModulesForKPMG() {
        return ResponseEntity.ok(moduleService.getModuleMapForSupplier("KPMG", MAX_PAGEABLE));
    }

    @RoleMapping("KORNFERRY_SUPPLIER_REPORTER")
    @GetMapping("/modules")
    public ResponseEntity<Map<String, ModuleDto>> getModulesForKornferry() {
        return ResponseEntity.ok(moduleService.getModuleMapForSupplier("KORNFERRY", MAX_PAGEABLE));
    }

    @RoleMapping("KPMG_SUPPLIER_REPORTER")
    @GetMapping("/events")
    public ResponseEntity<Map<String, EventDto>> getEventsForSupplier() {
        return ResponseEntity.ok(eventService.getEventMapBySupplier("KPMG", MAX_PAGEABLE));
    }

    @RoleMapping("KORNFERRY_SUPPLIER_REPORTER")
    @GetMapping("/events")
    public ResponseEntity<Map<String, EventDto>> getEventsForKornferrySupplier() {
        return ResponseEntity.ok(eventService.getEventMapBySupplier("KORNFERRY", MAX_PAGEABLE));
    }

    @GetMapping("/events")
    public ResponseEntity<Map<String, EventDto>> getEvents() {
        return ResponseEntity.ok(eventService.getEventMap());
    }
}
