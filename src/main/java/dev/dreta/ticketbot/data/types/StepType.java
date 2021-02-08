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

package dev.dreta.ticketbot.data.types;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotating a class with this annotation will allow it
 * to specify the metadata for a StepType. This is required
 * for all of the subclasses of {@link dev.dreta.ticketbot.data.TicketStepType}.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface StepType {
    /**
     * The name of this {@link dev.dreta.ticketbot.data.TicketStepType}.
     *
     * @return -
     */
    String name();

    /**
     * The description of this {@link dev.dreta.ticketbot.data.TicketStepType}.
     *
     * @return -
     */
    String description();

    /**
     * The emoji of this {@link dev.dreta.ticketbot.data.TicketStepType}.
     * <p>
     * The emoji will be used to uniquely identify this
     * {@link dev.dreta.ticketbot.data.TicketStepType}
     * when the guild owner is prompted to choose a
     * {@link dev.dreta.ticketbot.data.TicketStepType}.
     *
     * @return -
     */
    String emoji();
}
