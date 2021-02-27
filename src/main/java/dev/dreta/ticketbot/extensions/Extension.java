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

import com.google.gson.JsonParser;
import dev.dreta.ticketbot.TicketBot;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public abstract class Extension {
    @Getter
    private final File dataDir;
    @Getter
    private final boolean enabled;
    @Getter
    private final ExtensionMetaFile meta;
    @Getter
    @Setter
    private File file;

    protected Extension() throws IOException {
        meta = new ExtensionMetaFile(
                JsonParser.parseString(
                        new String(
                                getClass().getResourceAsStream("/extension.json").readAllBytes(),
                                StandardCharsets.UTF_8
                        )
                ).getAsJsonObject()
        );
        dataDir = new File(TicketBot.extLoader.getExtensionsDir(), meta.getID());
        enabled = false;
    }

    /**
     * The onEnable method will be called when this
     * extension is being enabled.
     * <p>
     * You can register custom step types and add your
     * own listeners in this method.
     */
    public void onEnable() {
    }

    /**
     * The onDisable method will be called when this
     * extension is being disabled.
     * <p>
     * You might want to display a farewell message
     * here?
     */
    public void onDisable() {
    }

    /**
     * Convenient method for getting the class loader
     * of this extension.
     * <p>
     * (Who would need this?)
     *
     * @return -
     */
    public ExtensionClassLoader getClassLoader() {
        return (ExtensionClassLoader) getClass().getClassLoader();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getName() + " (Extension " + meta.getID() + ")";
    }
}
