package uk.gov.cslearning.catalogue.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import uk.gov.cslearning.catalogue.domain.Course;

import java.util.List;

public interface CourseRequiredRepository {
    Page<Course> findRequired(String profession, String gradeCode, List<String>departments, List<String>otherAreasOfWork,  List<String>interests, String courseStatus,  Pageable pageable);
}