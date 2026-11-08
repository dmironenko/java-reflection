package org.example.generator;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Generator {
    private static final int MAX_COLLECTION_SIZE = 5;
    private static final int MAX_STRING_LENGTH = 8;
    private static final int MAX_REPEATED_CLASS = 5;

    private final Random random;
    private final Map<Class<?>, List<Class<?>>> implementations = new HashMap<>();

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

        if (!(type instanceof Class<?> cls)) {
            throw new IllegalArgumentException("unsupported type: " + type.getTypeName());
        }

        if (cls == void.class || cls == Void.class) {
            throw new IllegalArgumentException("cannot generate " + cls.getName());
        }

        if (cls == byte.class || cls == Byte.class) return (byte) random.nextInt();
        if (cls == short.class || cls == Short.class) return (short) random.nextInt();
        if (cls == int.class || cls == Integer.class) return random.nextInt();
        if (cls == long.class || cls == Long.class) return random.nextLong();
        if (cls == float.class || cls == Float.class) return random.nextFloat();
        if (cls == double.class || cls == Double.class) return random.nextDouble();

        if (cls == boolean.class || cls == Boolean.class) return random.nextBoolean();

        if (cls == char.class || cls == Character.class) return randomChar();
        if (cls == String.class) return generateString();

        if (cls.isEnum()) {
            Object[] values = cls.getEnumConstants();

            if (values.length == 0) {
                throw new IllegalArgumentException("enum has no constants: " + cls.getName());
            }

            return values[random.nextInt(values.length)];
        }

        if (cls.isArray()) return generateArray(cls.getComponentType(), path);
        if (cls == List.class) return generateList(Integer.class, path);
        if (cls == Map.class) return generateMap(Integer.class, String.class, path);

        return generateObject(cls, path);
    }

    private char randomChar() {
        return (char) random.nextInt('a', 'z' + 1);
    }

    private String generateString() {
        int length = random.nextInt(1, MAX_STRING_LENGTH + 1);
        StringBuilder res = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            res.append(randomChar());
        }

        return res.toString();
    }

    private Object generateArray(Class<?> component, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        Object res = Array.newInstance(component, size);

        for (int i = 0; i < size; i++) {
            Array.set(res, i, generate(component, path));
        }

        return res;
    }

    private List<Object> generateList(Type elementType, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        List<Object> res = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            res.add(generate(elementType, path));
        }

        return res;
    }

    private Map<Object, Object> generateMap(Type keyType, Type valueType, List<Class<?>> path) {
        int size = random.nextInt(1, MAX_COLLECTION_SIZE + 1);
        Map<Object, Object> res = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            Object key = generate(keyType, path);
            Object value = generate(valueType, path);

            res.put(key, value);
        }

        return res;
    }

    private Object generateObject(Class<?> type, List<Class<?>> path) {
        if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
            List<Class<?>> classes = implementations.computeIfAbsent(type, ClassFinder::find);

            List<Class<?>> choices = new ArrayList<>(classes);
            Exception error = null;
            while (!choices.isEmpty()) {
                Class<?> cls = choices.remove(random.nextInt(choices.size()));

                try {
                    return generateObject(cls, path);
                } catch (IllegalArgumentException | IllegalStateException e) {
                    error = e;
                }
            }

            throw new IllegalStateException("no implementation for " + type.getName(), error);
        }

        int count = 0;
        for (Class<?> visitedType : path) {
            if (visitedType.equals(type)) {
                count++;
            }
        }
        if (count >= MAX_REPEATED_CLASS) {
            return null;
        }

        List<Constructor<?>> constructors = chooseConstructors(type);
        path.add(type);

        try {
            Throwable error = null;

            for (Constructor<?> constructor : constructors) {
                try {
                    Type[] parameterTypes = constructor.getGenericParameterTypes();
                    Object[] arguments = new Object[parameterTypes.length];

                    for (int i = 0; i < parameterTypes.length; i++) {
                        arguments[i] = generate(parameterTypes[i], path);
                    }

                    return constructor.newInstance(arguments);
                } catch (ReflectiveOperationException
                        | IllegalArgumentException
                        | IllegalStateException e) {
                    error = e;
                }
            }

            throw new IllegalStateException("cannot create " + type.getName(), error);
        } finally {
            path.remove(path.size() - 1);
        }
    }

    private List<Constructor<?>> chooseConstructors(Class<?> type) {
        Constructor<?>[] constructors = type.getConstructors();

        Constructor<?> marked = null;
        for (Constructor<?> constructor : constructors) {
            if (!constructor.isAnnotationPresent(Generatable.class)) {
                continue;
            }

            if (marked != null) {
                throw new IllegalStateException(
                        ">1 @Generatable constructors in " + type.getName());
            }

            marked = constructor;
        }
        if (marked != null) {
            return List.of(marked);
        }

        Arrays.sort(constructors, Comparator.comparingInt(Constructor::getParameterCount));

        return Arrays.asList(constructors);
    }
}
