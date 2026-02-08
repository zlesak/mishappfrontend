package cz.uhk.zlesak.threejslearningapp.domain.common;

import java.util.List;

/**
 * PageResult is a generic record that holds paginated results.
 * It contains a list of elements, the total number of elements, and the current page number
 *
 * @param elements list of elements of type T
 * @param total    total number of elements available
 * @param page     current page number
 * @param <T>      the type of elements in the list
 */
public record PageResult<T>(List<T> elements, Long total, Integer page) {
}
