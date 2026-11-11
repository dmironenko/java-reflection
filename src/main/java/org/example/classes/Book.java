package org.example.classes;

import java.util.List;

public class Book {
    private final String title;
    private final List<Color> tags;

    public Book(String title, List<Color> tags) {
        this.title = title;
        this.tags = tags;
    }

    public String getTitle() {
        return title;
    }

    public List<Color> getTags() {
        return tags;
    }
}
