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

package dev.dreta.ticketbot.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.dreta.ticketbot.TicketBot;
import lombok.Getter;
import lombok.Setter;

import java.io.*;

/**
 * This is a utility class that allows you to easily
 * create custom configuration files and access them.
 */
public class Configuration {
    private static final File LOCAL = new File(".");
    @Getter
    @Setter
    private File file;
    @Getter
    private JsonObject config;

    /**
     * Save the configuration.
     */
    public void save() {
        try {
            if (!this.file.exists()) {
                if (!this.file.createNewFile()) {
                    throw new IOException("Failed to create configuration file");
                }
                BufferedWriter writer = new BufferedWriter(new FileWriter(this.file));
                writer.write(config.toString());
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reload the configuration.
     */
    public void reload() {
        try {
            if (!this.file.exists()) {
                throw new IOException("Could not find configuration file");
            }
            BufferedReader reader = new BufferedReader(new FileReader(this.file));
            StringBuilder result = new StringBuilder();
            String s;
            while ((s = reader.readLine()) != null) {
                result.append(s);
            }
            this.config = (JsonObject) JsonParser.parseString(result.toString());
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Read string from an InputStream.
     *
     * @param stream The stream to read from
     * @return The contents of the stream.
     */
    public String readInputStream(InputStream stream) {
        StringBuilder builder = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        String resp;
        try {
            while ((resp = reader.readLine()) != null) {
                builder.append(resp);
                builder.append("\n");
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    /**
     * Load this configuration from a file.
     *
     * @param from The file to load from
     */
    public void load(String from) {
        load(from, TicketBot.class, LOCAL);
    }

    /**
     * Load this configuration from a file.
     *
     * @param from       The file to load from
     * @param clazz      The class to discover to resource in
     * @param dataFolder The data folder to put the configuration to
     */
    public void load(String from, Class<?> clazz, File dataFolder) {
        try {
            if (!dataFolder.exists()) dataFolder.mkdirs();
            this.file = new File(dataFolder, from);
            if (!this.file.exists()) {
                if (!this.file.createNewFile()) throw new IOException("File creation failed");
                BufferedWriter writer = new BufferedWriter(new FileWriter(this.file));
                writer.write(readInputStream(clazz.getResourceAsStream("/" + from)));
                writer.flush();
                writer.close();
            }
            reload();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

