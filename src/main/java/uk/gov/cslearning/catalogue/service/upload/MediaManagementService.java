package uk.gov.cslearning.catalogue.service.upload;

import uk.gov.cslearning.catalogue.domain.media.MediaEntity;
import uk.gov.cslearning.catalogue.dto.FileUpload;

import java.util.Optional;

public interface MediaManagementService {
    MediaEntity create(FileUpload fileUpload);

    Optional<MediaEntity> findById(String mediaId);
}
