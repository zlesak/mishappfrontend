package cz.uhk.zlesak.threejslearningapp.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.uhk.zlesak.threejslearningapp.api.clients.DocumentationApiClient;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntry;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationEntryIndex;
import cz.uhk.zlesak.threejslearningapp.domain.documentation.DocumentationFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service layer for managing application documentation.
 * Handles loading, role-based filtering, and persistence of documentation entries.
 */
@Slf4j
@Service
public class DocumentationService extends AbstractService<DocumentationEntry, DocumentationEntry, DocumentationFilter> {
    private final ObjectMapper objectMapper;
    private List<DocumentationEntry> cachedEntries = null;

    @Value("${documentation.storage.path:documentation-data}")
    private String storagePath;

    private static final String DEFAULT_DOC_PATH = "/documentation_cs.json";
    private static final String CLASSPATH_PATTERN = "classpath*:texts/doc/documentation*.json";

    @Autowired
    public DocumentationService(DocumentationApiClient documentationApiClient, ObjectMapper objectMapper) {
        super(documentationApiClient);
        this.objectMapper = objectMapper;
    }

    /**
     * Loads all documentation entries from classpath resources and external storage.
     * Results are cached in memory to optimize performance.
     *
     * @return a list of all loaded documentation entries.
     */
    private List<DocumentationEntry> loadEntries() {
        if (cachedEntries != null) return cachedEntries;

        Map<String, DocumentationEntry> result = new LinkedHashMap<>();
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(getClass().getClassLoader());
            Resource[] resources = resolver.getResources(CLASSPATH_PATTERN);

            for (Resource res : resources) {
                readAndMergeResource(res, result);
            }

            loadFromExternalStorage(result);

            cachedEntries = new ArrayList<>(result.values());
            return cachedEntries;
        } catch (Exception e) {
            log.error("Failed to load documentation entries: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Scans the external storage directory for JSON documentation files.
     *
     * @param target the map where loaded entries will be merged.
     */
    private void loadFromExternalStorage(Map<String, DocumentationEntry> target) {
        try {
            Path dir = Paths.get(storagePath);
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                try (Stream<Path> files = Files.list(dir)) {
                    files
                            .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                            .forEach(p -> {
                                try {
                                    String json = Files.readString(p, StandardCharsets.UTF_8);
                                    readAndMergeJsonString(json, target, p.getFileName().toString());
                                } catch (Exception ex) {
                                    log.warn("Failed to read external file {}: {}", p, ex.getMessage());
                                }
                            });
                }
            } else {
                Files.createDirectories(dir);
            }
        } catch (Exception e) {
            log.warn("External storage access error: {}", e.getMessage());
        }
    }

    /**
     * Reads a specific resource and merges its content into the target map.
     *
     * @param res    the resource to read.
     * @param target the destination map for entries.
     */
    private void readAndMergeResource(Resource res, Map<String, DocumentationEntry> target) {
        try (InputStream is = res.getInputStream()) {
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            readAndMergeJsonString(json, target, res.getFilename());
        } catch (Exception ex) {
            log.warn("Failed to read resource {}: {}", res.getFilename(), ex.getMessage());
        }
    }

    /**
     * Parses a JSON string representing either a list of entries or a single entry.
     *
     * @param json       the JSON raw content.
     * @param target     the destination map.
     * @param sourceName name of the source for logging purposes.
     */
    private void readAndMergeJsonString(String json, Map<String, DocumentationEntry> target, String sourceName) {
        try {
            List<DocumentationEntry> list = objectMapper.readValue(json, new TypeReference<>() {
            });
            if (list != null) {
                list.stream()
                        .filter(Objects::nonNull)
                        .filter(e -> e.getId() != null)
                        .forEach(e -> target.put(e.getId(), e));
                return;
            }
        } catch (Exception ignored) {
        }

        try {
            DocumentationEntry single = objectMapper.readValue(json, DocumentationEntry.class);
            if (single != null && single.getId() != null) {
                target.put(single.getId(), single);
            }
        } catch (Exception ex) {
            log.warn("JSON parsing failed for {}: {}", sourceName, ex.getMessage());
        }
    }

    /**
     * Invalidates the in-memory cache and reloads data from sources.
     */
    public void refresh() {
        this.cachedEntries = null;
        loadEntries();
    }

    /**
     * Retrieves the roles of the currently authenticated user.
     *
     * @return a list of role strings (e.g., ROLE_USER).
     */
    private List<String> currentUserRoles() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) return List.of();
            List<String> roles = auth.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList());
            return new ArrayList<>(normalizeRoles(roles));
        } catch (Exception e) {
            return List.of();
        }
    }

    private Set<String> normalizeRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }

        Set<String> normalized = new HashSet<>();
        for (String role : roles) {
            if (role == null || role.isBlank()) {
                continue;
            }
            String trimmed = role.trim();
            normalized.add(trimmed);

            if (!trimmed.startsWith("ROLE_")) {
                normalized.add("ROLE_" + trimmed.toUpperCase(Locale.ROOT));
            }

            if ("ROLE_ADMINISTRATOR".equals(trimmed)) {
                normalized.add("ROLE_ADMIN");
            }
            if ("ROLE_ADMIN".equals(trimmed)) {
                normalized.add("ROLE_ADMINISTRATOR");
            }
        }
        return normalized;
    }

    private boolean allowedForRoles(DocumentationEntry entry, List<String> roles) {
        if (entry == null) {
            return false;
        }
        if (entry.getRoles() == null || entry.getRoles().isEmpty()) {
            return true;
        }
        Set<String> normalizedUserRoles = normalizeRoles(roles);
        if (normalizedUserRoles.isEmpty()) {
            return false;
        }

        Set<String> normalizedEntryRoles = normalizeRoles(entry.getRoles());
        return normalizedEntryRoles.stream().anyMatch(normalizedUserRoles::contains);
    }

    /**
     * Returns all documentation entries accessible to the current user.
     *
     * @return a filtered list of documentation entries.
     */
    public List<DocumentationEntry> getEntries() {
        return getEntriesForRoles(currentUserRoles());
    }

    /**
     * Returns documentation entries accessible to the specified roles.
     *
     * @param roles list of role strings to filter by.
     * @return filtered list of documentation entries.
     */
    public List<DocumentationEntry> getEntriesForRoles(List<String> roles) {
        return loadEntries().stream()
                .filter(entry -> allowedForRoles(entry, roles))
                .collect(Collectors.toList());
    }

    /**
     * Returns all documentation entries without role filtering.
     * Intended for admin edit/save flows to avoid destructive overwrites.
     */
    public List<DocumentationEntry> getAllEntriesForSave() {
        return new ArrayList<>(loadEntries());
    }

    /**
     * Returns lightweight index entries for list rendering (without content payload).
     */
    public List<DocumentationEntryIndex> getEntryIndexes() {
        return getEntryIndexesForRoles(currentUserRoles());
    }

    /**
     * Returns lightweight index entries accessible to the specified roles.
     *
     * @param roles list of role strings to filter by.
     * @return filtered list of index entries.
     */
    public List<DocumentationEntryIndex> getEntryIndexesForRoles(List<String> roles) {
        return loadEntries().stream()
                .filter(entry -> allowedForRoles(entry, roles))
                .map(DocumentationEntry::toIndex)
                .collect(Collectors.toList());
    }

    /**
     * Filters accessible entries by their type.
     *
     * @param type the entry type (e.g., 'chapter', 'model').
     * @return a list of matching entries.
     */
    public List<DocumentationEntry> getEntriesByType(String type) {
        return getEntriesByTypeForRoles(type, currentUserRoles());
    }

    /**
     * Returns documentation entries of a given type accessible to the specified roles.
     *
     * @param type  entry type to filter by.
     * @param roles list of role strings to filter by.
     * @return filtered list of documentation entries.
     */
    public List<DocumentationEntry> getEntriesByTypeForRoles(String type, List<String> roles) {
        if (type == null) return List.of();
        return getEntriesForRoles(roles).stream()
                .filter(e -> type.equalsIgnoreCase(e.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all entries of a given type without role filtering.
     */
    public List<DocumentationEntry> getAllEntriesByTypeForSave(String type) {
        if (type == null) {
            return List.of();
        }
        return loadEntries().stream()
                .filter(e -> type.equalsIgnoreCase(e.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Returns filtered lightweight index entries by type.
     */
    public List<DocumentationEntryIndex> getEntryIndexesByType(String type) {
        return getEntryIndexesByTypeForRoles(type, currentUserRoles());
    }

    /**
     * Returns lightweight index entries of a given type accessible to the specified roles.
     *
     * @param type  entry type to filter by.
     * @param roles list of role strings to filter by.
     * @return filtered list of index entries.
     */
    public List<DocumentationEntryIndex> getEntryIndexesByTypeForRoles(String type, List<String> roles) {
        if (type == null) {
            return List.of();
        }

        return getEntryIndexesForRoles(roles).stream()
                .filter(e -> type.equalsIgnoreCase(e.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Resolves a full documentation entry by ID for the current user.
     */
    public DocumentationEntry getEntryDetail(String id) {
        return getEntryDetailForRoles(id, currentUserRoles());
    }

    /**
     * Returns a documentation entry by ID accessible to the specified roles.
     *
     * @param id    entry ID to look up.
     * @param roles list of role strings to filter by.
     * @return matching entry, or {@code null} if not found or not accessible.
     */
    public DocumentationEntry getEntryDetailForRoles(String id, List<String> roles) {
        if (id == null || id.isBlank()) {
            return null;
        }

        return loadEntries().stream()
                .filter(entry -> id.equals(entry.getId()))
                .filter(entry -> allowedForRoles(entry, roles))
                .findFirst()
                .orElse(null);
    }

    /**
     * Persists a complete list of documentation entries.
     * Primary target is the project resource directory (development mode).
     * If inaccessible, the list is written to the configured external storage path.
     *
     * @param entries the list of entries to be saved.
     * @throws RuntimeException if the save operation fails critically.
     */
    public void saveAll(List<DocumentationEntry> entries) {
        try {
            String jsonOutput = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(entries);

            Path targetPath = Paths.get(storagePath+DEFAULT_DOC_PATH);

            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }

            try {
                Files.writeString(targetPath, jsonOutput, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.warn("Resource path not writable, saving to external storage path instead.");
                Path fallbackPath = Paths.get(storagePath).resolve("documentation_cs.json");
                Files.writeString(fallbackPath, jsonOutput, StandardCharsets.UTF_8);
            }

            this.refresh();
        } catch (Exception e) {
            log.error("Critical error while saving documentation: {}", e.getMessage());
            throw new RuntimeException("Documentation save failure", e);
        }
    }
    /**
     * Validates the create entity.
     *
     * @param createEntity Entity to validate
     * @return Validated entity
     * @throws RuntimeException if validation fails
     */
    @Override
    protected DocumentationEntry validateCreateEntity(DocumentationEntry createEntity) throws RuntimeException {
        throw new NotImplementedException("Creation of documentation entries via API is not supported. Use saveAll() method instead.");
    }

    /**
     * Creates the final entity from the create entity.
     *
     * @param createEntity Entity to create
     * @return Final entity
     * @throws RuntimeException if creation fails
     */
    @Override
    protected DocumentationEntry createFinalEntity(DocumentationEntry createEntity) throws RuntimeException {
        throw new NotImplementedException("Creation of documentation entries via API is not supported.");
    }
}