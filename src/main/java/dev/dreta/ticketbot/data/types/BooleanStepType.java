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
 * The BooleanStepType TicketStepType asks the guild member a
 * yes-no question and allows the guild member to react with
 * yes/no emotes (which is configurable).
 * <p>
 * Options:
 * * "mustBeTrue" (boolean): Whether the user must react to "Yes" to continue.
 * * "mustBeFalse" (boolean): Whether the user must react to "No" to continue.
 */
@StepType(
        name = "Boolean",
        description = "Represents a Yes/No answer from the user.",
        emoji = ""
)
public class BooleanStepType extends ListenerAdapter implements TicketStepType<Boolean> {
    private TextChannel channel;
    private String question;
    private String description;
    private Consumer<Boolean> callback;
    private JsonObject options;
    private long messageId;  // We need this so we can know if the guild member is reacting to the correct message

    @Override
    public void init(TextChannel channel, String question, String description, Consumer<Boolean> callback, JsonObject options) {
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
        if (e.getChannel().getIdLong() == channel.getIdLong() && e.getMessageIdLong() == messageId && !e.getUser().isBot()) {
            e.getChannel().removeReactionById(messageId, e.getReactionEmote().getAsReactionCode(), e.getUser()).queue();
            if (e.getReactionEmote().getAsReactionCode().equalsIgnoreCase(TicketBot.config.booleanYesEmoji())) {
                // Yes

                // Must be false defaults to false
                if (options.has("mustBeFalse") && options.get("mustBeFalse").getAsBoolean()) {
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.booleanMustBeFalseMsg());
                    return;
                }
                callback.accept(true);
                cleanup();
            } else if (e.getReactionEmote().getAsReactionCode().equalsIgnoreCase(TicketBot.config.booleanNoEmoji())) {
                // No

                // Must be true defaults to false
                if (options.has("mustBeTrue") && options.get("mustBeTrue").getAsBoolean()) {
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.booleanMustBeTrueMsg());
                    return;
                }
                callback.accept(false);
                cleanup();
            }
        }
    }

    @Override
    public void ask() {
        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(question)
                        .setDescription(description + "\n\n" +
                                TicketBot.config.booleanInfoMsg()
                                        .replace("{YES_EMOJI}", TicketBot.config.booleanYesEmoji())
                                        .replace("{NO_EMOJI}", TicketBot.config.booleanNoEmoji()))
                        .setColor(TicketBot.config.getAccentColor())
                        .build()
        ).queue(m -> {
            messageId = m.getIdLong();
            // Add the reactions so the user can react
            m.addReaction(TicketBot.config.booleanYesEmoji()).queue();
            m.addReaction(TicketBot.config.booleanNoEmoji()).queue();
            ChannelLock.lockedChannels.add(channel.getIdLong());
        });
    }
}
