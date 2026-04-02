package cz.uhk.zlesak.threejslearningapp.components.inputs.quizes;

/**
 * Quiz option component used in texture-click questions.
 * Inherits standard option layout from {@link QuestionOption}.
 */
public class TextureQuestionOption extends QuestionOption {

    /**
     * Constructor for QuestionOption.
     *
     * @param index        the index of the option
     * @param labelTextKey the key for the label text
     */
    public TextureQuestionOption(int index, String labelTextKey, String... value) {
        super(index, labelTextKey, value);
    }

}
