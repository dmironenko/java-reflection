package org.example.generator;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.example.classes.BinaryTreeNode;
import org.example.classes.Book;
import org.example.classes.Branch;
import org.example.classes.Cart;
import org.example.classes.Catalog;
import org.example.classes.Color;
import org.example.classes.Example;
import org.example.classes.Polygon;
import org.example.classes.Product;
import org.example.classes.Rectangle;
import org.example.classes.Shape;
import org.example.classes.Triangle;
import org.junit.jupiter.api.Test;

class GeneratorTest {
    private final Generator generator = new Generator(2026L);

    enum Empty {}

    @Test
    void primitivesAndWrappers() {
        Class<?>[] types = {
            boolean.class, Boolean.class, byte.class, Byte.class, short.class, Short.class,
            int.class, Integer.class, long.class, Long.class, float.class, Float.class,
            double.class, Double.class, char.class, Character.class
        };
        Class<?>[] expected = {
            Boolean.class, Boolean.class, Byte.class, Byte.class, Short.class, Short.class,
            Integer.class, Integer.class, Long.class, Long.class, Float.class, Float.class,
            Double.class, Double.class, Character.class, Character.class
        };
        Object[] values = new Object[types.length];

        for (int i = 0; i < types.length; i++) {
            values[i] = generator.generateValueOfType(types[i]);
        }

        for (int i = 0; i < values.length; i++) {
            assertInstanceOf(expected[i], values[i]);
        }
    }

    @Test
    void randomValues() {
        int first = generator.generateValueOfType(Integer.class);
        int second = generator.generateValueOfType(Integer.class);
        String text = generator.generateValueOfType(String.class);
        Color color = generator.generateValueOfType(Color.class);

        assertNotEquals(first, second);
        assertFalse(text.isEmpty());
        assertNotNull(color);
    }

    @Test
    void arrays() {
        int[] numbers = generator.generateValueOfType(int[].class);
        String[] strings = generator.generateValueOfType(String[].class);
        Color[] colors = generator.generateValueOfType(Color[].class);
        Rectangle[] rectangles = generator.generateValueOfType(Rectangle[].class);

        assertTrue(numbers.length > 0);
        assertNotNull(strings[0]);
        assertNotNull(colors[0]);
        assertNotNull(rectangles[0]);
    }

    @Test
    void collections() {
        List<?> list = generator.generateValueOfType(List.class);
        Map<?, ?> map = generator.generateValueOfType(Map.class);

        assertFalse(list.isEmpty());

        for (Object value : list) {
            assertInstanceOf(Integer.class, value);
        }

        assertFalse(map.isEmpty());

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            assertInstanceOf(Integer.class, entry.getKey());
            assertInstanceOf(String.class, entry.getValue());
        }
    }

    @Test
    void objects() {
        Example example = generator.generateValueOfType(Example.class);
        Rectangle rectangle = generator.generateValueOfType(Rectangle.class);
        Triangle triangle = generator.generateValueOfType(Triangle.class);
        Shape shape = generator.generateValueOfType(Shape.class);
        Product product = generator.generateValueOfType(Product.class);
        Cart cart = generator.generateValueOfType(Cart.class);

        assertNotNull(example);
        assertNotNull(rectangle);
        assertNotNull(triangle);
        assertNotNull(shape);
        assertFalse(product.getName().isEmpty());
        assertFalse(cart.getItems().isEmpty());

        for (Product item : cart.getItems()) {
            assertFalse(item.getName().isEmpty());
        }
    }

    @Test
    void typedCollections() {
        Book book = generator.generateValueOfType(Book.class);
        Catalog catalog = generator.generateValueOfType(Catalog.class);

        assertFalse(book.getTitle().isEmpty());
        assertFalse(book.getTags().isEmpty());
        assertFalse(catalog.getEntries().isEmpty());
        assertFalse(catalog.getEntries().values().iterator().next().isEmpty());
    }

    @Test
    void abstractClassAndAnnotation() {
        Polygon polygon = generator.generateValueOfType(Polygon.class);
        Product product = generator.generateValueOfType(Product.class);

        assertInstanceOf(Rectangle.class, polygon);
        assertNotEquals(Double.MIN_VALUE, product.getPrice());
    }

    @Test
    void objectRecursion() {
        BinaryTreeNode root = generator.generateValueOfType(BinaryTreeNode.class);

        assertNotNull(root.getData());
        BinaryTreeNode fifth = root.getLeft().getLeft().getLeft().getLeft();
        assertNull(fifth.getLeft());
        assertNull(fifth.getRight());
    }

    @Test
    void collectionRecursion() {
        Branch root = generator.generateValueOfType(Branch.class);

        Branch current = root;
        for (int i = 1; i < 5; i++) {
            current = current.getChildren().get(0);
            assertNotNull(current);
        }
        assertNull(current.getChildren().get(0));
    }

    @Test
    void sameSeed() {
        Generator first = new Generator(123L);
        Generator second = new Generator(123L);

        String firstText = first.generateValueOfType(String.class);
        int[] firstNumbers = first.generateValueOfType(int[].class);
        String secondText = second.generateValueOfType(String.class);
        int[] secondNumbers = second.generateValueOfType(int[].class);

        assertEquals(firstText, secondText);
        assertArrayEquals(firstNumbers, secondNumbers);
    }

    @Test
    void errors() {
        IllegalArgumentException emptyEnum =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> generator.generateValueOfType(Empty.class));
        IllegalStateException missingImplementation =
                assertThrows(
                        IllegalStateException.class,
                        () -> generator.generateValueOfType(Missing.class));
        IllegalStateException constructorFailure =
                assertThrows(
                        IllegalStateException.class,
                        () -> generator.generateValueOfType(ThrowsException.class));

        assertTrue(emptyEnum.getMessage().contains(Empty.class.getName()));
        assertTrue(missingImplementation.getMessage().contains(Missing.class.getName()));
        assertInstanceOf(IllegalStateException.class, constructorFailure.getCause());
    }

    public interface Missing {}

    public static class ThrowsException {
        public ThrowsException() {
            throw new IllegalStateException("failed");
        }
    }
}
