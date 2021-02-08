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
import dev.dreta.ticketbot.data.types.StepType;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.function.Consumer;

/**
 * A TicketStepType represents a possible type of input
 * to be parsed when creating a ticket. For more information,
 * see {@link TicketStepData}.
 *
 * @param <T> The type that this TicketStepType parses into
 * @see TicketStepData
 */
public interface TicketStepType<T> {
    /**
     * Get the name of this TicketStepType.
     * This name will be displayed when the steps are being
     * edited interactively.
     *
     * @return The name
     */
    default String getName() {
        if (!this.getClass().isAnnotationPresent(StepType.class)) {
            throw new IllegalStateException("Subclass of TicketStepType (" +
                    this.getClass().getName() + ") not annotated with @StepType.");
        }
        return this.getClass().getAnnotation(StepType.class).name();
    }

    /**
     * Get the description of this TicketStepType.
     * This description will be displayed when the steps are
     * being edited interactively.
     *
     * @return The description
     */
    default String getDescription() {
        if (!this.getClass().isAnnotationPresent(StepType.class)) {
            throw new IllegalStateException("Subclass of TicketStepType (" +
                    this.getClass().getName() + ") not annotated with @StepType.");
        }
        return this.getClass().getAnnotation(StepType.class).description();
    }

    /**
     * Get the emoji of this TicketStepType.
     * Must be unique across all TicketStepTypes.
     * This emoji will be displayed when the steps are
     * being edited interactively.
     *
     * @return The emoji
     */
    default String getEmoji() {
        if (!this.getClass().isAnnotationPresent(StepType.class)) {
            throw new IllegalStateException("Subclass of TicketStepType (" +
                    this.getClass().getName() + ") not annotated with @StepType.");
        }
        return this.getClass().getAnnotation(StepType.class).emoji();
    }

    /**
     * This method should initialize this TicketStepType.
     * It will be called immediately after the TicketStepType
     * is constructed.
     * <p>
     * For example, you can register the event handler if
     * you have the needs for it in {@link dev.dreta.ticketbot.TicketBot#jda}.
     * <p>
     * You should also set the variables of channel, question
     * and callback for access in {@link #ask}.
     *
     * @param channel     The channel to ask in
     * @param question    The question to ask
     * @param description The description of the question
     * @param callback    The callback for the results
     * @param options     The options the administrator specified for this type
     */
    void init(TextChannel channel, String question, String description, Consumer<T> callback, JsonObject options);

    /**
     * This method should cleanup the TicketStepType.
     * It should be manually called once this step is finished.
     */
    void cleanup();

    /**
     * This method should ask the guild member about a specific question
     * with this TicketStepType in a specific channel. This method handles
     * everything from sending the messages, handling the response and
     * calling the callback.
     */
    void ask();
}
