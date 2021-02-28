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

import com.google.gson.JsonObject;
import dev.dreta.ticketbot.ChannelLock;
import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.TicketStepType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The MultiSelectStepType TicketStepType allows the guild member
 * to select multiple options from a range of options by reacting
 * to specific emojis.
 * <p>
 * Notice that the order of emojis and the resulting selections are
 * NOT always the same. It depends on which request reaches Discord
 * first. DO NOT RELY ON THE ORDER OF THE SELECTIONS.
 * Options:
 * * "options" (JsonObject): A JSON object mapping each emoji to their respective selections.
 * * "maximumLength" (int): Prevents the user from selecting more options than (>) a specified value.
 * * "allowEmptyList" (boolean): Prevents the user from selecting no options if set to false.
 * * "emoji" (boolean): Whether you want this step to store the selected emojis' name without : or the message associated with the emojis.
 */
@StepType(
        name = "Multiple Selection",
        description = "Allows the user to select multiple options from a range of options. " +
                "Please enter a list of strings formatted like EMOJI<Space>MESSAGE.",
        emoji = ""
)
public class MultiSelectStepType extends ListenerAdapter implements TicketStepType<List<String>> {
    private final List<String> currentResponse = new ArrayList<>();

    private TextChannel channel;
    private String question;
    private String description;
    private Consumer<List<String>> callback;
    private JsonObject options;
    private long messageId;

    @Override
    public void init(TextChannel channel, String question, String description, Consumer<List<String>> callback, JsonObject options) {
        this.channel = channel;
        this.question = question;
        this.description = description;
        this.callback = callback;
        this.options = options;
        TicketBot.jda.addEventListener(this);
    }

    @Override
    public void cleanup() {
        TicketBot.jda.removeEventListener(this);
        if (TicketBot.config.autoDeleteMessages()) {
            channel.deleteMessageById(messageId).queue();
        }
        ChannelLock.lockedChannels.remove(channel.getIdLong());
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        int maximumLength = options.has("maximumLength") ? options.get("maximumLength").getAsInt() : Integer.MAX_VALUE;
        // allowEmptyList defaults to true
        boolean allowEmptyList = !options.has("allowEmptyList") || options.get("allowEmptyList").getAsBoolean();
        // emoji defaults to false
        boolean emoji = options.has("emoji") && options.get("emoji").getAsBoolean();

        if (e.getChannel().getIdLong() == channel.getIdLong() && e.getMessageIdLong() == messageId && !e.getUser().isBot()) {
            if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.selectMultiEndEmoji())) {
                if (!allowEmptyList && currentResponse.isEmpty()) {  // If we have an empty list and it's not allowed
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.selectMultiEmptyError());
                    // Remove the END emoji so the user can correct their input and end again.
                    e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
                    return;
                }
                callback.accept(currentResponse);
                cleanup();
            } else if (options.getAsJsonObject("options").has(e.getReactionEmote().getAsReactionCode())) {
                if (currentResponse.size() + 1 > maximumLength) {  // If we exceeded the maximum length
                    e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.selectMultiLengthError().replace("{LENGTH}", String.valueOf(maximumLength)));
                    return;
                }
                if (emoji) {
                    currentResponse.add(e.getReactionEmote().getAsReactionCode());
                } else {
                    currentResponse.add(options.getAsJsonObject("options").get(e.getReactionEmote().getAsReactionCode()).getAsString());
                }
            } else {
                // Clear the reaction if you can't actually react to this
                e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
            }
        }
    }

    @Override
    public void onGuildMessageReactionRemove(GuildMessageReactionRemoveEvent e) {
        // emoji defaults to false
        boolean emoji = options.has("emoji") && options.get("emoji").getAsBoolean();
        if (e.getChannel().getIdLong() == channel.getIdLong() && e.getMessageIdLong() == messageId) {
            e.retrieveUser().queue(user -> {
                if (user.isBot()) {
                    return;
                }

                if (options.getAsJsonObject("options").has(e.getReactionEmote().getAsReactionCode())) {
                    if (emoji) {
                        currentResponse.remove(e.getReactionEmote().getAsReactionCode());
                    } else {
                        currentResponse.remove(options.getAsJsonObject("options").get(e.getReactionEmote().getAsReactionCode()).getAsString());
                    }
                }
            });
        }
    }

    @Override
    public void ask() {
        // Format the available options to ready it for sending
        StringBuilder availableOptions = new StringBuilder();

        // Everything about this is just so similar to SingleSelectStepType.
        options.getAsJsonObject("options").entrySet().forEach(entry -> {
            String emote = entry.getKey();
            String message = entry.getValue().getAsString();
            availableOptions.append(TicketBot.config.selectOptionFormat()
                    .replace("{EMOTE}", emote)
                    .replace("{MESSAGE}", message))
                    .append("\n");
        });

        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(question)
                        .setDescription(description + "\n\n" +
                                TicketBot.config.selectOptionsMsg() + "\n" +
                                availableOptions.toString() + "\n\n" +
                                TicketBot.config.selectMultiInfoMsg()
                                        .replace("{EMOTE}", TicketBot.config.selectMultiEndEmoji()))
                        .setColor(TicketBot.config.getAccentColor())
                        .build()
        ).queue(m -> {
            messageId = m.getIdLong();
            // Add each of the reactions so the user can react
            for (String emote : options.getAsJsonObject("options").keySet()) {
                m.addReaction(emote).queue();
            }
            // We want there to be a high probability that the
            // end emoji will be shown last, however this does
            // not really matter so we are not enforcing that.
            m.addReaction(TicketBot.config.selectMultiEndEmoji()).queueAfter(50, TimeUnit.MILLISECONDS);
            ChannelLock.lockedChannels.add(channel.getIdLong());
        });
    }
}
