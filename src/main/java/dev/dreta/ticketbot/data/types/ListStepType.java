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
import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.TicketStepType;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * The DoubleStepType TicketStepType allows the guild member
 * to enter several strings, and react to the end emote
 * (configurable) to go to the next step.
 * <p>
 * Options:
 * * "maximumLength" (int): Prevents the user from entering more items than (>) a specified value.
 * * "allowEmptyList" (boolean): Prevents the user from entering an empty list if set to false.
 */
@StepType(
        name = "List",
        description = "Represents a list of text that the user can type in.",
        emoji = ""
)
public class ListStepType extends ListenerAdapter implements TicketStepType<List<String>> {
    private TextChannel channel;
    private String question;
    private String description;
    // What the user typed in so far
    private List<String> currentResponse;
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
        this.currentResponse = new ArrayList<>();
        TicketBot.jda.addEventListener(this);
    }

    @Override
    public void cleanup() {
        TicketBot.jda.removeEventListener(this);
        if (TicketBot.config.autoDeleteMessages()) {
            channel.deleteMessageById(messageId).queue();
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        // allowEmptyList defaults to true
        boolean allowEmptyList = !options.has("allowEmptyList") || options.get("allowEmptyList").getAsBoolean();
        if (e.getChannel().getIdLong() == channel.getIdLong() && e.getMessageIdLong() == messageId && !e.getUser().isBot()) {
            // Automagically remove the reaction from the user
            e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
            if (e.getReactionEmote().getAsReactionCode().equalsIgnoreCase(TicketBot.config.listEndEmoji())) {
                // End
                if (currentResponse.isEmpty() && !allowEmptyList) {  // If empty and allowEmptyList is false, send error
                    TicketBot.sendErrorMessage(channel, TicketBot.config.listEmptyListError());
                    return;
                }
                callback.accept(currentResponse);
                cleanup();
            } else if (e.getReactionEmote().getAsReactionCode().equalsIgnoreCase(TicketBot.config.listDeleteLastEmoji())) {
                if (currentResponse.isEmpty()) {  // If empty, send error message
                    TicketBot.sendErrorMessage(channel, TicketBot.config.listDeleteLastEmptyListErrorMsg());
                    return;
                }
                currentResponse.remove(currentResponse.size() - 1);  // Remove the last item
                updateListMessage();  // Edit message to reflect update in list content
            } else {
                // Otherwise we will remove the emoji that the guild member wrongly reacted to
                e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
            }
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        int maximumLength = options.has("maximumLength") ? options.get("maximumLength").getAsInt() : Integer.MAX_VALUE;

        if (e.getChannel().getIdLong() == channel.getIdLong() && !e.getAuthor().isBot()) {
            String msg = e.getMessage().getContentRaw();
            if (TicketBot.config.autoDeleteMessages()) {
                e.getMessage().delete().queue();
            }
            if (currentResponse.size() + 1 > maximumLength) {  // If the list is going to exceed the maximum length
                TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.listLengthErrorMsg().replace("{LENGTH}", String.valueOf(maximumLength)));
                return;
            }
            currentResponse.add(msg);
            updateListMessage();  // Edit message to reflect update in list content
        }
    }

    /**
     * Edit the message to accurately reflect items in the list.
     */
    private void updateListMessage() {
        channel.retrieveMessageById(messageId)
                .queue(message -> {
                    // Build a list of formatted items of this list.
                    StringBuilder items = new StringBuilder();
                    for (int i = 0; i < currentResponse.size(); i++) {
                        items.append(TicketBot.config.listItemFormat()
                                .replace("{INDEX}", String.valueOf(i + 1))
                                .replace("{ITEM}", currentResponse.get(i)))
                                .append("\n");
                    }
                    message.editMessage(new EmbedBuilder()
                            .setTitle(question)
                            .setDescription(description + "\n\n" +
                                    TicketBot.config.listItemsFormat()
                                            .replace("{ITEMS}", currentResponse.isEmpty() ? TicketBot.config.listEmptyFormat() + "\n" : items.toString()) + "\n" +
                                    TicketBot.config.listInfoMsg()
                                            .replace("{DELETE_LAST_EMOJI}", TicketBot.config.listDeleteLastEmoji())
                                            .replace("{END_EMOJI}", TicketBot.config.listEndEmoji()))
                            .setColor(TicketBot.config.getAccentColor())
                            .build()).queue();
                });
    }

    @Override
    public void ask() {
        // Send a message with the items set to "Empty"
        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(question)
                        .setDescription(description + "\n\n" +
                                TicketBot.config.listItemsFormat()
                                        .replace("{ITEMS}", TicketBot.config.listEmptyFormat()) + "\n\n" +
                                TicketBot.config.listInfoMsg()
                                        .replace("{DELETE_LAST_EMOJI}", TicketBot.config.listDeleteLastEmoji())
                                        .replace("{END_EMOJI}", TicketBot.config.listEndEmoji()))
                        .setColor(TicketBot.config.getAccentColor())
                        .build()
        ).queue(m -> {
            messageId = m.getIdLong();
            // Add the reactions so the user can react.
            m.addReaction(TicketBot.config.listDeleteLastEmoji()).queue();
            // We want there to be a high probability that the
            // end emoji will be shown last, however this does
            // not really matter so we are not enforcing that.
            m.addReaction(TicketBot.config.listEndEmoji()).queueAfter(50, TimeUnit.MILLISECONDS);
        });
    }
}
