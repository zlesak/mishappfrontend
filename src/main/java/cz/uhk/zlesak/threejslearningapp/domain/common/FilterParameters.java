package cz.uhk.zlesak.threejslearningapp.domain.common;

import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * FilterParameters Class - Encapsulates pagination and filtering parameters for data retrieval.
 * @param <F> The type of the filter object.
 */
@Data
@AllArgsConstructor
@SuperBuilder
@NoArgsConstructor
@Getter
public class FilterParameters<F> {
    PageRequest pageRequest = PageRequest.of(0, 10, Sort.Direction.ASC, "Name");
    F filter;

    public void setFilteredParameters(SearchEvent searchEvent, F filter) {
        this.pageRequest = PageRequest.of(
                0,
                10,
                searchEvent.getSortDirection(),
                searchEvent.getOrderBy()
        );
        this.filter = filter;
    }

    public void setPageNumber(int pageNumber) {
        this.pageRequest = PageRequest.of(
                pageNumber,
                this.pageRequest.getPageSize(),
                this.pageRequest.getSort().iterator().next().getDirection(),
                this.pageRequest.getSort().iterator().next().getProperty()
        );
    }
}
