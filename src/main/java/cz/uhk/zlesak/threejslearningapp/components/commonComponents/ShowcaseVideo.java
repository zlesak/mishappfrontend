package cz.uhk.zlesak.threejslearningapp.components.commonComponents;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.dom.Element;

/**
 * A short screen recording, played from a video file rather than an animated GIF.
 *
 * <p>The three recordings on the landing page used to be GIFs — thirteen megabytes of them, decoded on
 * the main thread, with no way to pause. As video the same three come to about three megabytes, the
 * browser decodes them on the graphics card, and a video element brings its own controls, so pausing
 * costs nothing to implement.
 *
 * <p>Two formats, not one. H.264 is what every mainstream browser plays, but Chromium builds without
 * proprietary codecs — Linux distribution packages, and the browser this project's tests run in — cannot
 * decode it at all and would show a still poster forever. VP9 in WebM covers those.
 *
 * <p>Nothing is fetched until the recording is scrolled to: {@code preload="none"} plus a poster frame
 * means the page loads a still image, and the sources are filled in only when the element comes into
 * view. Playback is muted and looped, which is what browsers require before they will start a video on
 * their own, and it does not start at all for a visitor who has asked for reduced motion.
 */
@Tag("video")
public class ShowcaseVideo extends Component implements HasSize, HasStyle {

    /**
     * @param baseUrl the recording without its extension; {@code .webm} and {@code .mp4} are both offered
     * @param posterUrl a still frame shown before playback starts
     * @param accessibleLabel what the recording shows
     */
    public ShowcaseVideo(String baseUrl, String posterUrl, String accessibleLabel) {
        getElement().setAttribute("poster", posterUrl);
        getElement().setAttribute("preload", "none");
        getElement().setAttribute("muted", true);
        getElement().setAttribute("loop", true);
        getElement().setAttribute("playsinline", true);
        getElement().setAttribute("controls", true);
        getElement().setAttribute("controlslist", "nodownload noremoteplayback");
        getElement().setAttribute("disablepictureinpicture", true);
        // A recording with no narration and no information beyond what the caption below already says.
        // The label names it; there is nothing further to transcribe.
        getElement().setAttribute("aria-label", accessibleLabel);
        addClassName("main-showcase-video");

        // WebM first: a browser takes the first source it can decode, and the VP9 files are the smaller
        // of the two. The addresses sit in data-src until the observer promotes them.
        getElement().appendChild(
                source(baseUrl + ".webm", "video/webm"),
                source(baseUrl + ".mp4", "video/mp4"));
    }

    private static Element source(String url, String mimeType) {
        Element source = new Element("source");
        source.setAttribute("data-src", url);
        source.setAttribute("type", mimeType);
        return source;
    }
}
