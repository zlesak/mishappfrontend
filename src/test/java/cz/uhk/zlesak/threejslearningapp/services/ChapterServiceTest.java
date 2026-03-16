package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.clients.ChapterApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.testsupport.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ChapterServiceTest {
    private ChapterApiClient chapterApiClient;
    private ChapterService chapterService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        chapterApiClient = mock(ChapterApiClient.class);
        objectMapper = new ObjectMapper();
        chapterService = new ChapterService(chapterApiClient, objectMapper);
    }

    @Test
    void create_shouldInjectHeaderModelIdsAndStoreDistinctModels() throws Exception {
        var mainModel = TestFixtures.model("main", "model-main", "Main", null, List.of());
        var subModel = TestFixtures.model("sub", "model-sub", "Sub", null, List.of());
        Map<String, cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity> headerModels = new LinkedHashMap<>();
        headerModels.put("main", mainModel);
        headerModels.put("h1", subModel);
        headerModels.put("h2", subModel);

        ChapterEntity chapter = ChapterEntity.builder()
                .name("Chapter")
                .content("""
                        {"blocks":[
                          {"id":"h1","type":"header","data":{"level":1,"text":"H1"}},
                          {"id":"h2","type":"header","data":{"level":2,"text":"H2"}}
                        ]}
                        """)
                .models(List.of(mainModel))
                .modelHeaderMap(headerModels)
                .build();

        when(chapterApiClient.create(any(ChapterEntity.class))).thenAnswer(invocation -> {
            ChapterEntity entity = invocation.getArgument(0);
            entity.setId("chapter-1");
            return entity;
        });

        String id = chapterService.create(chapter);

        assertEquals("chapter-1", id);
        ArgumentCaptor<ChapterEntity> captor = ArgumentCaptor.forClass(ChapterEntity.class);
        verify(chapterApiClient).create(captor.capture());
        ChapterEntity created = captor.getValue();

        JsonNode blocks = objectMapper.readTree(created.getContent()).get("blocks");
        assertEquals("model-sub", blocks.get(0).get("data").get("modelId").asText());
        assertEquals("model-sub", blocks.get(1).get("data").get("modelId").asText());
        assertEquals(2, created.getModels().size());
        assertEquals("model-main", created.getModels().getFirst().getModel().getId());
    }

    @Test
    void getSubChaptersNames_shouldExtractOnlyLevelOneHeaders() throws Exception {
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Chapter")
                .content("""
                        {"blocks":[
                          {"type":"header","data":{"level":1,"text":"Intro"}},
                          {"id":"h2","type":"header","data":{"level":2,"text":"Nested"}},
                          {"id":"h3","type":"header","data":{"level":1,"text":"Body","modelId":"model-3"}}
                        ]}
                        """)
                .models(List.of())
                .build();

        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        var result = chapterService.getSubChaptersNames("chapter-1");

        assertEquals(2, result.size());
        assertTrue(result.getFirst().id().startsWith("fallback-"));
        assertEquals("", result.getFirst().modelId());
        assertEquals("h3", result.get(1).id());
        assertEquals("model-3", result.get(1).modelId());
    }

    @Test
    void create_shouldThrowWhenNameIsMissing() {
        ChapterEntity invalid = ChapterEntity.builder()
                .name("")
                .content("{\"blocks\":[]}")
                .models(List.of())
                .build();

        RuntimeException ex = assertThrows(RuntimeException.class, () -> chapterService.create(invalid));
        assertFalse(ex.getMessage().isBlank());
    }

    @Test
    void processHeaders_shouldGroupLevelTwoHeadersUnderLevelOne() throws Exception {
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Chapter")
                .content("""
                        {"blocks":[
                          {"id":"h1","type":"header","data":{"level":1,"text":"<b>Intro</b>","modelId":"model-1"}},
                          {"id":"h2","type":"header","data":{"level":2,"text":"Nested A"}},
                          {"id":"h3","type":"header","data":{"level":2,"text":"Nested B"}},
                          {"id":"h4","type":"header","data":{"level":1,"text":"Body"}}
                        ]}
                        """)
                .models(List.of())
                .build();
        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        var result = chapterService.processHeaders("chapter-1");

        assertEquals(2, result.size());
        var first = result.entrySet().iterator().next();
        assertEquals("h1", first.getKey().getLeft());
        assertEquals("Intro", first.getKey().getMiddle());
        assertEquals("model-1", first.getKey().getRight());
        assertEquals(2, first.getValue().size());
        assertEquals("h2", first.getValue().getFirst()._1());
    }

    @Test
    void getChaptersModels_shouldMapMainAndSubchapterModels() throws Exception {
        var mainModel = TestFixtures.model("main", "model-main", "Main", null, List.of());
        var subModel = TestFixtures.model("sub", "model-sub", "Sub", null, List.of());
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Chapter")
                .content("""
                        {"blocks":[
                          {"id":"sub-1","type":"header","data":{"level":1,"text":"Intro","modelId":"model-sub"}}
                        ]}
                        """)
                .models(List.of(mainModel, subModel))
                .build();
        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        var result = chapterService.getChaptersModels("chapter-1");

        assertEquals(2, result.size());
        assertEquals("model-main", result.get("main").getModel().getId());
        assertEquals("model-sub", result.get("sub-1").getModel().getId());
    }

    @Test
    void getChapterNameAndContent_shouldReuseLoadedEntity() throws Exception {
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Kosti")
                .content("{\"blocks\":[]}")
                .models(List.of())
                .build();
        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        assertEquals("Kosti", chapterService.getChapterName("chapter-1"));
        assertEquals("{\"blocks\":[]}", chapterService.getChapterContent("chapter-1"));
    }

    @Test
    void saveChapter_shouldCreateAndUpdateUsingPreparedEntity() throws Exception {
        var mainModel = TestFixtures.model("main", "model-main", "Main", null, List.of());
        Map<String, cz.uhk.zlesak.threejslearningapp.domain.model.QuickModelEntity> allModels = Map.of("main", mainModel);
        ChapterEntity loadedChapter = ChapterEntity.builder()
                .id("chapter-1")
                .name("Loaded")
                .description("desc")
                .creatorId("teacher")
                .content("{\"blocks\":[{\"id\":\"main\",\"type\":\"header\",\"data\":{\"level\":1,\"text\":\"Main\"}}]}")
                .models(List.of(mainModel))
                .quizzes(List.of())
                .build();

        when(chapterApiClient.create(any(ChapterEntity.class))).thenReturn(ChapterEntity.builder()
                .id("created-id")
                .name(loadedChapter.getName())
                .description(loadedChapter.getDescription())
                .content(loadedChapter.getContent())
                .models(loadedChapter.getModels())
                .build());
        when(chapterApiClient.update(org.mockito.ArgumentMatchers.eq("chapter-1"), any(ChapterEntity.class))).thenReturn(ChapterEntity.builder()
                .id("chapter-1")
                .name(loadedChapter.getName())
                .description(loadedChapter.getDescription())
                .content(loadedChapter.getContent())
                .models(loadedChapter.getModels())
                .build());

        String created = chapterService.saveChapter(
                null,
                false,
                "Nova kapitola",
                "{\"blocks\":[{\"id\":\"main\",\"type\":\"header\",\"data\":{\"level\":1,\"text\":\"Main\"}}]}",
                allModels,
                null
        );
        String updated = chapterService.saveChapter(
                "chapter-1",
                true,
                "Upravena kapitola",
                "{\"blocks\":[{\"id\":\"main\",\"type\":\"header\",\"data\":{\"level\":1,\"text\":\"Main\"}}]}",
                allModels,
                loadedChapter
        );

        assertEquals("created-id", created);
        assertEquals("chapter-1", updated);
        verify(chapterApiClient).create(any(ChapterEntity.class));
        verify(chapterApiClient).update(org.mockito.ArgumentMatchers.eq("chapter-1"), any(ChapterEntity.class));
    }

    @Test
    void saveChapter_shouldRejectInvalidUpdateState() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> chapterService.saveChapter(
                "chapter-1",
                true,
                "Kapitola",
                "{\"blocks\":[]}",
                Map.of(),
                null
        ));

        assertNotNull(ex.getMessage());
    }

    @Test
    void getSubChaptersNames_shouldWrapInvalidJson() throws Exception {
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Chapter")
                .content("not-json")
                .models(List.of())
                .build();
        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        assertThrows(RuntimeException.class, () -> chapterService.getSubChaptersNames("chapter-1"));
    }

    @Test
    void getChaptersModels_shouldReturnEmptyWhenChapterHasNoModels() throws Exception {
        ChapterEntity loaded = ChapterEntity.builder()
                .id("chapter-1")
                .name("Chapter")
                .content("{\"blocks\":[]}")
                .models(List.of())
                .build();
        when(chapterApiClient.read("chapter-1")).thenReturn(loaded);

        assertTrue(chapterService.getChaptersModels("chapter-1").isEmpty());
    }

    @Test
    void saveChapter_shouldRejectEditWithoutId() {
        RuntimeException ex = assertThrows(RuntimeException.class, () -> chapterService.saveChapter(
                "",
                true,
                "Kapitola",
                "{\"blocks\":[]}",
                Map.of("main", TestFixtures.model("main", "model-main", "Main", null, List.of())),
                ChapterEntity.builder().description("").quizzes(List.of()).subChapters(List.of()).creatorId("teacher").build()
        ));

        assertNotNull(ex.getMessage());
    }
}
