package cz.uhk.zlesak.threejslearningapp.api.clients;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.common.InputStreamMultipartFile;
import cz.uhk.zlesak.threejslearningapp.domain.common.FilterParameters;
import cz.uhk.zlesak.threejslearningapp.domain.common.PageResult;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity;
import cz.uhk.zlesak.threejslearningapp.exceptions.ApiCallException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SuppressWarnings({"rawtypes", "unchecked", "SameParameterValue", "UnusedReturnValue"})
class AbstractApiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void filterToQueryParams_shouldIncludeInheritedFieldsAndSkipNulls() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ModelFilter filter = ModelFilter.builder()
                .Name("Femur Model")
                .CreatorId("user-1")
                .CreatedFrom(Instant.parse("2024-02-03T10:15:30Z"))
                .SearchText("bone anatomy")
                .build();

        String query = apiClient.exposeFilterToQueryParams(filter);

        assertTrue(query.startsWith("&"));
        assertTrue(query.contains("Name=Femur+Model"));
        assertTrue(query.contains("CreatorId=user-1"));
        assertTrue(query.contains("CreatedFrom=2024-02-03T10%3A15%3A30Z"));
        assertTrue(query.contains("SearchText=bone+anatomy"));
        assertFalse(query.contains("CreatedTo"));
    }

    @Test
    void filterToQueryParams_shouldReturnEmptyStringForNullFilter() {
        TestApiClient apiClient = new TestApiClient(objectMapper);

        assertEquals("", apiClient.exposeFilterToQueryParams(null));
    }

    @Test
    void pageRequestToQueryParams_shouldBuildPaginatedUrlWithFilter() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        FilterParameters<ModelFilter> filterParameters = new FilterParameters<>();
        filterParameters.setPageRequest(PageRequest.of(2, 25, Sort.Direction.DESC, "created"));
        filterParameters.setFilter(ModelFilter.builder().SearchText("atlas").build());

        String url = apiClient.exposePageRequestToQueryParams(filterParameters, null);

        assertEquals(
                "http://localhost:8050/api/test/list?limit=25&page=2&orderBy=created&sortDirection=DESC&SearchText=atlas",
                url
        );
    }

    @Test
    void pageRequestToQueryParams_shouldUseDefaultSortingAndCustomBaseUrl() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        FilterParameters<ModelFilter> filterParameters = new FilterParameters<>();
        filterParameters.setPageRequest(PageRequest.of(0, 10));
        filterParameters.setFilter(null);

        String url = apiClient.exposePageRequestToQueryParams(filterParameters, "search");

        assertEquals("http://localhost:8050/api/test/search?limit=10&page=0&orderBy=id&sortDirection=ASC", url);
    }

    @Test
    void parseResponse_shouldDeserializePageResult() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ResponseEntity<String> response = ResponseEntity.ok("""
                {"elements":[{"id":"model-1","name":"Femur"}],"total":1,"page":0}
                """);
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(PageResult.class, QuickModelEntity.class);

        PageResult<QuickModelEntity> result = apiClient.exposeParseResponse(response, type);

        assertEquals(1L, result.total());
        assertEquals(0, result.page());
        assertEquals("model-1", result.elements().getFirst().getId());
        assertEquals("Femur", result.elements().getFirst().getName());
    }

    @Test
    void parseResponse_shouldThrowForNonSuccessStatus() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ResponseEntity<String> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("failure");
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(PageResult.class, QuickModelEntity.class);

        assertThrows(ApiCallException.class, () -> apiClient.exposeParseResponse(response, type));
    }

    @Test
    void parseResponse_shouldThrowForMissingBody() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ResponseEntity<String> response = ResponseEntity.ok().build();
        JavaType type = objectMapper.getTypeFactory()
                .constructParametricType(PageResult.class, QuickModelEntity.class);

        assertThrows(ApiCallException.class, () -> apiClient.exposeParseResponse(response, type));
    }

    @Test
    void parseFileResponse_shouldCreateMultipartFileWithFilename() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"atlas.csv\"");
        ResponseEntity<byte[]> response = new ResponseEntity<>(
                "a;b;c".getBytes(StandardCharsets.UTF_8),
                headers,
                HttpStatus.OK
        );

        InputStreamMultipartFile file = apiClient.exposeParseFileResponse(response);

        assertEquals("atlas.csv", file.getOriginalFilename());
        assertEquals("atlas.csv", file.getDisplayName());
        assertEquals("a;b;c", new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    @Test
    void parseFileResponse_shouldAllowMissingFilename() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ResponseEntity<byte[]> response = ResponseEntity.ok("a;b;c".getBytes(StandardCharsets.UTF_8));

        InputStreamMultipartFile file = apiClient.exposeParseFileResponse(response);

        assertNull(file.getOriginalFilename());
        assertEquals("", file.getDisplayName());
    }

    @Test
    void parseFileResponse_shouldThrowForNonSuccessStatus() {
        TestApiClient apiClient = new TestApiClient(objectMapper);
        ResponseEntity<byte[]> response = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("failure".getBytes(StandardCharsets.UTF_8));

        assertThrows(ApiCallException.class, () -> apiClient.exposeParseFileResponse(response));
    }

    @Test
    void getStreamBeEndpointUrl_shouldUseDefaultApplicationUrl() {
        String url = AbstractApiClient.getStreamBeEndpointUrl("model-55");

        assertEquals("/api/model/download/model-55", url);
    }

    @Test
    void crudMethodsShouldDelegateToExpectedEndpoints() throws Exception {
        DelegatingApiClient apiClient = new DelegatingApiClient(objectMapper);
        QuickModelEntity entity = QuickModelEntity.builder().id("model-1").name("Femur").build();
        FilterParameters<ModelFilter> filterParameters = new FilterParameters<>();
        filterParameters.setPageRequest(PageRequest.of(1, 5, Sort.Direction.ASC, "name"));
        filterParameters.setFilter(ModelFilter.builder().SearchText("atlas").build());

        assertEquals("created-id", apiClient.create(entity).getId());
        assertEquals("http://localhost:8050/api/test/create", apiClient.lastUrl);

        assertEquals("read-1", apiClient.read("read-1").getId());
        assertEquals("http://localhost:8050/api/test/read-1", apiClient.lastUrl);

        assertEquals("quick-1", apiClient.readQuick("quick-1").getId());
        assertEquals("http://localhost:8050/api/test/quick-1/quick", apiClient.lastUrl);

        assertEquals("updated-id", apiClient.update("entity-9", entity).getId());
        assertEquals("http://localhost:8050/api/test/update", apiClient.lastUrl);

        assertTrue(apiClient.delete("entity-8"));
        assertEquals("http://localhost:8050/api/test/entity-8/delete", apiClient.lastUrl);

        PageResult<QuickModelEntity> page = apiClient.readEntities(filterParameters);
        assertEquals(1L, page.total());
        assertTrue(apiClient.lastUrl.contains("list?limit=5&page=1&orderBy=name&sortDirection=ASC"));
        assertTrue(apiClient.lastUrl.contains("SearchText=atlas"));
    }

    @Test
    void transportMethodsShouldUseRestClientChain() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestBodyUriSpec postSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestBodyUriSpec putSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(postSpec);
        when(postSpec.uri(anyString())).thenReturn(postSpec);
        when(postSpec.headers(any())).thenReturn(postSpec);
        when(postSpec.body(any(Object.class))).thenReturn(postSpec);
        when(postSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(QuickModelEntity.class)).thenReturn(QuickModelEntity.builder().id("created").name("Femur").build());

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("""
                {"elements":[{"id":"one","name":"Femur"}],"total":1,"page":0}
                """));

        when(restClient.put()).thenReturn(putSpec);
        when(putSpec.uri(anyString())).thenReturn(putSpec);
        when(putSpec.headers(any())).thenReturn(putSpec);
        when(putSpec.body(any(Object.class))).thenReturn(putSpec);
        when(putSpec.retrieve()).thenReturn(responseSpec);

        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenReturn(deleteSpec);
        when(deleteSpec.headers(any())).thenReturn(deleteSpec);
        when(deleteSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenReturn(ResponseEntity.noContent().build());

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);
        QuickModelEntity entity = QuickModelEntity.builder().id("entity-1").name("Entity").build();
        FilterParameters<ModelFilter> filterParameters = new FilterParameters<>();
        filterParameters.setPageRequest(PageRequest.of(0, 2, Sort.Direction.ASC, "id"));
        filterParameters.setFilter(ModelFilter.builder().SearchText("knee").build());

        assertEquals("created", apiClient.create(entity).getId());
        assertEquals("created", apiClient.read("read-id").getId());
        assertEquals("created", apiClient.readQuick("quick-id").getId());
        assertEquals("created", apiClient.update("read-id", entity).getId());
        assertTrue(apiClient.delete("delete-id"));
        assertEquals(1L, apiClient.readEntities(filterParameters).total());

        verify(postSpec).uri("http://localhost:8050/api/test/create");
        verify(getSpec).uri("http://localhost:8050/api/test/read-id");
        verify(getSpec).uri("http://localhost:8050/api/test/quick-id/quick");
        verify(putSpec).uri("http://localhost:8050/api/test/update");
        verify(deleteSpec).uri("http://localhost:8050/api/test/delete-id/delete");
    }

    @Test
    void sendGetRequestShouldWrapHttpStatusExceptions() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(QuickModelEntity.class)).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        ));

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);

        assertThrows(ApiCallException.class, () -> apiClient.read("entity-err"));
    }

    @Test
    void sendGetRequestRawShouldWrapHttpStatusExceptions() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenReturn(getSpec);
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        ));

        TokenTransportApiClient apiClient = new TokenTransportApiClient(restClient, objectMapper);

        assertThrows(ApiCallException.class,
                () -> apiClient.exposeSendGetRequestRaw("http://localhost:8050/api/test/list", String.class, "error", "entity-1", true));
    }

    @Test
    void transportRequestsShouldAddBearerTokenWhenAvailable() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenAnswer(invocation -> {
            HttpHeaders headers = new HttpHeaders();
            invocation.<java.util.function.Consumer<HttpHeaders>>getArgument(0).accept(headers);
            capturedHeaders.set(headers);
            return getSpec;
        });
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(QuickModelEntity.class)).thenReturn(QuickModelEntity.builder().id("token").build());

        TokenTransportApiClient apiClient = new TokenTransportApiClient(restClient, objectMapper);

        assertEquals("token", apiClient.read("entity-1").getId());
        assertEquals("Bearer jwt-token", capturedHeaders.get().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void deleteShouldWrapUnexpectedTransportErrors() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenThrow(new RuntimeException("network down"));

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);

        assertThrows(Exception.class, () -> apiClient.delete("broken"));
    }

    @Test
    void parameterUrlBuilderShouldAppendParametersToExistingQuery() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);

        String url = apiClient.exposeParameterUrlBuilder("http://localhost:8050/api/test/path?existing=true", "mode", "full", "lang", "cs");

        assertEquals("http://localhost:8050/api/test/path?existing=true&mode=full&lang=cs", url);
    }

    @Test
    void parameterUrlBuilderShouldReturnOriginalUrlWhenParamsMissing() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);

        assertEquals("http://localhost:8050/api/test/path", apiClient.exposeParameterUrlBuilder("http://localhost:8050/api/test/path"));
    }

    @Test
    void parameterUrlBuilderShouldIgnoreDanglingParamName() throws Exception {
        TestApiClient apiClient = new TestApiClient(objectMapper);

        String url = apiClient.exposeParameterUrlBuilder("http://localhost:8050/api/test/path", "mode", "full", "dangling");

        assertEquals("http://localhost:8050/api/test/path?mode=full", url);
    }

    @Test
    void parameterUrlBuilderShouldWrapEncodingFailures() {
        TestApiClient apiClient = new TestApiClient(objectMapper);

        InvocationTargetException thrown = assertThrows(
                InvocationTargetException.class,
                () -> apiClient.exposeParameterUrlBuilder("http://localhost:8050/api/test/path", null, "full")
        );

        assertInstanceOf(ApiCallException.class, thrown.getCause());
    }

    @Test
    void sendPostRequestShouldWrapUnexpectedTransportErrors() {
        RestClient restClient = mock(RestClient.class);
        when(restClient.post()).thenThrow(new RuntimeException("down"));

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);
        QuickModelEntity entity = QuickModelEntity.builder().id("entity-1").name("Entity").build();

        Exception thrown = assertThrows(Exception.class, () -> apiClient.create(entity));
        assertTrue(thrown.getMessage().contains("Neočekávaná chyba při volání API"));
    }

    @Test
    void sendPutRequestShouldWrapUnexpectedTransportErrors() {
        RestClient restClient = mock(RestClient.class);
        when(restClient.put()).thenThrow(new RuntimeException("down"));

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);
        QuickModelEntity entity = QuickModelEntity.builder().id("entity-1").name("Entity").build();

        Exception thrown = assertThrows(Exception.class, () -> apiClient.update("entity-1", entity));
        assertTrue(thrown.getMessage().contains("Neočekávaná chyba při volání API"));
    }

    @Test
    void deleteShouldWrapHttpStatusExceptions() {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec deleteSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(restClient.delete()).thenReturn(deleteSpec);
        when(deleteSpec.uri(anyString())).thenReturn(deleteSpec);
        when(deleteSpec.headers(any())).thenReturn(deleteSpec);
        when(deleteSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toBodilessEntity()).thenThrow(HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST, "bad", HttpHeaders.EMPTY, "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8
        ));

        TransportApiClient apiClient = new TransportApiClient(restClient, objectMapper);

        assertThrows(ApiCallException.class, () -> apiClient.delete("broken"));
    }

    @Test
    void sendGetRequestRawShouldUseAcceptHeaderWhenRequested() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenAnswer(invocation -> {
            HttpHeaders headers = new HttpHeaders();
            invocation.<java.util.function.Consumer<HttpHeaders>>getArgument(0).accept(headers);
            capturedHeaders.set(headers);
            return getSpec;
        });
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("ok"));

        TokenTransportApiClient apiClient = new TokenTransportApiClient(restClient, objectMapper);

        apiClient.exposeSendGetRequestRaw("http://localhost:8050/api/test/list", String.class, "error", "entity-1", true);

        assertEquals(MediaType.APPLICATION_JSON, capturedHeaders.get().getAccept().getFirst());
        assertEquals("Bearer jwt-token", capturedHeaders.get().getFirst(HttpHeaders.AUTHORIZATION));
    }

    @Test
    void sendGetRequestRawShouldUseOnlyAuthorizationWhenAcceptHeadersNotRequested() throws Exception {
        RestClient restClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec getSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        AtomicReference<HttpHeaders> capturedHeaders = new AtomicReference<>();

        when(restClient.get()).thenReturn(getSpec);
        when(getSpec.uri(anyString())).thenReturn(getSpec);
        when(getSpec.headers(any())).thenAnswer(invocation -> {
            HttpHeaders headers = new HttpHeaders();
            invocation.<java.util.function.Consumer<HttpHeaders>>getArgument(0).accept(headers);
            capturedHeaders.set(headers);
            return getSpec;
        });
        when(getSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(ResponseEntity.ok("ok"));

        TokenTransportApiClient apiClient = new TokenTransportApiClient(restClient, objectMapper);

        apiClient.exposeSendGetRequestRaw("http://localhost:8050/api/test/list", String.class, "error", "entity-1", false);

        assertTrue(capturedHeaders.get().getAccept().isEmpty());
        assertEquals("Bearer jwt-token", capturedHeaders.get().getFirst(HttpHeaders.AUTHORIZATION));
    }

    private static final class TestApiClient extends AbstractApiClient<QuickModelEntity, QuickModelEntity, ModelFilter> {
        private TestApiClient(ObjectMapper objectMapper) {
            super(mock(RestClient.class), objectMapper, "test/");
        }

        @Override
        protected Class<QuickModelEntity> getEntityClass() {
            return QuickModelEntity.class;
        }

        @Override
        protected Class<QuickModelEntity> getQuicEntityClass() {
            return QuickModelEntity.class;
        }

        private String exposeFilterToQueryParams(ModelFilter filter) {
            return filterToQueryParams(filter);
        }

        private String exposePageRequestToQueryParams(FilterParameters<ModelFilter> filterParameters, String customBaseUrl) {
            return pageRequestToQueryParams(filterParameters, customBaseUrl);
        }

        private PageResult<QuickModelEntity> exposeParseResponse(ResponseEntity<String> response, JavaType type) throws Exception {
            return parseResponse(response, type, "error", null);
        }

        private InputStreamMultipartFile exposeParseFileResponse(ResponseEntity<byte[]> response) throws Exception {
            return parseFileResponse(response, "error", null);
        }

        @Override
        public String getJwtToken() {
            return null;
        }

        private String exposeParameterUrlBuilder(String url, String... params) throws Exception {
            Method method = AbstractApiClient.class.getDeclaredMethod("parameterUrlBuilder", String.class, String[].class);
            method.setAccessible(true);
            return (String) method.invoke(this, url, params);
        }
    }

    private static final class DelegatingApiClient extends AbstractApiClient<QuickModelEntity, QuickModelEntity, ModelFilter> {
        private String lastUrl;

        private DelegatingApiClient(ObjectMapper objectMapper) {
            super(mock(RestClient.class), objectMapper, "test/");
        }

        @Override
        protected Class<QuickModelEntity> getEntityClass() {
            return QuickModelEntity.class;
        }

        @Override
        protected Class<QuickModelEntity> getQuicEntityClass() {
            return QuickModelEntity.class;
        }

        @Override
        protected <R> R sendPostRequest(String url, Object body, Class<R> responseType, String errorMessage, String entityId, HttpHeaders headers) {
            lastUrl = url;
            return responseType.cast(QuickModelEntity.builder().id("created-id").build());
        }

        @Override
        protected <R> R sendGetRequest(String url, Class<R> responseType, String errorMessage, String entityId, String... params) {
            lastUrl = url;
            return responseType.cast(QuickModelEntity.builder().id(entityId).build());
        }

        @Override
        protected <R> ResponseEntity<R> sendGetRequestRaw(String url, Class<R> responseType, String errorMessage, String entityId, boolean includeHeaders) {
            lastUrl = url;
            @SuppressWarnings("unchecked")
            ResponseEntity<R> response = (ResponseEntity<R>) ResponseEntity.ok("""
                    {"elements":[{"id":"page-1","name":"Paged"}],"total":1,"page":1}
                    """);
            return response;
        }

        @Override
        public QuickModelEntity update(String id, QuickModelEntity entity) {
            lastUrl = baseUrl + "update";
            return QuickModelEntity.builder().id("updated-id").build();
        }

        @Override
        public boolean delete(String id) {
            lastUrl = baseUrl + id + "/delete";
            return true;
        }

        @Override
        public String getJwtToken() {
            return null;
        }
    }

    private static class TransportApiClient extends AbstractApiClient<QuickModelEntity, QuickModelEntity, ModelFilter> {
        private TransportApiClient(RestClient restClient, ObjectMapper objectMapper) {
            super(restClient, objectMapper, "test/");
        }

        @Override
        protected Class<QuickModelEntity> getEntityClass() {
            return QuickModelEntity.class;
        }

        @Override
        protected Class<QuickModelEntity> getQuicEntityClass() {
            return QuickModelEntity.class;
        }

        @Override
        public String getJwtToken() {
            return null;
        }
    }

    private static final class TokenTransportApiClient extends TransportApiClient {
        private TokenTransportApiClient(RestClient restClient, ObjectMapper objectMapper) {
            super(restClient, objectMapper);
        }

        @Override
        public String getJwtToken() {
            return "jwt-token";
        }

        private <R> ResponseEntity<R> exposeSendGetRequestRaw(String url, Class<R> responseType, String errorMessage, String entityId, boolean includeHeaders) throws Exception {
            return sendGetRequestRaw(url, responseType, errorMessage, entityId, includeHeaders);
        }
    }
}

