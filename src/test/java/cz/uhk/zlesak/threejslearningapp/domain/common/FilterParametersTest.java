package cz.uhk.zlesak.threejslearningapp.domain.common;

import cz.uhk.zlesak.threejslearningapp.components.inputs.FilterComponent;
import cz.uhk.zlesak.threejslearningapp.domain.model.ModelFilter;
import cz.uhk.zlesak.threejslearningapp.events.threejs.SearchEvent;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class FilterParametersTest {

    @Test
    void setFilteredParameters_shouldResetPaginationAndApplyFilter() {
        FilterParameters<ModelFilter> parameters = new FilterParameters<>();
        ModelFilter filter = ModelFilter.builder().SearchText("atlas").build();
        SearchEvent event = new SearchEvent("atlas", Sort.Direction.DESC, "created", mock(FilterComponent.class));

        parameters.setFilteredParameters(event, filter);

        assertEquals(0, parameters.getPageRequest().getPageNumber());
        assertEquals(10, parameters.getPageRequest().getPageSize());
        assertEquals(Sort.Direction.DESC, parameters.getPageRequest().getSort().iterator().next().getDirection());
        assertEquals("created", parameters.getPageRequest().getSort().iterator().next().getProperty());
        assertSame(filter, parameters.getFilter());
    }

    @Test
    void setPageNumber_shouldKeepPageSizeAndSorting() {
        FilterParameters<ModelFilter> parameters = FilterParameters.<ModelFilter>builder()
                .pageRequest(org.springframework.data.domain.PageRequest.of(1, 20, Sort.Direction.ASC, "name"))
                .build();

        parameters.setPageNumber(4);

        assertEquals(4, parameters.getPageRequest().getPageNumber());
        assertEquals(20, parameters.getPageRequest().getPageSize());
        assertEquals(Sort.Direction.ASC, parameters.getPageRequest().getSort().iterator().next().getDirection());
        assertEquals("name", parameters.getPageRequest().getSort().iterator().next().getProperty());
    }
}
