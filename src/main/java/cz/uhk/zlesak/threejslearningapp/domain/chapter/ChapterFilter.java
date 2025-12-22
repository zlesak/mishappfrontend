package cz.uhk.zlesak.threejslearningapp.domain.chapter;

import cz.uhk.zlesak.threejslearningapp.domain.common.FilterBase;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
public class ChapterFilter extends FilterBase {
    String SearchText;
}
