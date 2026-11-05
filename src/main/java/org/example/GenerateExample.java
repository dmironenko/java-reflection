package org.example;

import org.example.classes.Example;
import org.example.generator.Generator;

public class GenerateExample {
    public static void main(String[] args) {
        Generator generator = new Generator();

        Example example = generator.generateValueOfType(Example.class);

        System.out.println(example);
    }
}
