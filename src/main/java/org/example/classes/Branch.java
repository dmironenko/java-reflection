package org.example.classes;

import java.util.List;

public class Branch {
    private final List<Branch> children;

    public Branch(List<Branch> children) {
        this.children = children;
    }

    public List<Branch> getChildren() {
        return children;
    }
}
