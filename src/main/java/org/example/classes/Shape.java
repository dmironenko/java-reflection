package org.example.classes;

public sealed interface Shape permits Polygon, Triangle {
    double getArea();

    double getPerimeter();
}
