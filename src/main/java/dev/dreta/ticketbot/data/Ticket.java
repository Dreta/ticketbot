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

package dev.dreta.ticketbot.data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.types.StepType;
import lombok.Data;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.requests.RestAction;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Ticket is the basis of TicketBot. It contains information
 * about who created this ticket, who this ticket is assigned
 * to, and detailed steps for this ticket. It is serializable
 * to a JSON data file.
 *
 * @see TicketStepData
 */
@Data
public class Ticket {
    /**
     * This map map channels to the Ticket they represent.
     */
    public static final Map<Long, Ticket> tickets = new HashMap<>();

    /**
     * This map maps users to the Ticket's they have created.
     */
    public static final Multimap<Long, Ticket> ticketsByUser = HashMultimap.create();

    /**
     * This static field represents the type of a
     * list of longs, used in deserializing the
     * Ticket for the assignees.
     */
    public static Type LONG_LIST_TYPE = new TypeToken<List<Long>>() {
    }.getType();

    private String title;
    private long author;
    private long channel;
    private boolean open;
    // NOTE: We will NOT remember the TicketType, as removing that
    //       TicketType will cause it to break. For details, see
    //       the docs for TicketStepData (specifically the part of
    //       "But this raises the problem...")
    //private TicketType type;
    private List<Long> assignees;
    private List<TicketStepData<?>> steps;

    private Member cachedAuthor;

    public Ticket(String title, long author, long channel, boolean open, List<Long> assignees, List<TicketStepData<?>> steps) {
        this.title = title;
        this.author = author;
        this.channel = channel;
        this.open = open;
        this.assignees = assignees;
        this.steps = steps;

        // Cache the author
        TicketBot.config.getGuild().retrieveMemberById(author).queue(m -> cachedAuthor = m);
    }

    public static Ticket deserialize(JsonObject j) {
        List<TicketStepData<?>> steps = new ArrayList<>();
        for (JsonElement step : j.getAsJsonArray("steps")) {
            JsonObject stp = step.getAsJsonObject();
            steps.add(TicketStepData.deserialize(stp));
        }

        return new Ticket(
                j.get("title").getAsString(), j.get("author").getAsLong(), j.get("channel").getAsLong(),
                j.get("open").getAsBoolean(),
                new ArrayList<>(TicketBot.gson.fromJson(j.getAsJsonArray("assignees"), LONG_LIST_TYPE)),
                steps);
    }

    /**
     * Send information about this ticket in a channel.
     *
     * @param channel The channel to send in
     * @return The action to operate on
     */
    public RestAction<Message> sendBaseInfo(MessageChannel channel) {
        TextChannel c = TicketBot.config.getGuild().getTextChannelById(this.channel);
        StringBuilder steps = new StringBuilder();
        for (int i = 0; i < this.steps.size(); i++) {
            TicketStepData<?> step = this.steps.get(i);
            steps.append(TicketBot.config.ticketDataStep()
                    .replace("{INDEX}", String.valueOf(i + 1))
                    .replace("{STEPTITLE}", step.getTitle())
                    .replace("{STEPTYPE}", step.getType().getAnnotation(StepType.class).name())
                    .replace("{STEPANSWER}", String.valueOf(step.getAnswer()))).append("\n");
        }
        StringBuilder assignees = new StringBuilder();
        for (int i = 0; i < this.assignees.size(); i++) {
            Member assignee = TicketBot.config.getGuild().getMemberById(this.assignees.get(i));
            assignees.append(TicketBot.config.ticketDataAssignee()
                    .replace("{INDEX}", String.valueOf(i + 1))
                    .replace("{NAME}", assignee.getUser().getName())
                    .replace("{DISCRIM}", assignee.getUser().getDiscriminator())
                    .replace("{NICKNAME}", assignee.getEffectiveName()))
                    .append("\n");
        }
        return channel.sendMessage(new EmbedBuilder()
                .setTitle(TicketBot.config.ticketDataTitle()
                        .replace("{TITLE}", title)
                        .replace("{AUTHORNAME}", cachedAuthor.getUser().getName())
                        .replace("{AUTHORDISCRIM}", cachedAuthor.getUser().getDiscriminator())
                        .replace("{AUTHORNICKNAME}", cachedAuthor.getEffectiveName())
                        .replace("{CHANNEL}", c.getName())
                        .replace("{OPEN}", open ? TicketBot.config.ticketDataOpenYes() : TicketBot.config.ticketDataOpenNo()))
                .setDescription(TicketBot.config.ticketDataDescription()
                        .replace("{TITLE}", title)
                        .replace("{AUTHORNAME}", cachedAuthor.getUser().getName())
                        .replace("{AUTHORDISCRIM}", cachedAuthor.getUser().getDiscriminator())
                        .replace("{AUTHORNICKNAME}", cachedAuthor.getEffectiveName())
                        .replace("{CHANNEL}", c.getName())
                        .replace("{OPEN}", open ? TicketBot.config.ticketDataOpenYes() : TicketBot.config.ticketDataOpenNo())
                        .replace("{STEPS}", steps.toString())
                        .replace("{ASSIGNEES}", this.assignees.size() == 0 ? TicketBot.config.listEmptyFormat() : assignees))
                .setColor(TicketBot.config.getAccentColor())
                .build());
    }

    /**
     * Send information about this ticket's assignees in a channel.
     *
     * @param channel The channel to send in
     * @return The action to operate on
     */
    public RestAction<Message> sendAssigneesInfo(MessageChannel channel) {
        StringBuilder assignees = new StringBuilder();
        for (int i = 0; i < this.assignees.size(); i++) {
            Member assignee = TicketBot.config.getGuild().getMemberById(this.assignees.get(i));
            assignees.append(TicketBot.config.ticketDataAssignee()
                    .replace("{INDEX}", String.valueOf(i + 1))
                    .replace("{NAME}", assignee.getUser().getName())
                    .replace("{DISCRIM}", assignee.getUser().getDiscriminator())
                    .replace("{NICKNAME}", assignee.getEffectiveName()))
                    .append("\n");
        }
        return channel.sendMessage(new EmbedBuilder()
                .setTitle(TicketBot.config.ticketDataAssigneesTitle())
                .setDescription(this.assignees.isEmpty() ? TicketBot.config.listEmptyFormat() : assignees.toString())
                .setColor(TicketBot.config.getAccentColor())
                .build());
    }

    public JsonObject serialize() {
        JsonObject j = new JsonObject();
        j.addProperty("title", title);
        j.addProperty("author", author);
        j.addProperty("channel", channel);
        j.addProperty("open", open);
        j.add("assignees", TicketBot.gson.toJsonTree(assignees).getAsJsonArray());
        JsonArray steps = new JsonArray();
        for (TicketStepData<?> stepData : this.steps) {
            steps.add(stepData.serialize());
        }
        j.add("steps", steps);
        return j;
    }
}
