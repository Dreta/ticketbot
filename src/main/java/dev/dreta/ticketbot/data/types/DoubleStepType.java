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
 * The DoubleStepType TicketStepType allows the guild member
 * to enter a floating-point number.
 * <p>
 * Options:
 * * "min" (double): Requires the user to enter a value above (>=) the minimum.
 * * "max" (double): Requires the user to enter a value below (<=) the maximum.
 */
@StepType(
        name = "Double",
        description = "Represents a floating-point number that the user can type in.",
        emoji = ""
)
public class DoubleStepType extends ListenerAdapter implements TicketStepType<Double> {
    private TextChannel channel;
    private String question;
    private String description;
    private Consumer<Double> callback;
    private JsonObject options;
    private long messageId;

    @Override
    public void init(TextChannel channel, String question, String description, Consumer<Double> callback, JsonObject options) {
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
        if (e.getChannel().getIdLong() == channel.getIdLong() && !e.getAuthor().isBot()) {
            try {
                double d = Double.parseDouble(e.getMessage().getContentStripped());
                double min = options.has("min") ? options.get("min").getAsDouble() : Double.MIN_VALUE;
                double max = options.has("max") ? options.get("max").getAsDouble() : Double.MAX_VALUE;

                if (TicketBot.config.autoDeleteMessages()) {
                    e.getMessage().delete().queue();
                }
                if (d < min) {  // If the value does not match minimum requirements
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.doubleMinErrorMsg()
                            .replace("{MIN}", String.valueOf(min)));
                    return;
                }
                if (d > max) {  // If the value does not match maximum requirements
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.doubleMaxErrorMsg()
                            .replace("{MAX}", String.valueOf(max)));
                    return;
                }
                callback.accept(d);
                cleanup();
            } catch (NumberFormatException ex) {
                e.getMessage().delete().queue();
                TicketBot.sendErrorMessage(channel, TicketBot.config.doubleFormatErrorMsg());
            }
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
