package uk.gov.cslearning.catalogue.service.upload.uploader;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.gov.cslearning.catalogue.dto.UploadedFile;
import uk.gov.cslearning.catalogue.service.upload.FileFactory;
import uk.gov.cslearning.catalogue.service.upload.InputStreamFactory;
import uk.gov.cslearning.catalogue.service.upload.client.UploadClient;
import uk.gov.cslearning.catalogue.service.upload.processor.MetadataParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(IOUtils.class)
public class SubstituteZipEntryUploaderTest {

    @Mock
    private FileFactory fileFactory;

    @Mock
    private InputStreamFactory inputStreamFactory;

    @Mock
    private Map<String, String> fileSubstitutions;

    @Mock
    private MetadataParser metadataParser;

    @InjectMocks
    private SubstituteZipEntryUploader uploader;

    @Test
    public void shouldUploadReplacementFileAndReturnOptionalUploadedFile() throws URISyntaxException, IOException {
        String destinationPath = "destination-path";
        String zipEntryName = "zip-entry";
        String substitutePath = "substitute-path";
        String contentType = "content-type";
        InputStream byteArrayInputStream = mock(InputStream.class);

        ZipEntry zipEntry = mock(ZipEntry.class);
        when(zipEntry.getName()).thenReturn(zipEntryName);

        UploadClient uploadClient = mock(UploadClient.class);
        when(fileSubstitutions.get(zipEntryName)).thenReturn(substitutePath);

        File file = mock(File.class);
        long fileLength = 999;
        when(file.length()).thenReturn(fileLength);
        when(fileFactory.get(substitutePath)).thenReturn(file);

        InputStream inputStream = mock(InputStream.class);

        byte[] bytes = "Hello World!".getBytes();

        PowerMockito.mockStatic(IOUtils.class);
        when(IOUtils.toByteArray(inputStream)).thenReturn(bytes);

        when(inputStreamFactory.createFileInputStream(file)).thenReturn(inputStream);
        when(inputStreamFactory.createByteArrayInputStream(bytes)).thenReturn(byteArrayInputStream);

        when(metadataParser.getContentType(byteArrayInputStream, zipEntryName)).thenReturn(contentType);

        UploadedFile uploadedFile = mock(UploadedFile.class);
        when(uploadClient.upload(byteArrayInputStream, destinationPath, bytes.length, contentType)).thenReturn(uploadedFile);

        Optional<UploadedFile> result = uploader.upload(uploadClient, zipEntry, inputStream, destinationPath);

        assertTrue(result.isPresent());
        assertEquals(uploadedFile, result.get());
    }
}