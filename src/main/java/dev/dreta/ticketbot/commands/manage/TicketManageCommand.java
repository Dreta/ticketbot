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

package dev.dreta.ticketbot.commands.manage;

import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.commands.manage.ticket.ManageTicketsBasic;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.stream.Collectors;

/**
 * This class separately handles all of the ticket management
 * steps, details:
 * - Select what to manage
 * - Tickets:
 * - Select a ticket
 * - Show data
 * - Manage assignees
 * - Close/Open ticket
 */
public class TicketManageCommand extends ListenerAdapter {
    private static final String COMMAND = TicketBot.config.getCommandPrefix() + "ticket manage";

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getMessage().getContentRaw().equalsIgnoreCase(COMMAND) && !e.getAuthor().isBot()) {
            if (TicketBot.config.botCommandsChannel() != 0 && e.getChannel().getIdLong() != TicketBot.config.botCommandsChannel()) {
                return;
            }
            TicketBot.config.channelsTicketCategory().createTextChannel(TicketBot.config.channelsManageFormat()
                    .replace("{NAMEDISCRIM}", e.getAuthor().getName() + e.getAuthor().getDiscriminator()))
                    .queue(channel -> {
                        if (!e.getMember().getRoles().stream().map(Role::getName).collect(Collectors.toList()).contains("Ticket Bot Manager") && !e.getMember().isOwner()) {
                            TicketBot.sendErrorMessage(channel, TicketBot.config.managePermissionError());
                            return;
                        }
                        ManageTicketsBasic.selectTicket(channel);
                    });
        }
    }
}
