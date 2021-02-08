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
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class manages the assignees part of all tickets, including:
 * - Show assignees
 * - Assign
 * - Unassign
 */
public class ManageTicketsAssign extends ListenerAdapter {
    // All of the channels that already have the ticket assignees shown.
    // These channels will be able to react to emojis and do stuff with the assignees.
    // Maps from the channel ID to the message ID.
    private static final Map<Long, Long> channelDataShown = new HashMap<>();
    // All of the channels that already have the ticket assignees shown.
    // Maps from the channel ID to the CHANNEL ID OF THE TICKET.
    private static final Map<Long, Long> channelDataShownTickets = new HashMap<>();
    // This set represents the channels that are currently assigning
    // and is being prompted for who to assign.
    private static final Set<Long> addingChannels = new HashSet<>();
    // This set represents the channels that are currently unassigning
    // and is being prompted for who to unassign.
    private static final Set<Long> removingChannels = new HashSet<>();

    public static void showAssigneeData(MessageChannel channel, Ticket ticket) {
        ticket.sendAssigneesInfo(channel).queue(msg -> {
            channelDataShown.put(channel.getIdLong(), msg.getIdLong());
            channelDataShownTickets.put(channel.getIdLong(), ticket.getChannel());
            // Make sure you message me on Discord if you know how to eliminate this
            // callback hell here.
            msg.addReaction(TicketBot.config.manageTicketAssigneesAddEmoji()).queue(__ ->
                    msg.addReaction(TicketBot.config.manageTicketAssigneesRemoveEmoji()).queue(___ ->
                            msg.addReaction(TicketBot.config.manageTicketAssigneesExitEmoji()).queue()));
        });
    }

    public static void assign(Ticket ticket, long assignee) {
        if (ticket.getAssignees().contains(assignee)) {
            return;
        }
        ticket.getAssignees().add(assignee);
    }

    public static void unassign(Ticket ticket, long assignee) {
        ticket.getAssignees().remove(assignee);
    }

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent e) {
        if (channelDataShown.containsKey(e.getChannel().getIdLong()) &&
                e.getMessageIdLong() == channelDataShown.get(e.getChannel().getIdLong())) {
            e.retrieveUser().queue(user -> {
                if (user.isBot()) {
                    return;
                }

                if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketAssigneesAddEmoji())) {
                    // If we reacted with the add emoji
                    addingChannels.add(e.getChannel().getIdLong());
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setTitle(TicketBot.config.manageTicketAssigneesAssignUser())
                            .setColor(TicketBot.config.getAccentColor())
                            .build()).queue();
                } else if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketAssigneesRemoveEmoji())) {
                    // If we reacted with the remove emoji
                    removingChannels.add(e.getChannel().getIdLong());
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setTitle(TicketBot.config.manageTicketAssigneesUnassignUser())
                            .setColor(TicketBot.config.getAccentColor())
                            .build()).queue();
                } else if (e.getReactionEmote().getAsReactionCode().equals(TicketBot.config.manageTicketAssigneesExitEmoji())) {
                    channelDataShown.remove(e.getChannel().getIdLong());
                    // Return to the ticket data stage
                    ManageTicketsBasic.showTicketData(e.getChannel(),
                            Ticket.tickets.get(channelDataShownTickets.remove(e.getChannel().getIdLong())));
                }
            });
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        if (e.getAuthor().isBot()) {
            return;
        }
        if (addingChannels.contains(e.getChannel().getIdLong())) {
            Ticket ticket = Ticket.tickets.get(channelDataShownTickets.get(e.getChannel().getIdLong()));
            TextChannel channel = TicketBot.config.getGuild().getTextChannelById(ticket.getChannel());

            if (e.getMessage().getMentionedUsers().isEmpty()) {
                // If no one is mentioned
                TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.manageTicketAssigneesMentionInvalid());
                return;
            }
            for (User user : e.getMessage().getMentionedUsers()) {
                assign(ticket, user.getIdLong());  // Assign everybody that's mentioned

                // Send an info message
                channel.sendMessage(
                        new EmbedBuilder()
                                .setTitle(TicketBot.config.manageTicketTitleAssign()
                                        .replace("{USER}", e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator())
                                        .replace("{ASSIGNEE}", user.getName() + "#" + user.getDiscriminator()))
                                .setColor(TicketBot.config.getAccentColor())
                                .build()).queue();
            }
            showAssigneeData(e.getChannel(), ticket);
            addingChannels.remove(e.getChannel().getIdLong());
            ticket.sendBaseInfo(channel).queue();
        } else if (removingChannels.contains(e.getChannel().getIdLong())) {
            Ticket ticket = Ticket.tickets.get(channelDataShownTickets.get(e.getChannel().getIdLong()));
            TextChannel channel = TicketBot.config.getGuild().getTextChannelById(ticket.getChannel());
            if (e.getMessage().getMentionedUsers().isEmpty()) {
                // If no one is mentioned
                TicketBot.sendErrorMessage(e.getChannel(), TicketBot.config.manageTicketAssigneesMentionInvalid());
                return;
            }
            for (User user : e.getMessage().getMentionedUsers()) {
                unassign(ticket, user.getIdLong());  // Unassign everybody that's mentioned

                // Send an info message
                channel.sendMessage(
                        new EmbedBuilder()
                                .setTitle(TicketBot.config.manageTicketTitleUnassign()
                                        .replace("{USER}", e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator())
                                        .replace("{ASSIGNEE}", user.getName() + "#" + user.getDiscriminator()))
                                .setColor(TicketBot.config.getAccentColor())
                                .build()).queue();
            }
            showAssigneeData(e.getChannel(), ticket);
            removingChannels.remove(e.getChannel().getIdLong());
            ticket.sendBaseInfo(channel).queue();
        }
    }
}
