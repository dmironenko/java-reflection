package org.example.classes;

import java.util.List;
import java.util.Map;

public class Catalog {
    private final Map<Boolean, List<Book>> entries;

    public Catalog(Map<Boolean, List<Book>> entries) {
        this.entries = entries;
    }

    public Map<Boolean, List<Book>> getEntries() {
        return entries;
    }
}
