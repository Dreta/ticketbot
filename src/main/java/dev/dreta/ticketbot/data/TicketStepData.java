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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.dreta.ticketbot.TicketBot;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A {@link TicketStepData} represents a step in
 * the ticket and the value the guild member answered
 * for the step.
 * <p>
 * The idea here is that the guild owner should be able to
 * customize what each step is, so if this ticket bot is
 * used for a reporting bugs scenario, then the steps would
 * probably be:
 * TITLE --------------- TYPE
 * 1. TOS agreement      <Boolean>
 * 2. Describe your bug  <String>
 * 3. Steps to reproduce <List>
 * 4. System information <String>
 * 5. Additional context <String>
 * <p>
 * And in the case of a support ticket, the steps would
 * probably be:
 * TITLE ----------------- TYPE
 * 1. TOS agreement       <Boolean>
 * 2. Support details     <String>
 * 3. Contact information <String>
 * <p>
 * In {@link TicketStepData}, we will store the TITLE as a string,
 * the type as a class reference to {@link TicketStepType},
 * and the answer the user provided as generic T.
 * <p>
 * But this raises the problem that we will not be able to
 * update a {@link TicketStepData} in case the guild owner changed
 * the steps, well we do not, since we would not know how
 * to map the previous answer to the new one.
 * <p>
 * Note that a {@link TicketStepData} is NOT a {@link TicketStep}.
 * It is independent for storing the data associated to a user's
 * ANSWER.
 */
@Data
@AllArgsConstructor
public class TicketStepData<T> {
    /**
     * This static field represents the type of a
     * list of strings, used in deserializing the
     * TicketStepData.
     */
    public static Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();
    private String title;
    private Class<TicketStepType<T>> type;
    private T answer;

    /**
     * This method attempts to deserialize a TicketStepData from a
     * JSON object.
     * <p>
     * NOTE: This method effectively ignores typing because it
     * is very hard for Java to understand that the type
     * is always right.
     *
     * @param j The JsonObject to deserialize from
     * @return -
     */
    public static TicketStepData<?> deserialize(JsonObject j) {
        Class<TicketStepType<Object>> type;
        try {
            type = (Class<TicketStepType<Object>>) Class.forName(j.get("type").getAsString());
        } catch (ClassNotFoundException ex) {
            throw new IllegalArgumentException("Couldn't find step type of " + j.get("type").getAsString() + ".");
        }
        JsonElement ans = j.get("answer");
        Object answer = null;
        if (ans.isJsonArray()) {
            answer = TicketBot.gson.fromJson(ans, STRING_LIST_TYPE);
        } else if (ans.isJsonPrimitive()) {
            try {
                // We know for sure the Class.forName is going to succeed,
                // because it is always a primitive, String or a List type
                // which always exists.
                answer = TicketBot.gson.fromJson(ans, Class.forName(j.get("answerType").getAsString()));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return new TicketStepData<>(j.get("title").getAsString(), type, answer);
    }

    public JsonObject serialize() {
        JsonObject j = new JsonObject();
        j.addProperty("title", title);
        j.addProperty("type", type.getName());
        // If the answer is a primitive (which in most cases it should),
        // then add it directly to the JsonObject.
        if (answer instanceof String) {
            j.addProperty("answer", (String) answer);
            j.addProperty("answerType", answer.getClass().getName());
        } else if (answer instanceof Number) {
            j.addProperty("answer", (Number) answer);
            j.addProperty("answerType", answer.getClass().getName());
        } else if (answer instanceof Character) {
            j.addProperty("answer", (Character) answer);
            j.addProperty("answerType", answer.getClass().getName());
        } else if (answer instanceof Boolean) {
            j.addProperty("answer", (Boolean) answer);
            j.addProperty("answerType", answer.getClass().getName());
        } /* If we are a list, then convert to JSON array */ else if (answer instanceof List) {
            j.add("answer", TicketBot.gson.toJsonTree(answer).getAsJsonArray());
            j.addProperty("answerType", List.class.getName());
        } else {
            throw new IllegalArgumentException("Illegal argument type");
        }
        return j;
    }
}
