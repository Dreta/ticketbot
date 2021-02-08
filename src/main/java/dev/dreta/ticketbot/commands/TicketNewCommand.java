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

package dev.dreta.ticketbot.commands;

import com.google.gson.JsonObject;
import dev.dreta.ticketbot.ChannelLock;
import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.*;
import dev.dreta.ticketbot.data.types.SingleSelectStepType;
import dev.dreta.ticketbot.data.types.StringStepType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;

import java.util.ArrayList;

/**
 * This class separately handles all of the ticket new
 * steps, including:
 * 1. Select the ticket type
 * 2. The title of the ticket
 * 3. Each step
 */
public class TicketNewCommand {
    /**
     * Step 1 of ticket creation.
     * This step utilizes a {@link dev.dreta.ticketbot.data.types.SingleSelectStepType}
     * for the user to choose a ticket type.
     * This step will be skipped when there is only 1 ticket type.
     *
     * @param member  The member who requested the ticket creation
     * @param channel The channel to ask questions in
     */
    public static void ticketNewStep1(Member member, TextChannel channel) {
        // Check if only 1 ticket type exists.
        if (TicketType.types.size() == 1) {
            ticketNewStep2(member, channel, TicketType.types.values().toArray(TicketType[]::new)[0]);
            return;
        }

        // Start asking
        ChannelLock.lockedChannels.add(channel.getIdLong());

        // Setup the StepType
        SingleSelectStepType su = new SingleSelectStepType();

        JsonObject options = new JsonObject();
        JsonObject opOptions = new JsonObject();
        for (TicketType type : TicketType.types.values()) {
            opOptions.addProperty(type.getEmoji(), TicketBot.config.ticketTypeFormat()
                    .replace("{NAME}", type.getName())
                    .replace("{DESCRIPTION}", type.getDescription()));
        }
        options.add("options", opOptions);
        options.addProperty("emoji", true);
        su.init(channel, TicketBot.config.ticketTypeTitle(), "", emoji -> {
            ticketNewStep2(member, channel, TicketType.types.get(emoji));
            ChannelLock.lockedChannels.remove(channel.getIdLong());
        }, options);

        // Ask the user the question
        su.ask();
    }

    /**
     * Step 2 of ticket creation.
     * This step utilizes a {@link dev.dreta.ticketbot.data.types.StringStepType}
     * for the user to set the title of their ticket.
     * This step also creates a new {@link dev.dreta.ticketbot.data.Ticket}
     * object.
     *
     * @param member  The member who requested the ticket creation
     * @param channel The channel to ask questions in
     * @param type    The {@link TicketType} provided by the previous step
     */
    public static void ticketNewStep2(Member member, TextChannel channel, TicketType type) {
        // Setup the StepType
        StringStepType su = new StringStepType();

        JsonObject options = new JsonObject();
        options.addProperty("maximumLength", TicketBot.config.ticketTitleMaxLength());

        su.init(channel, TicketBot.config.ticketTitleMsg(), "", title -> {
            // Create the ticket

            // For the assignees field, we used a Collections.emptyList(),
            // because we know that the assignees are going to be deserialized
            // from the data file, so it doesn't matter whether it will
            // be writable or not, we will always wrap it in an ArrayList
            // when writing to it for safety.

            // For the steps field, we used an ArrayList, because we will
            // immediately write to it in the next step.
            Ticket ticket = new Ticket(title, member.getIdLong(), channel.getIdLong(), true, new ArrayList<>(), new ArrayList<>());
            try {
                ticketNewStep3(channel, ticket, type);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }, options);
        // Ask the user the question
        su.ask();
    }

    /**
     * Step 3 of ticket creation.
     * This step utilizes each individual {@link dev.dreta.ticketbot.data.TicketStep} of
     * the selected {@link TicketType} to setup the ticket.
     *
     * @param channel The channel to ask questions in
     * @param ticket  The ticket created in the previous step
     * @param type    The type selected in the previous-previous step
     */
    public static void ticketNewStep3(TextChannel channel, Ticket ticket, TicketType type) throws ReflectiveOperationException {
        // Start with the first step
        ticketNewStep3Callback(channel, ticket, type, 0, type.getSteps().get(0));
    }

    /**
     * This method is the callback passed to the
     * {@link TicketStepType#init} function.
     * <p>
     * As we are dealing with multiple callbacks
     * linked together, we can't use a simple for
     * loop and we have to deal with callbacks in
     * this recursive approach.
     *
     * @param channel The channel to ask questions in
     * @param ticket  The ticket to add data to
     * @param type    The TicketType to get TicketStep's from
     * @param index   The index of "next"
     * @param next    The TicketStep to ask questions for
     */
    private static void ticketNewStep3Callback(TextChannel channel, Ticket ticket, TicketType type, int index, TicketStep<?> next) throws ReflectiveOperationException {
        // Create the step type
        TicketStepType<Object> stepType = (TicketStepType<Object>) next.getType().getConstructor().newInstance();

        stepType.init(channel, next.getTitle(), next.getDescription(), dataNew -> {
            try {
                ticket.getSteps().add(new TicketStepData<>(next.getTitle(), (Class<TicketStepType<Object>>) stepType.getClass(), dataNew));
                if (index + 1 == type.getSteps().size()) {
                    // We finished all the questions
                    Ticket.tickets.put(ticket.getChannel(), ticket);
                    Ticket.ticketsByUser.put(ticket.getAuthor(), ticket);
                    ChannelLock.lockedChannels.remove(channel.getIdLong());
                    channel.sendMessage(
                            new EmbedBuilder()
                                    .setTitle(TicketBot.config.ticketEndTitleMsg())
                                    .setDescription(TicketBot.config.ticketEndDescriptionMsg())
                                    .setColor(TicketBot.config.getAccentColor())
                                    .build()).queue(msg -> {
                        // Send another message in the channel so that
                        // the details of this ticket can be known.
                        ticket.sendBaseInfo(channel).queue();
                    });
                } else {
                    // Ask the next question
                    ticketNewStep3Callback(channel, ticket, type, index + 1, type.getSteps().get(index + 1));
                }
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException(ex);
            }
        }, next.getOptions());
        stepType.ask();
    }
}
