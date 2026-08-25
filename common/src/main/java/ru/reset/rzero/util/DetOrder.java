package ru.reset.rzero.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

public final class DetOrder {
    private DetOrder() {}

    public static <K, V, C extends Comparable<? super C>> List<Map.Entry<K, V>> sortedEntries(
            Map<K, V> map, Function<? super K, C> keyFn) {
        List<Map.Entry<K, V>> list = new ArrayList<>(map.entrySet());
        list.sort(Comparator.comparing(e -> keyFn.apply(e.getKey())));
        return list;
    }

    public static <V> Iterable<V> commutativeValues(Map<?, V> map) {
        return map.values();
    }

    public static <K, V> Iterable<Map.Entry<K, V>> commutativeEntries(Map<K, V> map) {
        return map.entrySet();
    }

    public static <V> boolean anyValueMatches(Map<?, V> map, Predicate<? super V> pred) {
        for (V v : map.values()) if (pred.test(v)) return true;
        return false;
    }
}
