package org.example.generator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Stream;

final class ClassFinder {
    private static final String CLASS_SUFFIX = ".class";

    private ClassFinder() {}

    @SuppressWarnings("PMD.UseProperClassLoader")
    static List<Class<?>> find(Class<?> type) {
        ClassLoader loader = type.getClassLoader();
        if (loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }

        String pkg = type.getPackageName();
        List<Class<?>> res = new ArrayList<>();

        try {
            Enumeration<URL> urls = loader.getResources(pkg.replace('.', '/'));

            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                if (!url.getProtocol().equals("file")) {
                    continue;
                }

                Path root = Path.of(url.toURI());
                try (Stream<Path> files = Files.walk(root)) {
                    for (Path file :
                            files.filter(Files::isRegularFile)
                                    .filter(path -> path.toString().endsWith(CLASS_SUFFIX))
                                    .toList()) {
                        String name =
                                root.relativize(file)
                                        .toString()
                                        .replace('/', '.')
                                        .replace('\\', '.');
                        name =
                                (pkg.isEmpty() ? "" : pkg + ".")
                                        + name.substring(0, name.length() - CLASS_SUFFIX.length());

                        try {
                            Class<?> cls = Class.forName(name, false, loader);
                            if (type.isAssignableFrom(cls)
                                    && !cls.isInterface()
                                    && !Modifier.isAbstract(cls.getModifiers())
                                    && !res.contains(cls)) {
                                res.add(cls);
                            }
                        } catch (ClassNotFoundException | LinkageError e) {
                            continue;
                        }
                    }
                }
            }
        } catch (IOException | UncheckedIOException | URISyntaxException e) {
            throw new IllegalStateException(
                    "failed to search for implementations of " + type.getName(), e);
        }

        res.sort(Comparator.comparing(Class::getName));
        return res;
    }
}
