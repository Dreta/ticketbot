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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.dreta.ticketbot.commands.TicketCommand;
import dev.dreta.ticketbot.commands.manage.TicketManageCommand;
import dev.dreta.ticketbot.commands.manage.ticket.ManageTicketsAssign;
import dev.dreta.ticketbot.commands.manage.ticket.ManageTicketsBasic;
import dev.dreta.ticketbot.data.Ticket;
import dev.dreta.ticketbot.data.TicketStepType;
import dev.dreta.ticketbot.data.TicketType;
import dev.dreta.ticketbot.data.types.*;
import dev.dreta.ticketbot.extensions.ExtensionLoader;
import dev.dreta.ticketbot.utils.Configuration;
import dev.dreta.ticketbot.utils.DataConfiguration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;

import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * The TicketBot.
 * <p>
 * For detailed explanation on how this works, see
 * {@link dev.dreta.ticketbot.data.TicketStepData},
 * {@link dev.dreta.ticketbot.data.TicketStep} and
 * {@link dev.dreta.ticketbot.data.TicketType}.
 *
 * @see dev.dreta.ticketbot.data.TicketStepData
 * @see dev.dreta.ticketbot.data.TicketStep
 * @see dev.dreta.ticketbot.data.TicketType
 */
public class TicketBot {
    public static DataConfiguration config;
    public static Configuration data;
    public static JDA jda;
    public static Gson gson;
    public static ExtensionLoader extLoader;

    public static List<Class<? extends TicketStepType<?>>> stepTypes = new ArrayList<>();

    public static void main(String[] args) throws LoginException,
            InterruptedException, InvocationTargetException,
            InstantiationException, IllegalAccessException,
            IOException {
        gson = new Gson();

        System.out.println("Loading configuration...");
        config = new DataConfiguration();
        config.load("config.json");
        data = new Configuration();
        data.load("data.json");

        System.out.println("Connecting...");
        jda = JDABuilder.createDefault(config.getToken())
                .addEventListeners(new ManageTicketsAssign(), new ManageTicketsBasic(),
                        new TicketCommand(), new TicketManageCommand(), new ExitListener(),
                        new ChannelLock())
                .build();
        jda.awaitReady();

        System.out.println("Registering built-in step types...");
        // Register built-in step-types
        registerStepType(BooleanStepType.class);
        registerStepType(DoubleStepType.class);
        registerStepType(IntegerStepType.class);
        registerStepType(ListStepType.class);
        registerStepType(StringStepType.class);
        registerStepType(SingleSelectStepType.class);
        registerStepType(MultiSelectStepType.class);

        System.out.println("Loading extensions...");

        File extensionsDir = new File("extensions").getAbsoluteFile();
        if (!extensionsDir.exists() && !extensionsDir.mkdirs()) {
            TicketBot.jda.shutdown();
            throw new RuntimeException(new IOException("Failed to create extensions directory."));
        }

        extLoader = new ExtensionLoader(extensionsDir);
        for (File ext : extLoader.getAvailableExtensions()) {
            extLoader.enableExtension(extLoader.getOrCreateLoader(ext));
        }

        System.out.println("Loading data...");
        loadAll();

        // Add shutdown hook for saving
        Thread shutdownSaveThread = new Thread(TicketBot::saveAll);
        Runtime.getRuntime().addShutdownHook(shutdownSaveThread);

        System.out.println("Successfully loaded TicketBot.");

        // Role "Ticket Bot Manager" is required for managing the tickets
    }

    /**
     * Load (reload) everything and everything.
     */
    public static void loadAll() {
        Ticket.tickets.clear();
        Ticket.ticketsByUser.clear();
        TicketType.types.clear();

        if (data.getConfig().has("tickets")) {
            JsonArray tickets = data.getConfig().getAsJsonArray("tickets");
            for (JsonElement tkt : tickets) {
                JsonObject ticket = tkt.getAsJsonObject();
                Ticket object = Ticket.deserialize(ticket);
                Ticket.tickets.put(object.getChannel(), object);
                Ticket.ticketsByUser.put(object.getAuthor(), object);
            }
        }

        if (data.getConfig().has("ticketTypes")) {
            JsonArray ticketTypes = data.getConfig().getAsJsonArray("ticketTypes");
            for (JsonElement typ : ticketTypes) {
                JsonObject type = typ.getAsJsonObject();
                TicketType object = TicketType.deserialize(type);
                TicketType.types.put(object.getEmoji(), object);
            }
        }
    }

    /**
     * Save everything and everything.
     */
    public static void saveAll() {
        JsonArray tickets = new JsonArray();
        for (Ticket ticket : Ticket.tickets.values()) {
            tickets.add(ticket.serialize());
        }
        JsonArray ticketTypes = new JsonArray();
        for (TicketType type : TicketType.types.values()) {
            ticketTypes.add(type.serialize());
        }
        data.getConfig().remove("tickets");
        data.getConfig().remove("ticketTypes");
        data.getConfig().add("tickets", tickets);
        data.getConfig().add("ticketTypes", ticketTypes);
        data.save();
    }

    /**
     * Send a standardized error message to a channel.
     *
     * @param channel The channel to send message to
     * @param error   The error
     */
    public static void sendErrorMessage(MessageChannel channel, String error) {
        channel.sendMessage(
                new EmbedBuilder()
                        .setTitle(TicketBot.config.stepTypesErrorTitle())
                        .setDescription(error)
                        .setColor(TicketBot.config.getErrorColor())
                        .build()
        ).queue(m -> {
            if (config.stepTypesDeleteErrorMsg()) {
                m.delete().queueAfter(config.stepTypesDeleteErrorMsgDelay(), TimeUnit.SECONDS);
            }
        });
    }

    /**
     * Register a step type so that the guild owner will
     * be able to use it when configuring steps.
     *
     * @param clazz The class of the step type
     */
    public static void registerStepType(Class<? extends TicketStepType<?>> clazz) {
        stepTypes.add(clazz);
    }
}
