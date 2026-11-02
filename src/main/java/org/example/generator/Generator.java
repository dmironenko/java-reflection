package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Generator {
    private static final int MAX_COLLECTION_SIZE = 3;
    private static final int MAX_STRING_LENGTH = 8;
    private static final int MAX_REPEATED_CLASS = 3;

    private final Random random;

    public Generator() {
        random = new Random();
    }

    public Generator(long seed) {
        random = new Random(seed);
    }

    @SuppressWarnings("unchecked")
    public <T> T generateValueOfType(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("requested type must not be null");
        }

        return (T) generate(type, new ArrayList<>());
    }

    private Object generate(Type type, List<Class<?>> path) {
        if (type instanceof ParameterizedType parameterized) {
            Type[] arguments = parameterized.getActualTypeArguments();

            if (parameterized.getRawType() == List.class) {
                return generateList(arguments[0], path);
            }

            if (parameterized.getRawType() == Map.class) {
                return generateMap(arguments[0], arguments[1], path);
            }

            throw new IllegalArgumentException("unsupported type: " + type.getTypeName());
        }

        if (!(type instanceof Class<?> clazz)) {
            throw new IllegalArgumentException("unsupported type: " + type.getTypeName());
        }

        if (clazz == void.class || clazz == Void.class) {
            throw new IllegalArgumentException("cannot generate " + clazz.getName());
        }

        if (clazz == byte.class || clazz == Byte.class) return (byte) random.nextInt();
        if (clazz == short.class || clazz == Short.class) return (short) random.nextInt();
        if (clazz == int.class || clazz == Integer.class) return random.nextInt();
        if (clazz == long.class || clazz == Long.class) return random.nextLong();
        if (clazz == float.class || clazz == Float.class) return random.nextFloat();
        if (clazz == double.class || clazz == Double.class) return random.nextDouble();

        if (clazz == boolean.class || clazz == Boolean.class) return random.nextBoolean();

        if (clazz == char.class || clazz == Character.class) return randomChar();
        if (clazz == String.class) return generateString();

        if (clazz.isEnum()) {
            Object[] values = clazz.getEnumConstants();

            if (values.length == 0) {
                throw new IllegalArgumentException("enum has no constants: " + clazz.getName());
            }

            return values[random.nextInt(values.length)];
        }

        if (clazz.isArray()) return generateArray(clazz.getComponentType(), path);
        if (clazz == List.class) return generateList(Integer.class, path);
        if (clazz == Map.class) return generateMap(Integer.class, String.class, path);

        return generateObject(clazz, path);
    }

    private char randomChar() {
        return (char) random.nextInt('a', 'z' + 1);
    }

    private String generateString() {
        int length = random.nextInt(1, MAX_STRING_LENGTH + 1);
        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(randomChar());
        }

        return result.toString();
    }

    private Object generateArray(Class<?> component, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        Object result = Array.newInstance(component, size);

        for (int i = 0; i < size; i++) {
            Array.set(result, i, generate(component, path));
        }

        return result;
    }

    private List<Object> generateList(Type elementType, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        List<Object> result = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            result.add(generate(elementType, path));
        }

        return result;
    }

    private Map<Object, Object> generateMap(Type keyType, Type valueType, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        Map<Object, Object> result = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            Object key = generate(keyType, path);
            Object value = generate(valueType, path);

            result.put(key, value);
        }

        return result;
    }

    private Object generateObject(Class<?> type, List<Class<?>> path) {
        while (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            Class<?>[] implementations = type.getPermittedSubclasses();

            if (implementations == null || implementations.length == 0) {
                throw new IllegalStateException(
                        "no permitted implementations for " + type.getName());
            }

            type = implementations[random.nextInt(implementations.length)];
        }

        if (Collections.frequency(path, type) >= MAX_REPEATED_CLASS) {
            return null;
        }

        Constructor<?> constructor = chooseConstructor(type);
        path.add(type);

        try {
            Type[] parameterTypes = constructor.getGenericParameterTypes();
            Object[] arguments = new Object[parameterTypes.length];

            for (int i = 0; i < parameterTypes.length; i++) {
                arguments[i] = generate(parameterTypes[i], path);
            }

            return constructor.newInstance(arguments);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                    "constructor failed for " + type.getName(), e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("cannot create " + type.getName(), e);
        } finally {
            path.remove(path.size() - 1);
        }
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private Constructor<?> chooseConstructor(Class<?> type) {
        Constructor<?>[] constructors = type.getConstructors();

        if (constructors.length == 1) return constructors[0];

        Constructor<?> selected = null;
        for (Constructor<?> constructor : constructors) {
            if (!constructor.isAnnotationPresent(Generatable.class)) continue;

            if (selected != null) {
                throw new IllegalStateException(
                        "more than one @Generatable constructor in " + type.getName());
            }

            selected = constructor;
        }

        if (selected == null) {
            throw new IllegalStateException("no suitable constructor: " + type.getName());
        }

        return selected;
    }
}
