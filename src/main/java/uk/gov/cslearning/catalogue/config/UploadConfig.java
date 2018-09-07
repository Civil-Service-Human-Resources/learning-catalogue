package uk.gov.cslearning.catalogue.config;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.cslearning.catalogue.service.upload.processor.DefaultFileProcessor;
import uk.gov.cslearning.catalogue.service.upload.processor.FileProcessor;
import uk.gov.cslearning.catalogue.service.upload.processor.mp4.Mp4FileProcessor;
import uk.gov.cslearning.catalogue.service.upload.uploader.DefaultUploader;
import uk.gov.cslearning.catalogue.service.upload.uploader.ScormUploader;
import uk.gov.cslearning.catalogue.service.upload.uploader.Uploader;

import java.util.Map;
import java.util.function.Supplier;

@Configuration
public class UploadConfig {
    @Bean
    public CloudBlobClient storageClient(CloudStorageAccount cloudStorageAccount) {
        return cloudStorageAccount.createCloudBlobClient();
    }

    @Bean(name = "uploaderFactoryMethods")
    public Map<String, Supplier<Uploader>> uploaderFactoryMethods(
            DefaultUploader defaultUploader,
            ScormUploader scormUploader
    ) {
        return ImmutableMap.<String, Supplier<Uploader>>builder()
                .put("doc",  () -> defaultUploader) // MS Word
                .put("docx", () -> defaultUploader) // MS Word
                .put("pdf",  () -> defaultUploader) // PDF
                .put("ppsm", () -> defaultUploader) // MS PowerPoint
                .put("ppt",  () -> defaultUploader) // MS PowerPoint
                .put("pptx", () -> defaultUploader) // MS PowerPoint
                .put("xls",  () -> defaultUploader) // MS Excel
                .put("xlsx", () -> defaultUploader) // MS Excel
                .put("mp4",  () -> defaultUploader) // Video
                .put("zip",  () -> scormUploader)   // Scorm
                .build();

    }

    @Bean("fileSubstitutions")
    public Map<String, String> fileSubstitions() {
        return ImmutableMap.of(
                /* file-to-substitute => substituted-with */
                "js/player_management/close_methods.js", "/file-substitutions/close_methods.js", //GOMO
                "js/player_management/content_tracking/adapters/tincan_wrapper.js", "/file-substitutions/tincan_wrapper.js", //GOMO
                "js/player_management/portal_overrides.js", "/file-substitutions/portal_overrides.js", //GOMO
                "story_content/user.js", "/file-substitutions/user.js", //Storyline
                "SCORMDriver/Configuration.js", "/file-substitutions/Configuration.js" // DominKNOW
        );
    }

    @Bean("fileProcessorMap")
    public Map<String, FileProcessor> fileProcessorMap(
            DefaultFileProcessor defaultFileProcessor,
            Mp4FileProcessor mp4FileProcessor
    ) {
        return ImmutableMap.<String, FileProcessor>builder()
                .put("doc",  defaultFileProcessor) // MS Word
                .put("docx", defaultFileProcessor) // MS Word
                .put("pdf",  defaultFileProcessor) // PDF
                .put("ppsm", defaultFileProcessor) // MS PowerPoint
                .put("ppt",  defaultFileProcessor) // MS PowerPoint
                .put("pptx", defaultFileProcessor) // MS PowerPoint
                .put("xls",  defaultFileProcessor) // MS Excel
                .put("xlsx", defaultFileProcessor) // MS Excel
                .put("zip",  defaultFileProcessor) // Scorm
                .put("mp4",  mp4FileProcessor)     // Video
                .build();
    }
}
