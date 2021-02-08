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

package dev.dreta.ticketbot;

import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class allows another class to request the
 * locking of a channel, which means no one will be
 * able to send messages in that channel anymore.
 */
public class ChannelLock extends ListenerAdapter {
    public static final List<Long> lockedChannels = new ArrayList<>();

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (lockedChannels.contains(e.getChannel().getIdLong()) && !e.getAuthor().isBot()) {
            e.getMessage().delete().queue();
        }
    }
}
