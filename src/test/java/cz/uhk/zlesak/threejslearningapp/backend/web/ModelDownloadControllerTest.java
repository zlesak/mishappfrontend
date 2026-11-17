package cz.uhk.zlesak.threejslearningapp.backend.web;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelDownloadControllerTest {

    private FileStorageService fileStorageService;
    private ModelDownloadController controller;

    @BeforeEach
    void setUp() {
        fileStorageService = Mockito.mock(FileStorageService.class);
        controller = new ModelDownloadController(fileStorageService);
    }

    @Test
    void doesNotLetAQuoteInTheFileNameBreakTheHeader() throws IOException {
        // The name is whatever the uploader called the file. An unescaped quote used to close the
        // quoted string early, leaving the rest of the name loose in the header.
        MockHttpServletResponse response = download("eviL\"; download; x=\"a.glb");

        String header = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(header).startsWith("attachment;");
        // Exactly one quoted string: the two quotes are the ones delimiting the filename, so the
        // name's own quote did not end it early and smuggle "download" in as a parameter.
        assertThat(header.chars().filter(character -> character == '"').count()).isEqualTo(2);
        assertThat(header).doesNotContain("\"; download");
    }

    @Test
    void sendsANameWithDiacriticsInADeclaredEncoding() throws IOException {
        MockHttpServletResponse response = download("lebka-příčná.glb");

        String header = response.getHeader(HttpHeaders.CONTENT_DISPOSITION);
        assertThat(header).contains("filename*=UTF-8''");
        assertThat(header).contains("p%C5%99%C3%AD%C4%8Dn%C3%A1");
    }

    @Test
    void stillWorksForAFileStoredWithoutAName() throws IOException {
        MockHttpServletResponse response = download(null);

        assertThat(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)).isEqualTo("attachment");
    }

    @Test
    void refusesAFileIdThatIsNotAPlainIdentifier() {
        assertThatThrownBy(() -> controller.download("../../etc/passwd", new MockHttpServletResponse()))
                .isInstanceOf(BackendException.Validation.class);

        Mockito.verifyNoInteractions(fileStorageService);
    }

    private MockHttpServletResponse download(String filename) throws IOException {
        byte[] content = "glb".getBytes();
        GridFsResource resource = Mockito.mock(GridFsResource.class);
        Mockito.when(resource.getFilename()).thenReturn(filename);
        Mockito.when(resource.getContentType()).thenReturn("model/gltf-binary");
        Mockito.when(resource.contentLength()).thenReturn((long) content.length);
        Mockito.when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(content));
        Mockito.when(fileStorageService.requireResource("abc123")).thenReturn(resource);

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.download("abc123", response);
        return response;
    }
}
