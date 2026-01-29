package cz.uhk.zlesak.threejslearningapp.common;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class for parsing Moodle ZIP exports containing chapters and subchapters.
 * Each ZIP contains an imsmanifest.xml describing the structure and folders with HTML content and images.
 */
@Slf4j
public class MoodleZipParser {

    /**
     * Represents a parsed subchapter from Moodle ZIP
     */
    @Data
    public static class SubChapter {
        private String title;
        private String htmlContent;
        private Map<String, byte[]> images = new HashMap<>();
        private int order;
    }

    /**
     * Represents the complete parsed chapter with all subchapters
     */
    @Data
    public static class ParsedChapter {
        private String chapterTitle;
        private List<SubChapter> subChapters = new ArrayList<>();
        private Map<String, byte[]> allImages = new HashMap<>();
    }

    /**
     * Parse a Moodle ZIP file and extract chapter structure, HTML content, and images
     *
     * @param zipInputStream InputStream of the ZIP file
     * @return ParsedChapter containing all extracted data
     * @throws Exception if parsing fails
     */
    public static ParsedChapter parseZip(InputStream zipInputStream) throws Exception {
        ParsedChapter parsedChapter = new ParsedChapter();
        Map<String, byte[]> zipContents = new HashMap<>();

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        baos.write(buffer, 0, len);
                    }
                    zipContents.put(entry.getName(), baos.toByteArray());
                }
                zis.closeEntry();
            }
        }

        byte[] manifestData = zipContents.get("imsmanifest.xml");
        if (manifestData == null) {
            throw new IllegalArgumentException("imsmanifest.xml not found in ZIP");
        }

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setExpandEntityReferences(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(manifestData));

        NodeList organizations = doc.getElementsByTagName("organization");
        if (organizations.getLength() > 0) {
            Element organization = (Element) organizations.item(0);
            NodeList orgTitles = organization.getElementsByTagName("title");
            if (orgTitles.getLength() > 0) {
                Element titleElement = (Element) orgTitles.item(0);
                String titleText = titleElement.getTextContent();
                if (titleText != null) {
                    parsedChapter.setChapterTitle(titleText.trim());
                }
            }
        }

        NodeList items = doc.getElementsByTagName("item");
        List<SubChapter> subChapters = new ArrayList<>();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);
            String identifierRef = item.getAttribute("identifierref");

            if (identifierRef.isEmpty()) {
                continue;
            }

            SubChapter subChapter = new SubChapter();
            subChapter.setOrder(i);

            NodeList itemChildren = item.getChildNodes();
            for (int j = 0; j < itemChildren.getLength(); j++) {
                if (itemChildren.item(j) instanceof Element child) {
                    if ("title".equals(child.getTagName())) {
                        String titleText = child.getTextContent();
                        if (titleText != null) {
                            subChapter.setTitle(titleText.trim());
                            log.debug("Subchapter {}: {}", i + 1, subChapter.getTitle());
                        }
                        break;
                    }
                }
            }

            NodeList resources = doc.getElementsByTagName("resource");
            for (int j = 0; j < resources.getLength(); j++) {
                Element resource = (Element) resources.item(j);
                if (resource.getAttribute("identifier").equals(identifierRef)) {
                    String xmlBase = resource.getAttribute("xml:base");
                    String href = resource.getAttribute("href");

                    String htmlPath;
                    if (xmlBase.isEmpty()) {
                        htmlPath = href;
                    } else if (xmlBase.endsWith("/")) {
                        htmlPath = xmlBase + href;
                    } else {
                        htmlPath = xmlBase + "/" + href;
                    }

                    byte[] htmlData = zipContents.get(htmlPath);

                    if (htmlData != null) {
                        String htmlContent = new String(htmlData, StandardCharsets.UTF_8);
                        subChapter.setHtmlContent(extractBodyContent(htmlContent));
                    }

                    NodeList files = resource.getElementsByTagName("file");
                    for (int k = 0; k < files.getLength(); k++) {
                        Element fileElement = (Element) files.item(k);
                        String fileHref = fileElement.getAttribute("href");

                        if (isImageFile(fileHref)) {
                            byte[] imageData = zipContents.get(fileHref);
                            if (imageData != null) {
                                String imageName = fileHref.substring(fileHref.lastIndexOf('/') + 1);
                                subChapter.getImages().put(imageName, imageData);
                                parsedChapter.getAllImages().put(imageName, imageData);
                            }
                        }
                    }
                    break;
                }
            }

            if (subChapter.getHtmlContent() != null) {
                subChapters.add(subChapter);
            }
        }

        parsedChapter.setSubChapters(subChapters);

        return parsedChapter;
    }

    /**
     * Extract content from HTML body tag and remove the first H1 heading
     * (since we add chapter title as H1 separately)
     */
    private static String extractBodyContent(String html) {
        int bodyStart = html.indexOf("<body");
        if (bodyStart == -1) return html;

        bodyStart = html.indexOf('>', bodyStart) + 1;
        int bodyEnd = html.indexOf("</body>", bodyStart);

        if (bodyEnd == -1) bodyEnd = html.length();

        String bodyContent = html.substring(bodyStart, bodyEnd).trim();

        bodyContent = bodyContent.replaceFirst("(?i)<h1[^>]*id=['\"]header['\"][^>]*>.*?</h1>\\s*", "");

        return bodyContent;
    }

    /**
     * Check if file is an image based on extension
     */
    private static boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg") ||
                lower.endsWith(".png") || lower.endsWith(".gif") ||
                lower.endsWith(".bmp") || lower.endsWith(".svg");
    }
}

