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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A {@link TicketType} represents a configurable
 * possible type for a {@link Ticket}, including
 * different {@link TicketStep}s etc.
 * <p>
 * For example, a Discord server for an
 * art commission team might have 3 types of tickets:
 * 1. Art commission
 * 2. Report
 * 3. Support request
 * <p>
 * Before creating a {@link Ticket}, the guild member
 * will be prompted to choose a {@link TicketType} in
 * a message. The guild member will be able to react to
 * this message with {@link #emoji} to select this
 * {@link TicketType}, and information will be shown
 * in the message.
 *
 * @see TicketStep
 */
@Data
@AllArgsConstructor
public class TicketType {
    /**
     * This map map emojis to the TicketType they represent.
     */
    public static final Map<String, TicketType> types = new HashMap<>();

    private String name;
    private String description;
    private String emoji;  // Must be unique across all TicketTypes.
    private List<TicketStep<?>> steps;

    /**
     * This method attempts to deserialize a TicketType from a
     * JSON object.
     * <p>
     * NOTE: This method actually deserializes a STORED TicketType,
     * as a TicketType is always STATIC and it will not change
     * based on the instance of the Ticket.
     * <p>
     * NOTE 2: This method effectively ignores typing because it
     * is very hard for Java to understand that the type
     * is always right.
     *
     * @param j The JsonObject to deserialize from
     * @return -
     */
    public static TicketType deserialize(JsonObject j) {
        List<TicketStep<?>> steps = new ArrayList<>();
        for (JsonElement step : j.getAsJsonArray("steps")) {
            JsonObject stp = step.getAsJsonObject();
            steps.add(TicketStep.deserialize(stp));
        }
        return new TicketType(j.get("name").getAsString(), j.get("description").getAsString(), j.get("emoji").getAsString(), steps);
    }

    public JsonObject serialize() {
        JsonObject j = new JsonObject();
        j.addProperty("name", name);
        j.addProperty("description", description);
        j.addProperty("emoji", emoji);
        JsonArray stps = new JsonArray();
        for (TicketStep<?> step : steps) {
            stps.add(step.serialize());
        }
        j.add("steps", stps);
        return j;
    }
}
