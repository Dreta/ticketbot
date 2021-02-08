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
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.function.Consumer;

/**
 * The SingleSelectStepType TicketStepType allows the guild member
 * to select one from a range of options by reacting to a specific
 * emoji.
 * <p>
 * Notice that the order of the emojis are NOT always the same.
 * It depends on which request reaches Discord first. DO NOT RELY ON
 * THE ORDER OF THE SELECTIONS.
 * <p>
 * Options:
 * * "options" (JsonObject): A JSON object mapping each emoji to their respective selections.
 * * "emoji" (boolean): Whether you want this step to store the selected emoji's name without : or the message associated with the emoji.
 */
@StepType(
        name = "Single Selection",
        description = "Allows the user to select one from a range of options. " +
                "Please enter a list of strings formatted like EMOJI<Space>MESSAGE.",
        emoji = "negative_squared_cross_mark"
)
public class SingleSelectStepType extends ListenerAdapter implements TicketStepType<String> {
    private TextChannel channel;
    private String question;
    private String description;
    private Consumer<String> callback;
    private JsonObject options;
    private long messageId;

    @Override
    public void init(TextChannel channel, String question, String description, Consumer<String> callback, JsonObject options) {
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
        // emoji defaults to false
        boolean emoji = options.has("emoji") && options.get("emoji").getAsBoolean();

        if (e.getChannel().getIdLong() == channel.getIdLong() && e.getMessageIdLong() == messageId && !e.getUser().isBot()) {
            e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
            if (options.getAsJsonObject("options").has(e.getReactionEmote().getAsReactionCode())) {
                if (emoji) {
                    callback.accept(e.getReactionEmote().getAsReactionCode());
                } else {
                    callback.accept(options.getAsJsonObject("options").get(e.getReactionEmote().getAsReactionCode()).getAsString());
                }
                cleanup();
            }
        }
    }

    @Override
    public void ask() {
        // Format the available options to ready it for sending
        StringBuilder availableOptions = new StringBuilder();

        // For each provided option
        options.getAsJsonObject("options").entrySet().forEach(entry -> {
            String emote = entry.getKey();
            String message = entry.getValue().getAsString();
            availableOptions.append(TicketBot.config.selectOptionFormat()
                    .replace("{EMOTE}", emote)  // Construct their respective message.
                    .replace("{MESSAGE}", message))
                    .append("\n");
        });

        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(question)
                        .setDescription(description + "\n\n" +
                                TicketBot.config.selectOptionsMsg() + "\n" +
                                availableOptions.toString() + "\n\n" +
                                TicketBot.config.selectOneInfoMsg())
                        .setColor(TicketBot.config.getAccentColor())
                        .build()
        ).queue(m -> {
            messageId = m.getIdLong();
            // Add each of the reactions so the user can react
            for (String emote : options.getAsJsonObject("options").keySet()) {
                m.addReaction(emote).queue();
            }
            ChannelLock.lockedChannels.add(channel.getIdLong());
        });
    }
}
