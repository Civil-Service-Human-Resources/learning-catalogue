package uk.gov.cslearning.catalogue.config;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.apache.tika.Tika;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.cslearning.catalogue.service.upload.processor.DefaultFileProcessor;
import uk.gov.cslearning.catalogue.service.upload.processor.FileProcessor;
import uk.gov.cslearning.catalogue.service.upload.processor.Mp4FileProcessor;
import uk.gov.cslearning.catalogue.service.upload.processor.ScormFileProcessor;
import uk.gov.cslearning.catalogue.service.upload.uploader.DefaultUploader;
import uk.gov.cslearning.catalogue.service.upload.uploader.ScormUploader;
import uk.gov.cslearning.catalogue.service.upload.uploader.Uploader;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.util.Map;

@Configuration
public class UploadConfig {
    @Bean
    public CloudBlobClient storageClient(CloudStorageAccount cloudStorageAccount) {
        return cloudStorageAccount.createCloudBlobClient();
    }

    @Bean(name = "uploaderMap")
    public Map<String, Uploader> uploaderMap(
            DefaultUploader defaultUploader,
            ScormUploader scormUploader
    ) {
        return ImmutableMap.<String, Uploader>builder()
                .put("doc",  defaultUploader) // MS Word
                .put("docx", defaultUploader) // MS Word
                .put("pdf",  defaultUploader) // PDF
                .put("ppsm", defaultUploader) // MS PowerPoint
                .put("ppt",  defaultUploader) // MS PowerPoint
                .put("pptx", defaultUploader) // MS PowerPoint
                .put("xls",  defaultUploader) // MS Excel
                .put("xlsx", defaultUploader) // MS Excel
                .put("mp4",  defaultUploader) // Video
                .put("zip",  scormUploader)   // Scorm
                .build();

    }

    @Bean("fileSubstitutions")
    public Map<String, String> fileSubstitutions() {
        return ImmutableMap.<String, String>builder()
                /* file-to-substitute => substituted-with */
                .put("js/player_management/close_methods.js", "/file-substitutions/close_methods.js") //GOMO
                .put("js/player_management/content_tracking/adapters/tincan_wrapper.js", "/file-substitutions/tincan_wrapper.js") //GOMO
                .put("js/player_management/portal_overrides.js", "/file-substitutions/portal_overrides.js") //GOMO
                .put("js/corePrimaryLoadList.min.js", "/file-substitutions/corePrimaryLoadList.min.js") //GOMO
                .put("story_content/user.js", "/file-substitutions/user.js") //Storyline
                .put("SCORMDriver/Configuration.js", "/file-substitutions/Configuration.js") //DominKNOW
                .build();
                
    }

    @Bean("fileProcessorMap")
    public Map<String, FileProcessor> fileProcessorMap(
            DefaultFileProcessor defaultFileProcessor,
            Mp4FileProcessor mp4FileProcessor,
            ScormFileProcessor scormFileProcessor
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
                .put("zip", scormFileProcessor) // Scorm
                .put("mp4",  mp4FileProcessor)     // Video
                .build();
    }

    @Bean
    public Tika tika() {
        return new Tika();
    }

    @Bean
    public DocumentBuilderFactory documentBuilderFactory() {
        return DocumentBuilderFactory.newInstance();
    }
    @Bean
    public XPathFactory xPathFactory() {
        return XPathFactory.newInstance();
    }
    @Bean("scormManifestXpathMap")
    public Map<String, String> scormManifestXpathMap() {
        return ImmutableMap.of(
                "imsmanifest.xml", "/manifest/resources/resource/@href",
                "tincan.xml", "/tincan/activities/activity/launch"
        );
    }
}
