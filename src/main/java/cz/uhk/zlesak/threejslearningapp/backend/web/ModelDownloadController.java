package cz.uhk.zlesak.threejslearningapp.backend.web;

import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.backend.service.FileStorageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves stored model and texture files over HTTP.
 * This is the one part of the backend that has to stay an HTTP endpoint: the Three.js viewer loads
 * models straight from the browser, so it cannot go through the in-process client.
 */
@RestController
@RequestMapping("/api/model/download")
@RequiredArgsConstructor
public class ModelDownloadController {

    private final FileStorageService fileStorageService;

    /**
     * Streams one stored file to the browser.
     *
     * @param fileId   id of the file.
     * @param response response to stream into.
     * @throws IOException if the connection drops mid-transfer.
     */
    @GetMapping("/{fileId}")
    public void download(@PathVariable String fileId, HttpServletResponse response) throws IOException {
        if (!fileId.matches("[A-Za-z0-9_-]+")) {
            throw new BackendException.Validation("Neplatné ID souboru.");
        }

        GridFsResource resource = fileStorageService.requireResource(fileId);

        response.setContentType(resource.getContentType() != null
                ? resource.getContentType()
                : MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLengthLong(resource.contentLength());
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, contentDisposition(resource.getFilename()));

        try (InputStream input = resource.getInputStream()) {
            input.transferTo(response.getOutputStream());
        }
    }

    /**
     * Builds the {@code Content-Disposition} header for a stored file.
     *
     * <p>The name comes from whoever uploaded the model and was pasted into the header as it
     * stood. A quotation mark in it ended the quoted string early, so the browser saved the file
     * under a truncated name, and a Czech name was sent as raw bytes with no encoding declared.
     * Spring's builder escapes the quoted form and adds the RFC 5987 {@code filename*} parameter
     * beside it, which is what makes a name with diacritics arrive intact.
     *
     * @param filename name the file was stored under, may be {@code null}.
     * @return the header value.
     */
    private static String contentDisposition(String filename) {
        ContentDisposition.Builder disposition = ContentDisposition.attachment();
        if (filename != null && !filename.isBlank()) {
            disposition = disposition.filename(filename, StandardCharsets.UTF_8);
        }
        return disposition.build().toString();
    }

    /**
     * Reports a missing file as 404 rather than letting it surface as a server error.
     *
     * @param exception the failure.
     * @return the message to show.
     */
    @ExceptionHandler(BackendException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(BackendException.NotFound exception) {
        return exception.getMessage();
    }

    /**
     * Reports a malformed file id as 400.
     *
     * @param exception the failure.
     * @return the message to show.
     */
    @ExceptionHandler(BackendException.Validation.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String badRequest(BackendException.Validation exception) {
        return exception.getMessage();
    }
}
