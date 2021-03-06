package dev.dreta.ticketbot.extensions;

import com.google.common.io.ByteStreams;
import dev.dreta.ticketbot.TicketBot;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Custom class loader to allow sharing classes
 * between extensions.
 */
public class ExtensionClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    @Getter
    private final Map<String, Class<?>> classes = new ConcurrentHashMap<>(); // This also acts as a cache for classes in other extensions
    @Getter
    private final Extension extension;
    private final JarFile jar;
    private final Manifest manifest;
    @Getter
    private final URL url;
    @Getter
    private final File file;
    @Getter
    private final Class<? extends Extension> mainClass;
    @Getter
    private final ExtensionLoader loader;

    public ExtensionClassLoader(File file, ExtensionLoader loader) throws IOException, IllegalAccessException, InvocationTargetException, InstantiationException {
        super(new URL[]{file.toURI().toURL()}, TicketBot.class.getClassLoader());

        jar = new JarFile(file);
        manifest = jar.getManifest();
        url = file.toURI().toURL();
        this.file = file;
        this.loader = loader;

        String mainClassName = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (mainClassName == null) {
            throw new IllegalStateException("You must specify the main class of your extension in the manifest!");
        }
        try {
            Class<?> clazz = findClass(mainClassName);
            mainClass = clazz.asSubclass(Extension.class);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Main class \"" + mainClassName + "\" could not be found.");
        } catch (ClassCastException ex) {
            throw new IllegalStateException("Main class \"" + mainClassName + "\" must be a subclass of " + Extension.class.getName() + ".");
        }

        try {
            extension = mainClass.getConstructor().newInstance();
            extension.setFile(file);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Main class \"" + mainClassName + "\" must contain a public constructor without arguments.");
        }

        loader.getLoaders().add(this);
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        if (name.startsWith("dev.dreta.ticketbot.")) {
            // Load a built-in class
            return Class.forName(name);
        }

        Class<?> result = classes.get(name);
        if (result == null) {
            // This class isn't available in our own extension. Let's check other extensions:
            for (ExtensionClassLoader loader : loader.getLoaders()) {
                if (loader != this && loader.getExtension() != null && loader.getExtension().isEnabled() && loader.getClasses().containsKey(name)) {
                    result = loader.getClasses().get(name);
                }
            }

            if (result == null) {
                // This class isn't loaded yet. We will load it in our component now.
                String path = name.replace('.', '/') + ".class";
                JarEntry entry = jar.getJarEntry(path);

                if (entry != null) {
                    // Define the class.
                    byte[] classBytes;

                    try (InputStream is = jar.getInputStream(entry)) {
                        classBytes = ByteStreams.toByteArray(is);
                    } catch (IOException ex) {
                        throw new ClassNotFoundException(name, ex);
                    }

                    int dot = name.lastIndexOf('.');
                    if (dot != -1) {
                        String pkgName = name.substring(0, dot);
                        if (getDefinedPackage(pkgName) == null) {
                            try {
                                if (manifest != null) {
                                    definePackage(pkgName, manifest, url);
                                } else {
                                    definePackage(pkgName, null, null, null, null, null, null, null);
                                }
                            } catch (IllegalArgumentException ex) {
                                if (getDefinedPackage(pkgName) == null) {
                                    throw new IllegalStateException("Cannot find package " + pkgName);
                                }
                            }
                        }
                    }

                    result = defineClass(name, classBytes, 0, classBytes.length, new CodeSource(url, entry.getCodeSigners()));
                }

                if (result == null) {
                    result = super.findClass(name);
                }
            }

            classes.put(name, result);
        }
        return result;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            jar.close();
        }
    }

    @Override
    public String toString() {
        return "ExtensionClassLoader(" + extension.getMeta().getName() + ")";
    }
}
