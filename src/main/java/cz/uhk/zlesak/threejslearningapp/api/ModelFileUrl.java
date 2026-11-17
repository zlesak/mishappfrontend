package cz.uhk.zlesak.threejslearningapp.api;

/**
 * Builds the URLs the browser uses to fetch stored model files.
 * The 3D viewer loads models and textures directly, so those two are the only pieces of the
 * backend the browser addresses over HTTP.
 */
public final class ModelFileUrl {

    private static final String DOWNLOAD_PATH = "/api/model/download/";

    private ModelFileUrl() {
    }

    /**
     * @param fileId id of a stored file.
     * @return URL the browser can fetch the file from.
     * @throws IllegalArgumentException when the id is not a plain identifier, which would let a
     *                                  crafted value escape the download path.
     */
    public static String of(String fileId) {
        if (fileId == null || !fileId.matches("[A-Za-z0-9_-]+")) {
            throw new IllegalArgumentException("Neplatné ID souboru modelu.");
        }
        return DOWNLOAD_PATH + fileId;
    }

    /**
     * Extracts the file id from a URL previously produced by {@link #of(String)}.
     *
     * @param url URL to inspect, may be {@code null}.
     * @return the file id, or {@code null} when the URL does not point at a stored file.
     */
    public static String fileIdOf(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        int start = url.indexOf(DOWNLOAD_PATH);
        if (start < 0) {
            return null;
        }

        String rest = url.substring(start + DOWNLOAD_PATH.length());
        int end = rest.indexOf('?');
        if (end >= 0) {
            rest = rest.substring(0, end);
        }
        end = rest.indexOf('/');
        if (end >= 0) {
            rest = rest.substring(0, end);
        }

        String trimmed = rest.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
