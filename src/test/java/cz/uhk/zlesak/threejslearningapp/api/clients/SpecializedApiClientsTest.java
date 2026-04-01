package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.model.*;
import cz.uhk.zlesak.threejslearningapp.domain.quiz.*;
import cz.uhk.zlesak.threejslearningapp.domain.texture.TextureEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.client.HttpClientErrorException;
import cz.uhk.zlesak.threejslearningapp.api.contracts.ApiTokenContext;
import cz.uhk.zlesak.threejslearningapp.exceptions.ApiCallException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings({"rawtypes", "unchecked", "SameParameterValue", "DataFlowIssue"})
class SpecializedApiClientsTest {
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void quizApiClientShouldBuildExpectedEndpoints() throws Exception {
        TestQuizApiClient apiClient = new TestQuizApiClient(objectMapper);

        apiClient.readQuizStudent("quiz-1");
        assertEquals("http://localhost:8050/api/quiz/quiz-1/questions", apiClient.lastUrl);
        assertEquals("startQuiz", apiClient.lastParams[0]);
        assertEquals("true", apiClient.lastParams[1]);

        apiClient.readAll("quiz-1");
        assertEquals("http://localhost:8050/api/quiz/quiz-1/all", apiClient.lastUrl);
        assertEquals(QuizEntity.class, apiClient.entityClass());
        assertEquals(QuickQuizEntity.class, apiClient.quickEntityClass());
    }

    @Test
    void quizResultApiClientShouldPostValidationRequestToExpectedEndpoint() throws Exception {
        TestQuizResultApiClient apiClient = new TestQuizResultApiClient(objectMapper);
        QuizSubmissionRequest request = new QuizSubmissionRequest("quiz-9", List.of());

        apiClient.validateAnswers(request);

        assertEquals("http://localhost:8050/api/quiz-result/validate-result", apiClient.lastUrl);
        assertEquals("quiz-9", ((QuizSubmissionRequest) apiClient.lastBody).getQuizId());
        assertEquals(QuizValidationResult.class, apiClient.entityClass());
        assertEquals(QuickQuizResult.class, apiClient.quickEntityClass());
    }

    @Test
    void chapterAndDocumentationClientsShouldExposeEntityTypes() {
        ChapterApiClient chapterApiClient = new ChapterApiClient(mock(RestClient.class), objectMapper);
        DocumentationApiClient documentationApiClient = new DocumentationApiClient(mock(RestClient.class), objectMapper);

        assertEquals("ChapterEntity", invokeTypeGetter(chapterApiClient, "getEntityClass").getSimpleName());
        assertEquals("ChapterEntity", invokeTypeGetter(chapterApiClient, "getQuicEntityClass").getSimpleName());
        assertEquals("DocumentationEntry", invokeTypeGetter(documentationApiClient, "getEntityClass").getSimpleName());
        assertEquals("DocumentationEntry", invokeTypeGetter(documentationApiClient, "getQuicEntityClass").getSimpleName());
    }

    @Test
    void modelApiClientShouldRejectReadAndBuildMultipartDescriptors() throws Exception {
        ModelApiClient apiClient = new ModelApiClient(mock(RestClient.class), objectMapper);
        ModelEntity entity = modelEntity();

        assertThrows(UnsupportedOperationException.class, () -> apiClient.read("model-1"));

        InputFileDesc fileDesc = (InputFileDesc) invoke(apiClient, "buildInputFileDesc", new Class[]{ModelEntity.class}, entity);
        assertEquals(FileSenseType.MODEL, fileDesc.getFileSenseType());
        assertEquals(2, fileDesc.getRelatedFiles().size());
        assertEquals(FileSenseType.MAIN_TEXTURE, fileDesc.getRelatedFiles().getFirst().getFileSenseType());
        assertEquals(FileSenseType.OTHER_TEXTURE, fileDesc.getRelatedFiles().get(1).getFileSenseType());
        assertEquals(FileSenseType.CSV_FILE, fileDesc.getRelatedFiles().get(1).getRelatedFiles().getFirst().getFileSenseType());

        MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) invoke(
                apiClient,
                "buildMultipartBody",
                new Class[]{ModelEntity.class, InputFileDesc.class, Object.class},
                entity,
                fileDesc,
                new cz.uhk.zlesak.threejslearningapp.domain.model.ModelMetadata("thumb", true)
        );

        assertEquals(4, body.get("files").size());
        assertTrue(body.containsKey("metadata"));
        assertTrue(body.containsKey("modelMetadata"));

        HttpEntity<?> metadataEntity = (HttpEntity<?>) body.getFirst("metadata");
        String metadataJson = (String) metadataEntity.getBody();
        assertNotNull(metadataJson);
        assertTrue(metadataJson.contains("\"originalFileName\":\"organ.glb\""));
        assertTrue(metadataJson.contains("\"fileSenseType\":\"MODEL\""));

        HttpEntity<?> modelMetadataEntity = (HttpEntity<?>) body.getFirst("modelMetadata");
        String modelMetadataJson = (String) modelMetadataEntity.getBody();
        assertNotNull(modelMetadataJson);
        assertTrue(modelMetadataJson.contains("\"isAdvanced\":true"));

        HttpEntity<?> firstFile = (HttpEntity<?>) body.get("files").getFirst();
        assertEquals("form-data; name=\"files\"; filename=\"organ.glb\"", firstFile.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertNotEquals("atlas.csv", invokeToJpgName(apiClient, "atlas.csv"));
    }

    @Test
    void modelApiClientShouldCreateUpdateDownloadAndReadTree() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodyUriSpec putSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.headers(any())).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.uri(anyString())).thenReturn(putSpec);
        when(putSpec.contentType(any(MediaType.class))).thenReturn(putSpec);
        when(putSpec.headers(any())).thenReturn(putSpec);
        when(putSpec.body(any(Object.class))).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);

        when(responseSpec.body(String.class))
                .thenReturn("""
                        {"id":"created-1","metadataId":"meta-1","name":"Lebka","advanced":true}
                        """)
                .thenReturn("""
                        {"id":"file-2","metadataId":"meta-2","name":"Atlas","advanced":false}
                        """);
        when(responseSpec.body(FileEntityTree.class)).thenReturn(FileEntityTree.builder().id("tree-1").name("Root").build());
        when(responseSpec.toEntity(byte[].class)).thenReturn(new ResponseEntity<>(
                "mesh".getBytes(StandardCharsets.UTF_8),
                headersWithDisposition("attachment; filename=\"atlas.glb\""),
                HttpStatus.OK
        ));

        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override
            public String getJwtToken() {
                return null;
            }
        };

        QuickModelEntity created = apiClient.create(modelEntity());
        ModelEntity updated = apiClient.update("meta-2", modelEntity());
        FileEntityTree tree = apiClient.readFileEntityTree("meta-3");
        InputStreamMultipartFile downloaded = apiClient.downloadFile("file-4");

        assertEquals("created-1", created.getId());
        assertEquals("meta-2", updated.getMetadataId());
        assertFalse(updated.isAdvanced());
        assertNotNull(tree);
        assertEquals("tree-1", tree.getId());
        assertEquals("atlas.glb", downloaded.getOriginalFilename());
        assertEquals("mesh", new String(downloaded.getBytes(), StandardCharsets.UTF_8));
    }

    private Class<?> invokeTypeGetter(Object target, String name) {
        try {
            Method method = target.getClass().getDeclaredMethod(name);
            method.setAccessible(true);
            return (Class<?>) method.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private String invokeToJpgName(ModelApiClient target, String filename) throws Exception {
        Method method = ModelApiClient.class.getDeclaredMethod("toJpgName", String.class);
        method.setAccessible(true);
        return (String) method.invoke(target, filename);
    }

    private HttpHeaders headersWithDisposition(String disposition) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, disposition);
        return headers;
    }

    private ModelEntity modelEntity() {
        return ModelEntity.builder()
                .name("Lebka")
                .description("thumb")
                .inputStreamMultipartFile(file("organ.glb", "glb"))
                .fullMainTexture(TextureEntity.builder().textureFile(file("main.jpg", "main")).build())
                .fullOtherTextures(List.of(TextureEntity.builder().textureFile(file("atlas.jpg", "atlas")).build()))
                .csvFiles(List.of(file("atlas.csv", "ff0000;Atlas")))
                .model(ModelFileEntity.builder().id("file-1").name("Lebka").build())
                .otherTextures(List.of(TestFixtures.texture("tex-1", "file-1", "atlas.jpg", "ff0000;Atlas")))
                .isAdvanced(true)
                .build();
    }

    private InputStreamMultipartFile file(String name, String content) {
        return InputStreamMultipartFile.builder()
                .fileName(name)
                .displayName(name)
                .inputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    private static final class TestQuizApiClient extends QuizApiClient {
        private String lastUrl;
        private String[] lastParams;

        private TestQuizApiClient(ObjectMapper objectMapper) {
            super(mock(RestClient.class), objectMapper);
        }

        @Override
        protected <R> R sendGetRequest(String url, Class<R> responseType, String errorMessage, String entityId, String... params) {
            this.lastUrl = url;
            this.lastParams = params;
            return responseType.cast(QuizEntity.builder().id(entityId).build());
        }

        private Class<?> entityClass() {
            return getEntityClass();
        }

        private Class<?> quickEntityClass() {
            return getQuicEntityClass();
        }
    }

    private static final class TestQuizResultApiClient extends QuizResultApiClient {
        private String lastUrl;
        private Object lastBody;

        private TestQuizResultApiClient(ObjectMapper objectMapper) {
            super(mock(RestClient.class), objectMapper);
        }

        @Override
        protected <R> R sendPostRequest(String url, Object body, Class<R> responseType, String errorMessage, String entityId, HttpHeaders headers) {
            this.lastUrl = url;
            this.lastBody = body;
            return responseType.cast(QuizValidationResult.builder().id(entityId).build());
        }

        private Class<?> entityClass() {
            return getEntityClass();
        }

        private Class<?> quickEntityClass() {
            return getQuicEntityClass();
        }
    }
    // ---- ModelApiClient additional coverage tests ----

    @Test
    void addFilePart_byteArrayResourceGetFilenameShouldReturnOriginalFilename() throws Exception {
        ModelApiClient apiClient = new ModelApiClient(mock(RestClient.class), objectMapper);
        ModelEntity entity = modelEntity();
        InputFileDesc fileDesc = (InputFileDesc) invoke(
                apiClient, "buildInputFileDesc", new Class[]{ModelEntity.class}, entity);
        MultiValueMap<String, Object> body = (MultiValueMap<String, Object>) invoke(
                apiClient, "buildMultipartBody",
                new Class[]{ModelEntity.class, InputFileDesc.class, Object.class},
                entity, fileDesc,
                new cz.uhk.zlesak.threejslearningapp.domain.model.ModelMetadata("", false));
        HttpEntity<?> firstFileEntity = (HttpEntity<?>) body.get("files").getFirst();
        ByteArrayResource resource = (ByteArrayResource) firstFileEntity.getBody();
        assertNotNull(resource);
        assertEquals("organ.glb", resource.getFilename());
    }

    @Test
    void sendMultipartPost_shouldThrowApiCallExceptionOnHttpStatusError() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad request", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8));
        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override public String getJwtToken() { return null; }
        };
        assertThrows(ApiCallException.class, () -> apiClient.create(modelEntity()));
    }

    @Test
    void sendMultipartPost_shouldWrapGenericExceptionFromPost() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("unexpected failure"));
        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override public String getJwtToken() { return null; }
        };
        Exception ex = assertThrows(Exception.class, () -> apiClient.create(modelEntity()));
        assertTrue(ex.getMessage().contains("Neočekávaná chyba") || ex.getCause() != null);
    }

    @Test
    void sendMultipartPut_shouldThrowApiCallExceptionOnHttpStatusError() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec putSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.uri(anyString())).thenReturn(putSpec);
        when(putSpec.contentType(any(MediaType.class))).thenReturn(putSpec);
        when(putSpec.body(any(Object.class))).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad request", HttpHeaders.EMPTY,
                new byte[0], StandardCharsets.UTF_8));
        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override public String getJwtToken() { return null; }
        };
        assertThrows(ApiCallException.class, () -> apiClient.update("meta-1", modelEntity()));
    }

    @Test
    void sendMultipartPut_shouldWrapGenericExceptionFromPut() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec putSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.uri(anyString())).thenReturn(putSpec);
        when(putSpec.contentType(any(MediaType.class))).thenReturn(putSpec);
        when(putSpec.body(any(Object.class))).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenThrow(new RuntimeException("put failure"));
        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override public String getJwtToken() { return null; }
        };
        Exception ex = assertThrows(Exception.class, () -> apiClient.update("meta-1", modelEntity()));
        assertTrue(ex.getMessage().contains("Neočekávaná chyba") || ex.getCause() != null);
    }

    @Test
    void executeWithUnauthorizedRetry_shouldRetryRequestWhenTokenContextIsSetAndResponseIs401() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.contentType(any(MediaType.class))).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class))
                .thenThrow(HttpClientErrorException.create(
                        HttpStatus.UNAUTHORIZED, "unauthorized", HttpHeaders.EMPTY,
                        new byte[0], StandardCharsets.UTF_8))
                .thenReturn("{\"id\":\"retry-ok\",\"metadataId\":\"meta-retry\",\"name\":\"T\",\"advanced\":false}");
        ModelApiClient apiClient = new ModelApiClient(restClient, objectMapper) {
            @Override public String getJwtToken() { return null; }
        };
        ApiTokenContext.set("bearer-token");
        try {
            QuickModelEntity result = apiClient.create(modelEntity());
            assertEquals("retry-ok", result.getId());
            assertNull(ApiTokenContext.get());
        } finally {
            ApiTokenContext.clear();
        }
    }

}