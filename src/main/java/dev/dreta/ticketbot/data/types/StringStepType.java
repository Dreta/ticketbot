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
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.function.Consumer;

/**
 * The StringStepType TicketStepType allows the guild member
 * to enter a string.
 * <p>
 * Options:
 * * "maximumLength" (int): Prevents the user from entering more characters than (>) a specified value.
 */
@StepType(
        name = "String",
        description = "Represents any text that the user can type in.",
        emoji = "abcd"
)
public class StringStepType extends ListenerAdapter implements TicketStepType<String> {
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
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        int maximumLength = options.has("maximumLength") ? options.get("maximumLength").getAsInt() : Integer.MAX_VALUE;

        if (e.getChannel().getIdLong() == channel.getIdLong() && !e.getAuthor().isBot()) {
            String msg = e.getMessage().getContentRaw();
            if (TicketBot.config.autoDeleteMessages()) {
                e.getMessage().delete().queue();
            }
            if (e.getMessage().getContentRaw().length() + 1 > maximumLength) {  // If the message exceeds the maximum length
                TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.stringLengthErrorMsg().replace("{LENGTH}", String.valueOf(maximumLength)));
                return;
            }
            callback.accept(msg);
            cleanup();
        }
    }

    @Override
    public void ask() {
        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(question)
                        .setDescription(description)
                        .setColor(TicketBot.config.getAccentColor())
                        .build()
        ).queue(m -> messageId = m.getIdLong());
    }
}
