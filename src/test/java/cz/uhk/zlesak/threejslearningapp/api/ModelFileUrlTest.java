package cz.uhk.zlesak.threejslearningapp.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelFileUrlTest {

    @Test
    void buildsTheDownloadUrlForAStoredFile() {
        assertThat(ModelFileUrl.of("6a6b821c95b2ddc6bb4aa8d1"))
                .isEqualTo("/api/model/download/6a6b821c95b2ddc6bb4aa8d1");
    }

    @Test
    void refusesAnIdThatCouldEscapeTheDownloadPath() {
        assertThatThrownBy(() -> ModelFileUrl.of("../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelFileUrl.of("id?x=1"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ModelFileUrl.of(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void readsTheFileIdBackOutOfAUrl() {
        assertThat(ModelFileUrl.fileIdOf("/api/model/download/file-1")).isEqualTo("file-1");
        assertThat(ModelFileUrl.fileIdOf("https://mish/api/model/download/file-1?v=2")).isEqualTo("file-1");
        assertThat(ModelFileUrl.fileIdOf("/api/model/download/file-1/preview")).isEqualTo("file-1");
    }

    @Test
    void reportsNothingForUrlsThatDoNotPointAtAStoredFile() {
        assertThat(ModelFileUrl.fileIdOf("https://example.org/image.png")).isNull();
        assertThat(ModelFileUrl.fileIdOf("/api/model/download/")).isNull();
        assertThat(ModelFileUrl.fileIdOf("  ")).isNull();
        assertThat(ModelFileUrl.fileIdOf(null)).isNull();
    }
}
