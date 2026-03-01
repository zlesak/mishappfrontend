package cz.uhk.zlesak.threejslearningapp.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.List;

/**
 * FileEntityRecursive represents a file entity that can contain related files, allowing for a recursive structure.
 * It includes metadata about the file and a list of related FileEntityRecursive objects.
 * Copies backend file entity structure for recursive deserialization of related files and serves to simple fetching of file metadata without the need for multiple calls.
 */
@Data
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileEntityRecursive {
    String id;
    String name;
    String creatorId;
    String description;
    String contentType;
    Long size;
    FileSenseType senseType;
    String backendEndpoint;
    Instant created;
    Instant updated;

    @JsonProperty("relatedFiles")
    @JsonAlias({"related", "relatedFiles"})
    List<FileEntityRecursive> relatedFiles;
}

