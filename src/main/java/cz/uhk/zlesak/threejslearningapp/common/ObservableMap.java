package cz.uhk.zlesak.threejslearningapp.common;

import java.util.HashMap;
import java.util.function.BiConsumer;

public class ObservableMap<K, V> extends HashMap<K, V> {
    private final BiConsumer<V, Boolean> onChange;

    public ObservableMap(BiConsumer<V, Boolean> onChange) {
        this.onChange = onChange;
    }

    public V putExtended(K key, V value, Boolean fromClient) {
        V v = super.put(key, value);
        onChange.accept(value, fromClient);
        return v;
    }

    public void putMultiple(K key, V value) {
        super.put(key, value);
    }

    public void notifyChange(V value, Boolean fromClient) {
        onChange.accept(value, fromClient);
    }

    @Override
    public V remove(Object key) {
        V v = super.remove(key);
        onChange.accept(v, true);
        return v;
    }

    @Override
    public void clear() {
        super.clear();
        onChange.accept(null, true);
    }
}


