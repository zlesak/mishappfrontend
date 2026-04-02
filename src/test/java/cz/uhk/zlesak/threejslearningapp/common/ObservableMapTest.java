package cz.uhk.zlesak.threejslearningapp.common;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObservableMapTest {

    @Test
    void putExtended_remove_and_clear_shouldNotifyCallback() {
        List<String> changes = new ArrayList<>();
        ObservableMap<String, String> map = new ObservableMap<>((value, fromClient) ->
                changes.add(value + ":" + fromClient));

        map.putExtended("a", "alpha", false);
        assertEquals("alpha", map.get("a"));
        map.remove("a");
        map.clear();

        assertEquals(List.of("alpha:false", "alpha:true", "null:true"), changes);
    }
}

