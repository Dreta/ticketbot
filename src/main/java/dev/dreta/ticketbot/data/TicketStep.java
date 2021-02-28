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

package dev.dreta.ticketbot.data;

import com.google.gson.JsonObject;
import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.extensions.ExtensionClassLoader;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * A ticket step represents a configurable step for a
 * {@link TicketType}.
 * <p>
 * In TicketBot, the guild owner can customize several
 * {@link TicketType}s. For example, a Discord server for an
 * art commission team might have 3 types of tickets:
 * 1. Art commission
 * 2. Report
 * 3. Support request
 * <p>
 * Each of these {@link TicketType}s may have different
 * steps for the ticket. For example, an art commission
 * {@link TicketType} may contain details on the art the
 * guild member needs, a budget and a deadline, while a
 * report may contain the specific artist to report and
 * the details about the report.
 * <p>
 * Note that a {@link TicketStep} is NOT a {@link TicketStepData}.
 * The {@link TicketStepData} is always specific to a ticket,
 * while a {@link TicketStep} is specific to a {@link TicketType}.
 */
@Data
@AllArgsConstructor
public class TicketStep<T> {
    private String title;
    private String description;
    private Class<TicketStepType<T>> type;
    /**
     * For the specific options that are allowed in each
     * {@link TicketStepType}, you might want to consult
     * its documentation.
     */
    private JsonObject options;

    /**
     * This method attempts to deserialize a TicketStep from a
     * JSON object.
     * <p>
     * NOTE: This method effectively ignores typing because it
     * is very hard for Java to understand that the type
     * is always right.
     *
     * @param j The JsonObject to deserialize from
     * @return -
     */
    public static TicketStep<?> deserialize(JsonObject j) {
        try {
            return new TicketStep<>(j.get("title").getAsString(), j.get("description").getAsString(),
                    (Class<TicketStepType<Object>>) Class.forName(j.get("type").getAsString()),
                    j.getAsJsonObject("options"));
        } catch (ClassNotFoundException e) {
            // Try to find inside extensions instead
            for (ExtensionClassLoader loader : TicketBot.extLoader.getLoaders()) {
                try {
                    return new TicketStep<>(j.get("title").getAsString(), j.get("description").getAsString(),
                            (Class<TicketStepType<Object>>) loader.findClass(j.get("type").getAsString()),
                            j.getAsJsonObject("options"));
                } catch (ClassNotFoundException ignored) {
                }
            }
            throw new IllegalArgumentException("Couldn't find step type of " + j.get("type").getAsString() + ".");
        }
    }

    public JsonObject serialize() {
        JsonObject j = new JsonObject();
        j.addProperty("title", title);
        j.addProperty("description", description);
        j.addProperty("type", type.getName());
        j.add("options", options);
        return j;
    }
}
