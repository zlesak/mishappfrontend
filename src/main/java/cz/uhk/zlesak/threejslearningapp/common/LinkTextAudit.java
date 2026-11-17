package cz.uhk.zlesak.threejslearningapp.common;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Finds links whose text does not say where they lead.
 *
 * <p>"ZDE" is the classic example. A screen reader user often navigates by pulling up a list of the
 * links on a page, stripped of the sentences around them; a page whose links all read "zde" gives them
 * a list of identical entries. WCAG 2.4.4 asks for link text that identifies its destination.
 *
 * <p>Chapter text is written in Editor.js and stored as content, so this cannot be fixed in code — it
 * is the author's to fix. What code can do is say so at the moment of saving.
 */
public final class LinkTextAudit {

    private static final Pattern ANCHOR = Pattern.compile("<a\\b[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TAG = Pattern.compile("<[^>]+>");

    /** Words that describe the act of clicking rather than the destination. */
    private static final Set<String> UNHELPFUL = Set.of(
            "zde", "tady", "sem", "odkaz", "odkaz zde", "klikněte zde", "klikni zde", "klikněte sem",
            "více", "více informací", "další", "here", "click here", "link", "more", "read more"
    );

    private LinkTextAudit() {
    }

    /**
     * @param content chapter content as stored by the editor, may be {@code null}
     * @return the offending link texts in the order they appear, without duplicates
     */
    public static Set<String> nonDescriptiveLinkTexts(String content) {
        Set<String> found = new LinkedHashSet<>();
        if (content == null || content.isBlank()) {
            return found;
        }

        Matcher matcher = ANCHOR.matcher(content);
        while (matcher.find()) {
            String text = plainText(matcher.group(1));
            if (text.isEmpty()) {
                continue;
            }
            String normalized = text.toLowerCase(Locale.forLanguageTag("cs"))
                    .replaceAll("[\\s\\u00a0]+", " ")
                    .replaceAll("[.!?:,…]+$", "")
                    .trim();
            if (UNHELPFUL.contains(normalized)) {
                found.add(text);
            }
        }
        return found;
    }

    /**
     * Strips markup and the escaping the editor applies, so the comparison sees what the reader sees.
     *
     * @param html the raw contents of an anchor
     * @return the visible text
     */
    private static String plainText(String html) {
        return TAG.matcher(html).replaceAll("")
                .replace("&nbsp;", " ")
                .replace("\\\"", "\"")
                .replace("&amp;", "&")
                .trim();
    }
}
