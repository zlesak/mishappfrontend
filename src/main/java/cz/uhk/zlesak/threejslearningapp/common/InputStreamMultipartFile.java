package cz.uhk.zlesak.threejslearningapp.common;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * A simple implementation of MultipartFile backed by in-memory bytes.
 * Used to handle file uploads for model or texture files.
 * Stores the file name and a display name (fallbacks to fileName).
 */
@SuppressWarnings("NullableProblems")
public class InputStreamMultipartFile implements MultipartFile {
    private final byte[] content;
    private final String fileName;
    @Getter
    private String displayName;

    /**
     * Constructor for InputStreamMultipartFile.
     *
     * @param inputStream the InputStream of the file (will be fully read and then closed)
     * @param fileName    the name of the file
     * @param displayName the display name of the file, if null, it will be set to fileName
     */
    @Builder
    public InputStreamMultipartFile(InputStream inputStream, String fileName, String displayName) {
        byte[] bytes;
        if (inputStream == null) {
            bytes = new byte[0];
        } else {
            try (InputStream is = inputStream) {
                bytes = is.readAllBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read input stream", e);
            }
        }
        this.content = bytes;
        this.fileName = fileName;
        this.displayName = displayName != null ? displayName : (fileName != null ? fileName : "");
    }

    /**
     * Override of method for the name getter
     * @return FILE NAME!
     */
    @Override
    public @NonNull String getName() {
        return fileName != null ? fileName : "";
    }

    /**
     * Display name setter
     * @param displayName new display name
     */

    public void setDisplayName(String displayName) {
        this.displayName = displayName != null ? displayName : (fileName != null ? fileName : "");
    }

    /**
     * Original filename getter
     * @return original filename
     */
    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    /**
     * Content type getter
     * @return content type
     */
    @Override
    public String getContentType() {
        return "application/octet-stream";
    }

    /**
     * Checks if the file is empty
     * @return true if empty, false otherwise
     */
    @Override
    public boolean isEmpty() {
        return content == null || content.length == 0;
    }

    /**
     * Size getter
     * @return size of the file
     */
    @Override
    public long getSize() {
        return content.length;
    }

    /**
     * Gets the bytes of the file
     * @return byte array of the file content
     */
    @Override
    public @NonNull byte[] getBytes() {
        return content.clone();
    }

    /**
     * Gets the InputStream of the file
     * @return InputStream of the file content
     */
    @Override
    public @NonNull InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    /**
     * Transfers the file to the given destination file
     * @param dest the destination file
     * @throws IllegalStateException if the transfer fails
     */
    @Override
    public void transferTo(@NonNull File dest) throws IllegalStateException {
        throw new UnsupportedOperationException("Not supported.");
    }
}
