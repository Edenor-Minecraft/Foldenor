package com.github.cao.awa.sinuatum.util.collection;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CollectionFactor {
    public static <K, V> HashMap<K, V> hashMap() {
        return new HashMap<>();
    }

    public static <K, V> HashMap<K, V> hashMap(Map<K, V> map) {
        return new HashMap<>(map);
    }

    public static <K, V> LinkedHashMap<K, V> linkedHashMap() {
        return new LinkedHashMap<>();
    }

    public static <K, V> LinkedHashMap<K, V> linkedHashMap(Map<K, V> map) {
        return new LinkedHashMap<>(map);
    }

    public static <K, V> ConcurrentHashMap<K, V> concurrentHashMap() {
        return new ConcurrentHashMap<>();
    }

    public static <V> List<V> syncList() {
        return Collections.synchronizedList(arrayList());
    }

    public static <V> List<V> syncList(V... values) {
        return Collections.synchronizedList(arrayList(values));
    }

    public static <V> List<V> syncList(int capacity) {
        return Collections.synchronizedList(arrayList(capacity));
    }

    public static <V> List<V> syncList(Collection<V> delegate) {
        return Collections.synchronizedList(arrayList(delegate));
    }

    public static <V> ArrayList<V> arrayList() {
        return new ArrayList<>();
    }

    public static <V> ArrayList<V> arrayList(V... values) {
        return new ArrayList<>(List.of(values));
    }

    public static <V> ArrayList<V> arrayList(int capacity) {
        return new ArrayList<>(capacity);
    }

    public static <V> ArrayList<V> arrayList(Collection<V> delegate) {
        return new ArrayList<>(delegate);
    }

    public static <V> LinkedList<V> linkedList() {
        return new LinkedList<>();
    }

    public static <V> LinkedList<V> linkedList(V...values) {
        return new LinkedList<>(List.of(values));
    }

    public static <V> LinkedList<V> linkedList(Collection<V> delegate) {
        return new LinkedList<>(delegate);
    }

    public static <V> HashSet<V> hashSet() {
        return new HashSet<>();
        //         return new ObjectOpenHashSet<>();
    }

    public static <V> HashSet<V> hashSet(V...values) {
        return new HashSet<>(List.of(values));
    }

    public static <V> HashSet<V> hashSet(Collection<V> delegate) {
        return new HashSet<>(delegate);
    }
}