package uk.gov.cslearning.catalogue.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.cslearning.catalogue.domain.media.Media;
import uk.gov.cslearning.catalogue.dto.FileUpload;
import uk.gov.cslearning.catalogue.service.FileUploadFactory;
import uk.gov.cslearning.catalogue.service.MediaManagementService;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringJUnit4ClassRunner.class)
@WebMvcTest(MediaController.class)
@WithMockUser(username = "user", password = "password")
public class MediaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MediaManagementService mediaManagementService;

    @MockBean
    private FileUploadFactory fileUploadFactory;

    @Test
    public void shouldUploadFileOnPostRequest() throws Exception {
        String fileContainer = "container-id";
        String mediaUid = "media-uid";

        MockMultipartFile file = new MockMultipartFile("file", "file.doc", "application/octet-stream", "abc".getBytes());
        FileUpload fileUpload = mock(FileUpload.class);

        when(fileUploadFactory.create(file, fileContainer)).thenReturn(fileUpload);

        Media media = mock(Media.class);
        when(media.getUid()).thenReturn(mediaUid);
        when(mediaManagementService.create(fileUpload)).thenReturn(media);

        mockMvc.perform(
                multipart("/media")
                        .file(file)
                        .param("container", fileContainer)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON).with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(header().string("location", "http://localhost/service/media/" + mediaUid));
    }
}