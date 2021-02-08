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

package dev.dreta.ticketbot.commands.manage.ticket;

import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.Ticket;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class manages the basic part of all the tickets, including:
 * - Select a ticket
 * - Show data
 * - Close/reopen a ticket
 */
public class ManageTicketsBasic extends ListenerAdapter {
    // All of the channels that are at the ticket selecting stage
    private static final Set<Long> channelSelecting = new HashSet<>();
    // All of the channels that already have the ticket data shown.
    // These channels will be able to react to emojis and do stuff with the ticket.
    // Maps from the channel ID to the message ID.
    private static final Map<Long, Long> channelDataShown = new HashMap<>();
    // All of the channels that already have the ticket data shown.
    // Maps from the channel ID to the CHANNEL ID OF THE TICKET.
    // We need this because we are not necessarily in the same channel
    // as the ticket.
    private static final Map<Long, Long> channelDataShownTickets = new HashMap<>();

    /**
     * Select a ticket to manage.
     *
     * @param channel The channel to send messages in
     */
    public static void selectTicket(MessageChannel channel) {
        channel.sendMessage(new EmbedBuilder()
                .setTitle(TicketBot.config.manageTicketSelectTitle())
                .setDescription(TicketBot.config.manageTicketSelectDescription())
                .setColor(TicketBot.config.getAccentColor())
                .build()).queue();
        channelSelecting.add(channel.getIdLong());
    }

    public static void showTicketData(MessageChannel channel, Ticket ticket) {
        ticket.sendBaseInfo(channel).queue(msg -> {
            channelDataShown.put(channel.getIdLong(), msg.getIdLong());
            channelDataShownTickets.put(channel.getIdLong(), ticket.getChannel());
            msg.addReaction(ticket.isOpen() ? TicketBot.config.manageTicketCloseEmoji() : TicketBot.config.manageTicketOpenEmoji()).queue();
            msg.addReaction(TicketBot.config.manageTicketAssigneesEmoji()).queue();
            msg.addReaction(TicketBot.config.manageTicketExitEmoji()).queue();
        });
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (channelSelecting.contains(e.getChannel().getIdLong()) && !e.getAuthor().isBot()) {
            Message message = e.getMessage();
            try {
                if (message.getMentionedChannels().isEmpty()) {
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.manageTicketSelectError());
                    return;
                }
                TextChannel channel = message.getMentionedChannels().get(0);
                // Find the ticket according to the channel
                Ticket ticket = Ticket.tickets.get(channel.getIdLong());
                if (ticket == null) {
                    TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.manageTicketSelectError());
                    return;
                }
                channelSelecting.remove(e.getChannel().getIdLong());
                showTicketData(e.getChannel(), ticket);
            } catch (IndexOutOfBoundsException ex) {
                TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.manageTicketSelectError());
            }
        }
    }

    @Override
    public void onGuildMessageReactionAdd(GuildMessageReactionAddEvent e) {
        if (channelDataShown.containsKey(e.getChannel().getIdLong()) &&
                e.getMessageIdLong() == channelDataShown.get(e.getChannel().getIdLong())) {
            e.retrieveUser().queue(user -> {
                if (user.isBot()) {
                    return;
                }

                Ticket ticket = Ticket.tickets.get(channelDataShownTickets.get(e.getChannel().getIdLong()));
                if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketCloseEmoji()) ||
                        e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketOpenEmoji())) {
                    // Toggle open state
                    ticket.setOpen(!ticket.isOpen());
                    // Update data
                    showTicketData(e.getChannel(), ticket);

                    TextChannel channel = TicketBot.config.getGuild().getTextChannelById(ticket.getChannel());
                    if (ticket.isOpen()) {
                        channel.sendMessage(
                                new EmbedBuilder()
                                        .setTitle(TicketBot.config.manageTicketTitleOpen()
                                                .replace("{USER}", user.getName() + "#" + user.getDiscriminator()))
                                        .setColor(TicketBot.config.getAccentColor())
                                        .build()).queue();
                    } else {
                        channel.sendMessage(
                                new EmbedBuilder()
                                        .setTitle(TicketBot.config.manageTicketTitleClose()
                                                .replace("{USER}", user.getName() + "#" + user.getDiscriminator()))
                                        .setColor(TicketBot.config.getAccentColor())
                                        .build()).queue();
                    }
                    ticket.sendBaseInfo(channel).queue();
                } else if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketAssigneesEmoji())) {
                    // Manage assignees
                    ManageTicketsAssign.showAssigneeData(e.getChannel(), ticket);
                    channelDataShown.remove(e.getChannel().getIdLong());
                    channelDataShownTickets.remove(e.getChannel().getIdLong());
                } else if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketExitEmoji())) {
                    // Stop managing
                    channelDataShown.remove(e.getChannel().getIdLong());
                    e.getChannel().delete().queue();
                }
            });
        }
    }
}
