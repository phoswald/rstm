package com.github.phoswald.rstm.http.metrics;

import java.util.Comparator;
import java.util.List;

class ListComparator<T> implements Comparator<List<T>> {

    private final Comparator<T> elementComparator;

    ListComparator(Comparator<T> elementComparator) {
        this.elementComparator = elementComparator;
    }

    @Override
    public int compare(List<T> o1, List<T> o2) {
        for (int i = 0; i < o1.size() && i < o2.size(); i++) {
            int value = elementComparator.compare(o1.get(i), o2.get(i));
            if (value != 0) {
                return value;
            }
        }
        return Integer.compare(o1.size(), o2.size());
    }
}
