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

package dev.dreta.ticketbot.utils;

import dev.dreta.ticketbot.TicketBot;
import dev.dreta.ticketbot.data.Ticket;
import dev.dreta.ticketbot.data.TicketStepData;
import dev.dreta.ticketbot.data.TicketStepType;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Category;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;

import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The data configuration is a subclass of Configuration
 * that provides convenient methods for accessing values
 * from the default config.json file.
 */
public class DataConfiguration extends Configuration {
    private Guild guild;

    /**
     * Get the token of the bot to run on.
     *
     * @return The token from the config
     */
    public String getToken() {
        return getConfig().get("token").getAsString();
    }

    /**
     * Get the character the user must type in before a command
     * so the bot will recognize it.
     *
     * @return -
     */
    public String getCommandPrefix() {
        return getConfig().get("commandPrefix").getAsString();
    }

    /**
     * Get the guild of this bot.
     * Only one guild is allowed per instance of this bot.
     * No details will be provided on the reason for this.
     * This method caches the guild for fast access.
     *
     * @return The guild from the config
     */
    public Guild getGuild() {
        if (guild != null) {
            return guild;
        }
        guild = TicketBot.jda.getGuildById(getConfig().get("guildId").getAsLong());
        return guild;
    }

    /**
     * Get the category to create the ticket channels in.
     *
     * @return The category from the config
     */
    public Category channelsTicketCategory() {
        return TicketBot.jda.getCategoryById(getConfig().getAsJsonObject("channels").get("categoryId").getAsLong());
    }

    /**
     * When specified, TicketBot will only listen to command
     * requests in the specific channel.
     *
     * @return -
     */
    public long botCommandsChannel() {
        return getConfig().get("botCommandsChannel").getAsLong();
    }

    /**
     * Get the name format of the channels to create for tickets.
     * <p>
     * Placeholders:
     * * {NAMEDISCRIM} Must be included, he name and discriminator of the
     * ticket's author, formatted as "dreta6665".
     * * {TICKETDISCRIM} Must be included, set to a unique number in case
     * the user created multiple tickets.
     *
     * @return The name format from the config
     */
    public String channelsChannelFormat() {
        return getConfig().getAsJsonObject("channels").get("channelFormat").getAsString();
    }

    /**
     * Get the name format of the channels to perform management
     * commands in.
     * <p>
     * Placeholders:
     * * {NAMEDISCRIM} See above.
     *
     * @return -
     */
    public String channelsManageFormat() {
        return getConfig().getAsJsonObject("channels").get("manageFormat").getAsString();
    }

    /**
     * Get the topic of the channels to create for tickets.
     * <p>
     * Placeholders:
     * * {NAME} The username of the ticket's author
     * * {NICKNAME} The nickname of the ticket's author
     * * {DISCRIM} The discriminator (the number after the hashtag) of the ticket's author
     *
     * @return The name format from the config
     */
    public String channelsChannelTopic() {
        return getConfig().getAsJsonObject("channels").get("channelTopic").getAsString();
    }

    /**
     * Get the question message when we are asking for the ticket title.
     *
     * @return The title message
     */
    public String ticketTitleMsg() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("title").get("title").getAsString();
    }

    /**
     * Get the title for the embed to send when we've successfully
     * created a ticket.
     *
     * @return -
     */
    public String ticketEndTitleMsg() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("end").get("title").getAsString();
    }

    /**
     * Get the description for the embed to send when we've
     * successfully created a ticket.
     *
     * @return -
     */
    public String ticketEndDescriptionMsg() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("end").get("description").getAsString();
    }

    /**
     * Get the maximum length of ticket titles.
     *
     * @return -
     */
    public int ticketTitleMaxLength() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("title").get("maxLength").getAsInt();
    }

    /**
     * Get the roles that can see the ticket channel.
     * The ticket author can always see the ticket channel.
     *
     * @return The roles
     */
    public List<Role> channelsAllowedRoles() {
        return ((List<Long>) TicketBot.gson.fromJson(
                getConfig().getAsJsonObject("channels").getAsJsonArray("allowedRoles"),
                Ticket.LONG_LIST_TYPE))
                .stream().map(l -> getGuild().getRoleById(l)).collect(Collectors.toList());
    }

    /**
     * Get the permission to assign to the channel for those
     * that can see the ticket channel.
     * These permissions are also granted to the ticket author.
     *
     * @return The permissions
     */
    public List<Permission> channelsPermissions() {
        return ((List<String>) TicketBot.gson.fromJson(
                getConfig().getAsJsonObject("channels").getAsJsonArray("permissions"),
                TicketStepData.STRING_LIST_TYPE))
                .stream().map(Permission::valueOf).collect(Collectors.toList());
    }

    /**
     * Get the accent color that will be used
     * in the embeds.
     *
     * @return The accent color from the config
     */
    public Color getAccentColor() {
        return Color.decode("0x" + getConfig().get("accentColor").getAsString().replace("#", ""));
    }

    /**
     * Get the emoji that represents "Yes" in {@link dev.dreta.ticketbot.data.types.BooleanStepType}.
     * Does not include :
     *
     * @return The yes emoji.
     */
    public String booleanYesEmoji() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("boolean").get("yes").getAsString();
    }

    /**
     * Get the emoji that represents "No" in {@link dev.dreta.ticketbot.data.types.BooleanStepType}.
     * Does not include :
     *
     * @return The no emoji.
     */
    public String booleanNoEmoji() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("boolean").get("no").getAsString();
    }

    /**
     * Get the information message for the {@link dev.dreta.ticketbot.data.types.BooleanStepType}.
     *
     * @return -
     */
    public String booleanInfoMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("boolean").get("info").getAsString();
    }

    /**
     * Get whether the bot should automatically delete
     * messages it sent.
     *
     * @return -
     */
    public boolean autoDeleteMessages() {
        return getConfig().get("deleteMessages").getAsBoolean();
    }

    /**
     * Get the color for the embed message when an error occurs.
     *
     * @return -
     */
    public Color getErrorColor() {
        return Color.decode("0x" + getConfig().get("errorColor").getAsString().replace("#", ""));
    }

    /**
     * Get the title for the embed to use when an error occurs.
     *
     * @return -
     */
    public String stepTypesErrorTitle() {
        return getConfig().getAsJsonObject("stepTypes").get("errorTitle").getAsString();
    }

    /**
     * Get the error message to send when the input isn't a double.
     *
     * @return -
     */
    public String doubleFormatErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("double").get("formatErrorMsg").getAsString();
    }

    /**
     * Get the error message to send when the user input is smaller than
     * the minimum.
     *
     * @return -
     */
    public String doubleMinErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("double").get("minMsg").getAsString();
    }

    /**
     * Get the error message to send when the user input is larger than
     * the maximum.
     *
     * @return -
     */
    public String doubleMaxErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("double").get("maxMsg").getAsString();
    }

    /**
     * Get the error message to send when the input isn't an integer.
     *
     * @return -
     */
    public String integerFormatErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("integer").get("formatErrorMsg").getAsString();
    }

    /**
     * Get the error message to send when the user input is smaller than
     * the minimum.
     *
     * @return -
     */
    public String integerMinErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("integer").get("minMsg").getAsString();
    }

    /**
     * Get the error message to send when the user input is larger than
     * the maximum.
     *
     * @return -
     */
    public String integerMaxErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("integer").get("maxMsg").getAsString();
    }

    /**
     * Get the emoji that will end the input in {@link dev.dreta.ticketbot.data.types.ListStepType}.
     *
     * @return -
     */
    public String listEndEmoji() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("endEmoji").getAsString();
    }

    /**
     * {@link dev.dreta.ticketbot.data.types.ListStepType} automatically
     * updates the message to show all the items inside the list. This
     * string specifies how that should be displayed.
     *
     * @return -
     */
    public String listItemsFormat() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("listItemsFormat").getAsString();
    }

    /**
     * {@link dev.dreta.ticketbot.data.types.ListStepType} automatically
     * updates the message to show all the items inside the list. This
     * string specifies how individual items should be displayed.
     *
     * @return -
     */
    public String listItemFormat() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("listItemFormat").getAsString();
    }

    /**
     * {@link dev.dreta.ticketbot.data.types.ListStepType} automatically
     * updates the message to show all the items inside the list. This
     * string specifies what should be displayed when the list is empty.
     *
     * @return -
     */
    public String listEmptyFormat() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("listEmptyFormat").getAsString();
    }

    /**
     * Get the emoji that will delete the last input in {@link dev.dreta.ticketbot.data.types.ListStepType}.
     *
     * @return -
     */
    public String listDeleteLastEmoji() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("deleteLastEmoji").getAsString();
    }

    /**
     * Get the information message for the {@link dev.dreta.ticketbot.data.types.ListStepType}.
     *
     * @return -
     */
    public String listInfoMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("info").getAsString();
    }

    /**
     * Get the message to send when the entered list's size
     * exceeds the maximum length.
     *
     * @return -
     */
    public String listLengthErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("lengthMsg").getAsString();
    }

    /**
     * Get the message to send when the entered string's character count
     * exceeds the maximum length.
     *
     * @return -
     */
    public String stringLengthErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("string").get("lengthMsg").getAsString();
    }

    /**
     * Get the message to send when the must be true option is true,
     * but the user selected no.
     *
     * @return Must be true message
     */
    public String booleanMustBeTrueMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("boolean").get("mustBeTrueMsg").getAsString();
    }

    /**
     * Get the message to send when the must be false option is true,
     * but the user selected yes.
     *
     * @return Must be false message
     */
    public String booleanMustBeFalseMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("boolean").get("mustBeFalseMsg").getAsString();
    }

    /**
     * Get the error message to send when the list is empty.
     *
     * @return -
     */
    public String listEmptyListError() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("emptyListErrorMsg").getAsString();
    }

    /**
     * Get the error message to send when the list is empty and the user tries to
     * delete the last input.
     *
     * @return -
     */
    public String listDeleteLastEmptyListErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("list").get("deleteLastEmptyListErrorMsg").getAsString();
    }

    /**
     * Returns whether the error messages should be automatically
     * designed after some time.
     *
     * @return -
     */
    public boolean stepTypesDeleteErrorMsg() {
        return getConfig().getAsJsonObject("stepTypes").get("deleteErrorMessages").getAsBoolean();
    }

    /**
     * Returns the delay in seconds to delete error messages after. Will have no
     * effect if {@link #stepTypesDeleteErrorMsg()} is false.
     *
     * @return -
     */
    public int stepTypesDeleteErrorMsgDelay() {
        return getConfig().getAsJsonObject("stepTypes").get("deleteErrorMsgsDelay").getAsInt();
    }

    /**
     * Get the information message for the {@link dev.dreta.ticketbot.data.types.SingleSelectStepType}.
     *
     * @return -
     */
    public String selectOneInfoMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .getAsJsonObject("one").get("info").getAsString();
    }

    /**
     * Get the message for displaying the possible options in SelectStepType's.
     *
     * @return -
     */
    public String selectOptionsMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .get("optionsMsg").getAsString();
    }

    /**
     * Get the option format for individual options when displaying possible
     * options.
     *
     * @return -
     */
    public String selectOptionFormat() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .get("optionFormat").getAsString();
    }

    /**
     * Get the information message for the {@link dev.dreta.ticketbot.data.types.MultiSelectStepType}.
     *
     * @return -
     */
    public String selectMultiInfoMsg() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .getAsJsonObject("multi").get("info").getAsString();
    }

    /**
     * Get the emoji that will end the input in {@link dev.dreta.ticketbot.data.types.MultiSelectStepType}.
     *
     * @return -
     */
    public String selectMultiEndEmoji() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .getAsJsonObject("multi").get("endEmoji").getAsString();
    }

    /**
     * Get the error message to send when no options are selected.
     *
     * @return -
     */
    public String selectMultiEmptyError() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .getAsJsonObject("multi").get("emptyListErrorMsg").getAsString();
    }

    /**
     * Get the error message to send when the options selected exceed
     * the maximum length.
     *
     * @return -
     */
    public String selectMultiLengthError() {
        return getConfig().getAsJsonObject("stepTypes").getAsJsonObject("selection")
                .getAsJsonObject("multi").get("lengthMsg").getAsString();
    }

    /**
     * Get what message to send when prompting the guild member
     * to select a TicketType.
     *
     * @return -
     */
    public String ticketTypeTitle() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("type").get("title").getAsString();
    }

    /**
     * Get how to format each of the TicketStep's
     * when prompting the guild member to select a TicketType.
     *
     * @return -
     */
    public String ticketTypeFormat() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("type").get("format").getAsString();
    }

    /**
     * Get the error message to send when you don't have the permissions
     * to manage the tickets.
     *
     * @return -
     */
    public String managePermissionError() {
        return getConfig().getAsJsonObject("manage").get("permissionError").getAsString();
    }

    /**
     * Get the title of the embed to send when choosing what ticket
     * to manage.
     *
     * @return -
     */
    public String manageTicketSelectTitle() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("select").get("title").getAsString();
    }

    /**
     * Get the description of the embed to send when choosing what ticket
     * to manage.
     *
     * @return -
     */
    public String manageTicketSelectDescription() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("select").get("description").getAsString();
    }

    /**
     * Get the error message to send when the message contains more or less than
     * one channel references when choosing what ticket to manage.
     *
     * @return -
     */
    public String manageTicketSelectError() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("select").get("error").getAsString();
    }

    /**
     * Get the emoji to open the ticket.
     *
     * @return -
     */
    public String manageTicketOpenEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").get("openEmoji").getAsString();
    }

    /**
     * Get the emoji to close the ticket.
     *
     * @return -
     */
    public String manageTicketCloseEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").get("closeEmoji").getAsString();
    }

    /**
     * Get the emoji to manage the assignees of the ticket.
     *
     * @return -
     */
    public String manageTicketAssigneesEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").get("assigneesEmoji").getAsString();
    }

    /**
     * Get the emoji to end the management process.
     *
     * @return -
     */
    public String manageTicketExitEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").get("exitEmoji").getAsString();
    }

    /**
     * Get the title of the embed to send when you
     * finished managing a ticket.
     *
     * @return -
     */
    public String manageTicketExitTitle() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("exit").get("title").getAsString();
    }

    /**
     * Get the description of the embed to send when you
     * finished managing a ticket.
     *
     * @return -
     */
    public String manageTicketExitDescription() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("exit").get("description").getAsString();
    }

    /**
     * Get the emoji to assign somebody to a ticket.
     *
     * @return -
     */
    public String manageTicketAssigneesAddEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("add").getAsString();
    }

    /**
     * Get the emoji to unassign somebody from a ticket.
     *
     * @return -
     */
    public String manageTicketAssigneesRemoveEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("remove").getAsString();
    }

    /**
     * Get the emoji to exit and return to the ticket data stage.
     *
     * @return -
     */
    public String manageTicketAssigneesExitEmoji() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("exit").getAsString();
    }

    /**
     * Get the message to send for prompting who to assign.
     *
     * @return -
     */
    public String manageTicketAssigneesAssignUser() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("assignUser").getAsString();
    }

    /**
     * Get the message to send for prompting who to unassign.
     *
     * @return -
     */
    public String manageTicketAssigneesUnassignUser() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("unassignUser").getAsString();
    }

    /**
     * Get the title of the informational embed to send
     * when somebody reopened the ticket.
     * <p>
     * Placeholders:
     * {USER}
     *
     * @return -
     */
    public String manageTicketTitleOpen() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("open").get("titleOpen").getAsString();
    }

    /**
     * Get the title of the informational embed to send
     * when somebody closed the ticket.
     * <p>
     * Placeholders:
     * {USER}
     *
     * @return =
     */
    public String manageTicketTitleClose() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("open").get("titleClose").getAsString();
    }

    /**
     * Get the title of the informational embed to send
     * when somebody assigned somebody to the tickete.
     * <p>
     * Placeholders:
     * {USER}
     * {ASSIGNEE}
     *
     * @return -
     */
    public String manageTicketTitleAssign() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assign").get("titleAssign").getAsString();
    }

    /**
     * Get the title of the informational embed to send
     * when somebody unassigned somebody to the tickete.
     * <p>
     * Placeholders:
     * {USER}
     * {ASSIGNEE}
     *
     * @return -
     */
    public String manageTicketTitleUnassign() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assign").get("titleUnassign").getAsString();
    }

    /**
     * Get the error message to send when the user haven't
     * mentioned any user in their response to which user
     * to assign/unassign.
     *
     * @return -
     */
    public String manageTicketAssigneesMentionInvalid() {
        return getConfig().getAsJsonObject("manage").getAsJsonObject("ticket").getAsJsonObject("assignees").get("mentionInvalid").getAsString();
    }

    /**
     * Get the title of the ticket data embed.
     * Placeholders:
     * {TITLE}
     * {AUTHORNAME}
     * {AUTHORDISCRIM}
     * {AUTHORNICKNAME} (The nickname of the channel in the guild)
     * {CHANNEL} (The name of the channel, without category, without hashtag)
     * {OPEN} (See {@link #ticketDataOpenYes()} and {@link #ticketDataOpenNo()})
     *
     * @return -
     */
    public String ticketDataTitle() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("title").getAsString();
    }

    /**
     * Get what to send if the ticket is open.
     * Context:
     * Is Open: Yes/No
     *
     * @return -
     */
    public String ticketDataOpenYes() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("openYes").getAsString();
    }

    /**
     * Get what to send if the ticket is closed.
     * Context:
     * Is Open: Yes/No
     *
     * @return -
     */
    public String ticketDataOpenNo() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("openNo").getAsString();
    }

    /**
     * Get the description of the ticket data embed.
     * Placeholders:
     * {TITLE}
     * {AUTHORNAME}
     * {AUTHORDISCRIM}
     * {AUTHORNICKNAME} (The nickname of the channel in the guild)
     * {CHANNEL} (The name of the channel, without category, without hashtag)
     * {OPEN} (See {@link #ticketDataOpenYes()} and {@link #ticketDataOpenNo()})
     * {STEPS} (See {@link #ticketDataStep()})
     *
     * @return -
     */
    public String ticketDataDescription() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("description").getAsString();
    }

    /**
     * Get how to format each step for the ticket data embed
     * description.
     * Placeholders:
     * {INDEX} (The number of the step)
     * {STEPTITLE}
     * {STEPTYPE} (See {@link TicketStepType#getName()}}
     * {STEPANSWER}
     *
     * @return -
     */
    public String ticketDataStep() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("step").getAsString();
    }

    /**
     * Get the title of the ticket assignees embed.
     * Placeholders: None
     *
     * @return -
     */
    public String ticketDataAssigneesTitle() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("assigneesTitle").getAsString();
    }

    /**
     * Get how to format each of the assignees.
     * All the assignees will be listed in this format one on each line.
     * Placeholders:
     * {INDEX}
     * {NAME}
     * {DISCRIM}
     * {NICKNAME}
     *
     * @return -
     */
    public String ticketDataAssignee() {
        return getConfig().getAsJsonObject("ticket").getAsJsonObject("data").get("assignee").getAsString();
    }
}
