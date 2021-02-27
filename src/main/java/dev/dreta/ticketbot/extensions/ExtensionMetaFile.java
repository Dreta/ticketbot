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

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * ExtensionMetaFile contains the metadata for all
 * extensions. For details, see docs of individual
 * methods.
 */
@AllArgsConstructor
public class ExtensionMetaFile {
    private static final Gson gson = new Gson();
    private final JsonObject config;

    /**
     * Get the ID of this extension.
     * This ID must be unique across all installed
     * extensions.
     * This ID will be used to uniquely identify this
     * extension.
     *
     * @return -
     */
    @NotNull
    public String getID() {
        return config.get("id").getAsString();
    }

    /**
     * Get the name of this extension.
     *
     * @return -
     */
    @NotNull
    public String getName() {
        return config.get("name").getAsString();
    }

    /**
     * Get the description of this extension.
     *
     * @return -
     */
    @NotNull
    public String getDescription() {
        return config.get("description").getAsString();
    }

    /**
     * Get the version of this extension.
     *
     * @return -
     */
    @NotNull
    public String getVersion() {
        return config.get("version").getAsString();
    }

    /**
     * Get the authors of this extension.
     * Returns an empty array if the authors aren't specified.
     *
     * @return -
     */
    @NotNull
    public String[] getAuthors() {
        if (!config.has("authors")) {
            return new String[0];
        }
        return gson.fromJson(config.getAsJsonArray("authors"), String[].class);
    }

    /**
     * Get the dependencies of this extension.
     * The dependencies will load before this extension loads
     * and an error will be thrown if the dependency isn't present.
     * Returns an empty array if no dependencies are specified.
     *
     * @return -
     */
    @NotNull
    public String[] dependencies() {
        if (!config.has("dependencies")) {
            return new String[0];
        }
        return gson.fromJson(config.getAsJsonArray("dependencies"), String[].class);
    }

    /**
     * Get the URL of this extension, because why not?
     *
     * @return -
     */
    @Nullable
    public String getURL() {
        return config.get("url").getAsString();
    }
}
