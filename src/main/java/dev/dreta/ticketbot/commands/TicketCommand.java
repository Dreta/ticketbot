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

import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.Ticket;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/**
 * The command !ticket creates a new channel for the
 * guild member to create a new ticket in an interactive
 * way.
 */
public class TicketCommand extends ListenerAdapter {
    private static final String COMMAND = TicketBot.config.getCommandPrefix() + "ticket";

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getMessage().getContentRaw().equalsIgnoreCase(COMMAND) && !e.getAuthor().isBot()) {
            if (TicketBot.config.botCommandsChannel() != 0 && e.getChannel().getIdLong() != TicketBot.config.botCommandsChannel()) {
                return;
            }
            Member member = e.getMember();

            TicketBot.config.channelsTicketCategory().createTextChannel(TicketBot.config.channelsChannelFormat()
                    .replace("{NAMEDISCRIM}", member.getUser().getName() + member.getUser().getDiscriminator())
                    .replace("{TICKETDISCRIM}", String.valueOf(Ticket.ticketsByUser.get(member.getIdLong()).size() + 1)))
                    .queue(channel -> {
                        channel.getManager().setTopic(TicketBot.config.channelsChannelTopic()
                                .replace("{NAME}", member.getUser().getName())
                                .replace("{NICKNAME}", member.getEffectiveName())
                                .replace("{DISCRIM}", member.getUser().getDiscriminator())).queue();
                        long permissions = Permission.getRaw(TicketBot.config.channelsPermissions());
                        channel.getManager().putPermissionOverride(channel.getGuild().getPublicRole(),
                                0, Permission.getRaw(Permission.VIEW_CHANNEL)).queue(__ ->
                                channel.getManager().putPermissionOverride(member, permissions, 0).queue());
                        for (Role role : TicketBot.config.channelsAllowedRoles()) {
                            channel.getManager().putPermissionOverride(role, permissions, 0).queue();
                        }

                        TicketNewCommand.ticketNewStep1(member, channel);
                    });
        }
    }
}
