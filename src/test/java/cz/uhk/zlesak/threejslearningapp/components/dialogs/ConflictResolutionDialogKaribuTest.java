package cz.uhk.zlesak.threejslearningapp.components.dialogs;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Div;
import cz.uhk.zlesak.threejslearningapp.backend.BackendException;
import cz.uhk.zlesak.threejslearningapp.domain.chapter.ChapterEntity;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ChapterConflictResolution;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictChoice;
import cz.uhk.zlesak.threejslearningapp.services.conflict.ConflictField;
import cz.uhk.zlesak.threejslearningapp.testsupport.KaribuSpringTestSupport;
import cz.uhk.zlesak.threejslearningapp.testsupport.OAuthTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._find;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@Import(OAuthTestConfig.class)
class ConflictResolutionDialogKaribuTest {

    private static final Instant BASE = Instant.parse("2026-11-01T10:00:00Z");
    private static final Instant THEIRS = Instant.parse("2026-11-01T10:05:00Z");
    private static final Instant THIRD = Instant.parse("2026-11-01T10:07:00Z");

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() {
        KaribuSpringTestSupport.setUp(applicationContext);
    }

    @AfterEach
    void tearDown() {
        KaribuSpringTestSupport.tearDown();
    }

    @Test
    void showsBothVersionsOfEachDifferingFieldAndKeepsMineUnlessToldOtherwise() {
        ConflictResolutionDialog dialog = open(fields(), choices -> { }, () -> { });

        assertThat(dialog.getHeaderTitle()).isEqualTo("Kapitolu mezitím uložil někdo jiný");
        assertThat(texts(dialog)).contains("Moje jméno", "Jejich jméno", "Můj text", "Jejich text");
        assertThat(dialog.choices()).containsExactly(
                Map.entry("name", ConflictChoice.MINE), Map.entry("content", ConflictChoice.MINE));
        // Each choice is named after its field, so a screen reader announces what is being chosen.
        assertThat(dialog.choiceFor("name").getLabel()).isEqualTo("Ponechat u položky Název");
    }

    @Test
    void onOpeningFocusGoesToTheFirstChoicesRadioButton() {
        ConflictResolutionDialog dialog = open(fields(), choices -> { }, () -> { });

        // The radio group host cannot take focus in the browser; the script has to reach the
        // checked radio button inside it. Whether focus lands is checked in a real browser (QA).
        var focus = focusCall();
        assertThat(focus.getOwner()).isSameAs(dialog.choiceFor("name").getElement().getNode());
        assertThat(focus.getInvocation().getParameters().getFirst()).isEqualTo(ConflictResolutionDialog.CHOICE_SELECTOR);
        assertThat(ConflictResolutionDialog.CHOICE_SELECTOR).startsWith("vaadin-radio-button[checked]");
        assertThat(ConflictResolutionDialog.FOCUS_JS).contains("querySelector(selector)").contains("setTimeout");
    }

    @Test
    void withNothingToChooseFocusGoesToBackToEditing() {
        ConflictResolutionDialog dialog = open(List.of(), choices -> { }, () -> { });

        var focus = focusCall();
        assertThat(focus.getOwner()).isSameAs(dialog.getBackButton().getElement().getNode());
        assertThat(focus.getInvocation().getParameters().getFirst()).isEqualTo("");
    }

    @Test
    void theTwoVersionsOfAFieldShareTheFullWidth() {
        ConflictResolutionDialog dialog = open(fields(), choices -> { }, () -> { });

        // Shrink-wrapped, a section with short values was narrower than two columns and the
        // versions wrapped below one another even on a wide screen.
        assertThat(_find(dialog, com.vaadin.flow.component.orderedlayout.FlexLayout.class))
                .hasSize(2)
                .allSatisfy(row -> {
                    assertThat(row.getWidth()).isEqualTo("100%");
                    assertThat(row.getParent().orElseThrow().getElement().getStyle().get("width")).isEqualTo("100%");
                });
    }

    private static com.vaadin.flow.component.internal.PendingJavaScriptInvocation focusCall() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
        return UI.getCurrent().getInternals().dumpPendingJavaScriptInvocations().stream()
                .filter(call -> call.getInvocation().getExpression().contains("target.focus()"))
                .findFirst().orElseThrow();
    }

    @Test
    void savingHandsBackTheChoicePerField() {
        AtomicReference<Map<String, ConflictChoice>> saved = new AtomicReference<>();
        ConflictResolutionDialog dialog = open(fields(), saved::set, () -> { });

        dialog.choiceFor("name").setValue(ConflictChoice.THEIRS);
        _click(dialog.getSaveButton());

        assertThat(saved.get()).containsEntry("name", ConflictChoice.THEIRS).containsEntry("content", ConflictChoice.MINE);
        assertThat(dialog.isOpened()).isFalse();
    }

    @Test
    void discardingThrowsTheChangesAwayAndSavesNothing() {
        AtomicInteger discarded = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        ConflictResolutionDialog dialog = open(fields(), choices -> saves.incrementAndGet(), discarded::incrementAndGet);

        _click(dialog.getDiscardButton());

        assertThat(discarded).hasValue(1);
        assertThat(saves).hasValue(0);
    }

    @Test
    void goingBackToEditingNeitherSavesNorDiscards() {
        AtomicInteger discarded = new AtomicInteger();
        AtomicInteger saves = new AtomicInteger();
        ConflictResolutionDialog dialog = open(fields(), choices -> saves.incrementAndGet(), discarded::incrementAndGet);

        _click(dialog.getBackButton());

        assertThat(dialog.isOpened()).isFalse();
        assertThat(discarded).hasValue(0);
        assertThat(saves).hasValue(0);
    }

    @Test
    void aConflictOnTheResolvedSaveOpensTheComparisonAgainAndNeverReportsSuccess() {
        ChapterEntity mine = chapter("Moje jméno", BASE);
        ChapterEntity theirs = chapter("Jejich jméno", THEIRS);
        ChapterEntity third = chapter("Třetí jméno", THIRD);
        List<ChapterEntity> sent = new ArrayList<>();
        List<String> successes = new ArrayList<>();
        List<Throwable> failures = new ArrayList<>();

        Function<ChapterEntity, String> backend = combined -> {
            sent.add(combined);
            if (sent.size() == 1) {
                throw new BackendException.Conflict("konflikt", third).withAttempted(combined);
            }
            return combined.getId();
        };
        ConflictResolutionFlow<ChapterEntity> flow = flow(backend, successes, failures);

        assertThat(flow.handle(new RuntimeException(new BackendException.Conflict("konflikt", theirs).withAttempted(mine))))
                .isTrue();
        _click(flow.getDialog().getSaveButton());

        // The first resolved save was based on the version the dialog showed and was turned away.
        assertThat(sent.getFirst().getUpdated()).isEqualTo(THEIRS);
        assertThat(successes).isEmpty();
        assertThat(failures).isEmpty();
        assertThat(flow.getDialog().isOpened()).isTrue();
        assertThat(texts(flow.getDialog())).contains("Třetí jméno");

        _click(flow.getDialog().getSaveButton());

        assertThat(sent.get(1).getUpdated()).isEqualTo(THIRD);
        assertThat(sent.get(1).getName()).isEqualTo("Moje jméno");
        assertThat(successes).containsExactly("ch-1");
    }

    @Test
    void anyOtherFailureIsLeftToTheView() {
        List<Throwable> failures = new ArrayList<>();
        ConflictResolutionFlow<ChapterEntity> flow = flow(combined -> {
            throw new IllegalStateException("Mongo nedostupná");
        }, new ArrayList<>(), failures);

        assertThat(flow.handle(new IllegalStateException("jiná chyba"))).isFalse();

        flow.handle(new BackendException.Conflict("konflikt", chapter("Jejich", THEIRS))
                .withAttempted(chapter("Moje", BASE)));
        _click(flow.getDialog().getSaveButton());

        assertThat(failures).singleElement().satisfies(error ->
                assertThat(error).hasMessage("Mongo nedostupná"));
    }

    private ConflictResolutionFlow<ChapterEntity> flow(Function<ChapterEntity, String> backend,
                                                       List<String> successes, List<Throwable> failures) {
        return new ConflictResolutionFlow<>(ChapterEntity.class, "conflict.chapter.title",
                new ChapterConflictResolution((key, params) -> key),
                backend,
                (work, onSuccess, onError) -> {
                    String result;
                    try {
                        result = work.get();
                    } catch (RuntimeException e) {
                        onError.accept(e);
                        return;
                    }
                    onSuccess.accept(result);
                },
                successes::add, failures::add, () -> { });
    }

    private static ChapterEntity chapter(String name, Instant updated) {
        return ChapterEntity.builder().id("ch-1").name(name).updated(updated)
                .content("{\"blocks\":[]}").modelIds(List.of("m")).build();
    }

    private static List<ConflictField> fields() {
        return List.of(
                new ConflictField("name", "Název", "Moje jméno", "Jejich jméno"),
                new ConflictField("content", "Obsah", "Můj text", "Jejich text"));
    }

    private ConflictResolutionDialog open(List<ConflictField> fields,
                                          java.util.function.Consumer<Map<String, ConflictChoice>> onSave,
                                          Runnable onDiscard) {
        ConflictResolutionDialog dialog = new ConflictResolutionDialog("conflict.chapter.title", THEIRS, fields, onSave, onDiscard);
        UI.getCurrent().add(dialog);
        dialog.open();
        return dialog;
    }

    private static List<String> texts(ConflictResolutionDialog dialog) {
        return _find(dialog, Div.class).stream().map(Div::getText).toList();
    }
}
