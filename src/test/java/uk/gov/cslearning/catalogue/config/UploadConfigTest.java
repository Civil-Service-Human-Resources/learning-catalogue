package uk.gov.cslearning.catalogue.config;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import org.apache.tika.Tika;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CloudBlobClient.class, CloudStorageAccount.class})
public class UploadConfigTest {

    private final UploadConfig config = new UploadConfig();

    @Test
    public void shouldHaveCorrectUploaderMappings() {
        DefaultUploader defaultUploader = mock(DefaultUploader.class);
        ScormUploader scormUploader = mock(ScormUploader.class);

        Map<String, Uploader> uploaderMap = ImmutableMap.<String, Uploader>builder()
                .put("doc", defaultUploader)
                .put("docx", defaultUploader)
                .put("pdf", defaultUploader)
                .put("ppsm", defaultUploader)
                .put("ppt", defaultUploader)
                .put("pptx", defaultUploader)
                .put("xls", defaultUploader)
                .put("xlsx", defaultUploader)
                .put("mp4", defaultUploader)
                .put("zip", scormUploader)
                .build();

        assertEquals(uploaderMap.keySet(), config.uploaderMap(defaultUploader, scormUploader).keySet());

        uploaderMap.forEach((key, value) -> assertEquals(
                key, value, config.uploaderMap(defaultUploader, scormUploader).get(key)));
    }

    @Test
    public void shouldHaveCorrectFileSubstitutionMap() {
        Map<String, String> substitutionMap = ImmutableMap.<String, String>builder()
                /* file-to-substitute => substituted-with */
                .put("js/player_management/close_methods.js", "/file-substitutions/close_methods.js") //GOMO
                .put("js/player_management/content_tracking/adapters/tincan_wrapper.js", "/file-substitutions/tincan_wrapper.js") //GOMO
                .put("js/player_management/portal_overrides.js", "/file-substitutions/portal_overrides.js") //GOMO
                .put("js/corePrimaryLoadList.min.js", "/file-substitutions/corePrimaryLoadList.min.js") //GOMO
                .put("story_content/user.js", "/file-substitutions/user.js") //Storyline
                .put("SCORMDriver/Configuration.js", "/file-substitutions/Configuration.js") //DominKNOW
                .build();

        assertEquals(substitutionMap.keySet(), config.fileSubstitutions().keySet());
        substitutionMap.forEach((key, value) -> assertEquals(key, value, config.fileSubstitutions().get(key)));
    }


    @Test
    public void storageClientShouldReturnCloudBlobClient() {
        CloudStorageAccount cloudStorageAccount = PowerMockito.mock(CloudStorageAccount.class);
        CloudBlobClient cloudBlobClient = PowerMockito.mock(CloudBlobClient.class);

        PowerMockito.when(cloudStorageAccount.createCloudBlobClient()).thenReturn(cloudBlobClient);

        assertEquals(cloudBlobClient, config.storageClient(cloudStorageAccount));
    }


    @Test
    public void shouldHaveCorrectFileProcessorMap() {
        DefaultFileProcessor defaultFileProcessor = mock(DefaultFileProcessor.class);
        Mp4FileProcessor mp4FileProcessor = mock(Mp4FileProcessor.class);
        ScormFileProcessor scormFileProcessor = mock(ScormFileProcessor.class);

        Map<String, FileProcessor> fileProcessorMap = ImmutableMap.<String, FileProcessor>builder()
                .put("doc",  defaultFileProcessor) // MS Word
                .put("docx", defaultFileProcessor) // MS Word
                .put("pdf",  defaultFileProcessor) // PDF
                .put("ppsm", defaultFileProcessor) // MS PowerPoint
                .put("ppt",  defaultFileProcessor) // MS PowerPoint
                .put("pptx", defaultFileProcessor) // MS PowerPoint
                .put("xls",  defaultFileProcessor) // MS Excel
                .put("xlsx", defaultFileProcessor) // MS Excel
                .put("zip",  scormFileProcessor) // Scorm
                .put("mp4",  mp4FileProcessor)     // Video
                .build();

        assertEquals(fileProcessorMap.keySet(),
                config.fileProcessorMap(defaultFileProcessor, mp4FileProcessor, scormFileProcessor).keySet());


        fileProcessorMap.forEach((key, value) -> assertEquals(key, value,
                config.fileProcessorMap(defaultFileProcessor, mp4FileProcessor, scormFileProcessor).get(key)));
    }

    @Test
    public void tikaShouldReturnTika() {
        Tika tika = config.tika();
        assertNotNull(tika);
    }

    @Test
    public void shouldReturnDocumentBuilderFatory() {
        DocumentBuilderFactory documentBuilderFactory = config.documentBuilderFactory();
        assertNotNull(documentBuilderFactory);
    }

    @Test
    public void shouldReturnXPathFctory() {
        XPathFactory xPathFactory = config.xPathFactory();
        assertNotNull(xPathFactory);
    }

    @Test
    public void shouldHaveCorrectManifestXPathMap() {
        Map<String, String> xPathMap = config.scormManifestXpathMap();
        assertEquals("/manifest/resources/resource/@href", xPathMap.get("imsmanifest.xml"));
        assertEquals("/tincan/activities/activity/launch", xPathMap.get("tincan.xml"));
    }
}