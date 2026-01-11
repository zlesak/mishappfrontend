package cz.uhk.zlesak.threejslearningapp.common;

import java.util.LinkedHashMap;
import java.util.function.BiConsumer;

/**
 * ObservableMap Class - A LinkedHashMap that notifies a callback on changes
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
public class ObservableMap<K, V> extends LinkedHashMap<K, V> {
    private final BiConsumer<V, Boolean> onChange;

    /**
     * Constructor for ObservableMap.
     *
     * @param onChange the callback to be invoked on changes, receiving the value and a boolean indicating if the change is from client
     */
    public ObservableMap(BiConsumer<V, Boolean> onChange) {
        this.onChange = onChange;
    }

    /**
     * Put method with change notification.
     *
     * @param key        the key with which the specified value is to be associated
     * @param value      the value to be associated with the specified key
     * @param fromClient indicates if the change is from client
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    public V putExtended(K key, V value, Boolean fromClient) {
        V v = super.put(key, value);
        onChange.accept(value, fromClient);
        return v;
    }

    /**
     * Put method without change notification.
     *
     * @param key   the key with which the specified value is to be associated
     * @param value the value to be associated with the specified key
     */
    public void putMultiple(K key, V value) {
        super.put(key, value);
    }

    /**
     * Notify change method.
     *
     * @param value      the value that changed
     * @param fromClient indicates if the change is from client
     */
    public void notifyChange(V value, Boolean fromClient) {
        onChange.accept(value, fromClient);
    }

    /**
     * Remove method with change notification.
     *
     * @param key key whose mapping is to be removed from the map
     * @return the previous value associated with key, or null if there was no mapping for key
     */
    @Override
    public V remove(Object key) {
        V v = super.remove(key);
        onChange.accept(v, true);
        return v;
    }

    /**
     * Clear method with change notification.
     */
    @Override
    public void clear() {
        super.clear();
        onChange.accept(null, true);
    }
}


