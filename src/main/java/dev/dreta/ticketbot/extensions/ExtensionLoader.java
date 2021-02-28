/*
 * Ticket Bot allows you to easily manage and track tickets.
 * Copyright (C) 2021 Dreta
 *
 * Ticket Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Ticket Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Ticket Bot.  If not, see <https://www.gnu.org/licenses/>.
 */

package dev.dreta.ticketbot.extensions;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class is a TODO.
 *
 * This class will be able to load all the extensions
 * from the specified extensions directory.
 */
@AllArgsConstructor
public class ExtensionLoader {
    @Getter
    private final File extensionsDir;
    @Getter
    private final Set<ExtensionClassLoader> loaders = new HashSet<>();

    public File[] getAvailableExtensions() {
        return extensionsDir.listFiles(file -> file.getName().endsWith(".jar") && !file.isHidden() && file.isFile());
    }

    public Set<Extension> getExtensions() {
        return loaders.stream().map(ExtensionClassLoader::getExtension).collect(Collectors.toSet());
    }

    public Extension getExtensionFromClass(Class<?> clazz) {
        if (clazz.getClassLoader() instanceof ExtensionClassLoader) {
            return ((ExtensionClassLoader) clazz.getClassLoader()).getExtension();
        }
        return null;
    }

    public Extension getExtension(String id) {
        for (ExtensionClassLoader loader : loaders) {
            if (id.equalsIgnoreCase(loader.getExtension().getMeta().getID())) {
                return loader.getExtension();
            }
        }
        return null;
    }

    public ExtensionClassLoader getOrCreateLoader(File file) throws IOException, InvocationTargetException, InstantiationException, IllegalAccessException {
        ExtensionClassLoader loader = null;
        for (ExtensionClassLoader ldr : loaders) {
            if (ldr.getUrl().equals(file.toURI().toURL())) {
                loader = ldr;
            }
        }

        if (loader == null) {
            loader = new ExtensionClassLoader(file, this);
        }
        return loader;
    }

    public void enableExtension(ExtensionClassLoader loader) {
        try {
            System.out.println("Loading " + loader.getExtension().getMeta().getID());
            loader.getExtension().setEnabled(true);
            loader.getExtension().onEnable();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to enable extension " + loader.getExtension().getMeta().getID(), ex);
        }
    }

    public void disableExtension(Extension extension) {
        try {
            extension.setEnabled(false);
            extension.onDisable();
        } catch (Exception ex) {
            throw new RuntimeException("Unable to disable extension " + extension.getMeta().getID(), ex);
        }

        // Unload the classes
        ExtensionClassLoader loader = extension.getClassLoader();
        loaders.remove(loader);
        for (ExtensionClassLoader ldr : loaders) {
            // Cleanup the caches of the other extensions to
            // ensure no one can access classes of disabled
            // extensions.
            Set<String> toRemove = new HashSet<>();
            for (Map.Entry<String, Class<?>> entry : ldr.getClasses().entrySet()) {
                if (entry.getValue().getClassLoader() == loader) {
                    toRemove.add(entry.getKey());
                }
            }

            for (String s : toRemove) {
                ldr.getClasses().remove(s);
            }
        }
    }
}
