/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2021-2022 Alessio Gravili
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package rocks.gravili.notquests.spigot.structs;


import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.CompletedQuest;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.commands.NotQuestColors;
import rocks.gravili.notquests.spigot.events.notquests.QuestCompletedEvent;
import rocks.gravili.notquests.spigot.events.notquests.QuestFinishAcceptEvent;
import rocks.gravili.notquests.spigot.events.notquests.QuestPointsChangeEvent;
import rocks.gravili.notquests.spigot.structs.actions.Action;
import rocks.gravili.notquests.spigot.structs.conditions.Condition;
import rocks.gravili.notquests.spigot.structs.objectives.OtherQuestObjective;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * The QuestPlayer Object is initialized for every player, once they join the server - loading its data from the database.
 * It contains all kinds of player data for that player, like active quests, completed quests and quest points.
 * Completed Quests are saved too, for things like Quest History (Future feature), or handling the maxAccepts per quest /
 * the quest cooldown.
 *
 * @author Alessio Gravili
 */
public class QuestPlayer {
    private final NotQuests main;

    private final UUID uuid;


    private final ArrayList<ActiveQuest> activeQuests, activeQuestsCopy;
    private final ArrayList<ActiveQuest> questsToComplete;
    private final ArrayList<ActiveQuest> questsToRemove;
    private final ArrayList<CompletedQuest> completedQuests; //has to accept multiple entries of the same value
    private long questPoints;

    public QuestPlayer(NotQuests main, UUID uuid) {
        this.main = main;
        this.uuid = uuid;
        activeQuests = new ArrayList<>();
        activeQuestsCopy = new ArrayList<>();
        questsToComplete = new ArrayList<>();
        questsToRemove = new ArrayList<>();
        completedQuests = new ArrayList<>();
    }

    public String addActiveQuest(final ActiveQuest quest, final boolean triggerAcceptQuestTrigger, final boolean sendQuestInfo) {
        //Configuration Option: general.max-active-quests-per-player
        if (main.getConfiguration().getMaxActiveQuestsPerPlayer() != -1 && activeQuests.size() >= main.getConfiguration().getMaxActiveQuestsPerPlayer()) {
            return "<RED>You can not accept more than <AQUA>" + main.getConfiguration().getMaxActiveQuestsPerPlayer() + "</AQUA> Quests.";
        }

        for (ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest.getQuest())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted", getPlayer());
            }
        }
        int completedAmount = 0;

        long mostRecentAcceptTime = 0;
        for (CompletedQuest completedQuest : completedQuests) {
            if (completedQuest.getQuest().equals(quest.getQuest())) {
                completedAmount += 1;
                if (completedQuest.getTimeCompleted() > mostRecentAcceptTime) {
                    mostRecentAcceptTime = completedQuest.getTimeCompleted();
                }
            }
        }

        final long acceptTimeDifference = System.currentTimeMillis() - mostRecentAcceptTime;
        final long acceptTimeDifferenceMinutes = TimeUnit.MILLISECONDS.toMinutes(acceptTimeDifference);


        final long timeToWaitInMinutes = quest.getQuest().getAcceptCooldown() - acceptTimeDifferenceMinutes;
        final double timeToWaitInHours = Math.round((timeToWaitInMinutes / 60f) * 10) / 10.0;
        final double timeToWaitInDays = Math.round((timeToWaitInHours / 24f) * 10) / 10.0;


        //Max Accepts:
        if (quest.getQuest().getMaxAccepts() <= -1 || completedAmount < quest.getQuest().getMaxAccepts()) {
            //Cooldown:
            if (acceptTimeDifferenceMinutes >= quest.getQuest().getAcceptCooldown()) {

                //Requirements
                StringBuilder requirementsStillNeeded = new StringBuilder();

                if (getPlayer() == null) {
                    requirementsStillNeeded.append("\n<YELLOW>Error: Player object not found. Please report this to the plugin developer.");
                }

                for (final Condition condition : quest.getQuest().getRequirements()) {
                    final String check = condition.check(this, false);
                    if (!check.isBlank()) {
                        requirementsStillNeeded.append("\n").append(check);

                    }
                }


                if (!requirementsStillNeeded.toString().isBlank()) {
                    return "<RED>You do not fulfill all the requirements this quest needs! Requirement still needed:" + requirementsStillNeeded;
                }else{
                    //Now loop through all the requirements again in order to enforce them
                    for (final Condition condition : quest.getQuest().getRequirements()) {
                        condition.check(this, true);
                    }
                }





                finishAddingQuest(quest, triggerAcceptQuestTrigger, false);
                if (sendQuestInfo) {
                    final Player player = getPlayer();
                    if (player != null) {
                        Audience audience = main.adventure().player(player);

                        audience.sendMessage(MiniMessage.miniMessage().deserialize(
                                main.getLanguageManager().getString("chat.objectives-label-after-quest-accepting", player)
                        ));
                        main.getQuestManager().sendActiveObjectivesAndProgress(player, quest);

                        if (main.getConfiguration().visualTitleQuestSuccessfullyAccepted_enabled) {

                            audience.showTitle(
                                    Title.title(MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-accepted.title", player)),
                                            MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-accepted.subtitle", player, this, quest)),
                                            Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                                    ));
                        }


                        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.MASTER, 100, 2);

                    }

                }


                return "accepted";
            } else {
                if (timeToWaitInMinutes < 60) {
                    return "<RED>This quest is on a cooldown! You have to wait another <AQUA>" + timeToWaitInMinutes + " minutes</AQUA> until you can take it again.";
                } else {
                    if (timeToWaitInHours < 24) {
                        if (timeToWaitInHours == 1) {
                            return "<RED>This quest is on a cooldown! You have to wait another <AQUA>" + timeToWaitInHours + " hour</AQUA> until you can take it again.";

                        } else {
                            return "<RED>This quest is on a cooldown! You have to wait another <AQUA>" + timeToWaitInHours + " hours</AQUA> until you can take it again.";
                        }
                    } else {
                        if (timeToWaitInDays == 1) {
                            return "<RED>This quest is on a cooldown! You have to wait another <AQUA>" + timeToWaitInDays + " day</AQUA> until you can take it again.";

                        } else {
                            return "<RED>This quest is on a cooldown! You have to wait another <AQUA>" + timeToWaitInDays + " days</AQUA> until you can take it again.";
                        }
                    }
                }
            }

        } else {
            return "<RED>You have finished this quests too many times already. You can only accept it <AQUA>" + quest.getQuest().getMaxAccepts() + "</AQUA> times, but you have already accepted it <AQUA>" + completedAmount + "</AQUA> times.";
        }


    }


    private void finishAddingQuest(final ActiveQuest activeQuest, boolean triggerAcceptQuestTrigger, final boolean sendUpdateObjectivesUnlocked) {
        QuestFinishAcceptEvent questFinishAcceptEvent = new QuestFinishAcceptEvent(this, activeQuest, triggerAcceptQuestTrigger);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questFinishAcceptEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questFinishAcceptEvent);
        }

        if (questFinishAcceptEvent.isCancelled()) {
            return;
        }


        activeQuests.add(activeQuest);
        activeQuestsCopy.add(activeQuest);


        activeQuest.updateObjectivesUnlocked(sendUpdateObjectivesUnlocked, triggerAcceptQuestTrigger);


    }

    public String forceAddActiveQuest(final ActiveQuest quest, boolean triggerAcceptQuestTrigger) { //ignores max amount, cooldown and requirements
        for (ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest.getQuest())) {
                return main.getLanguageManager().getString("chat.quest-already-accepted", getPlayer());
            }
        }
        finishAddingQuest(quest, triggerAcceptQuestTrigger, false);
        return "<GREEN>Successfully accepted the quest (Forced).";
    }

    public final UUID getUUID() {
        return uuid;
    }

    public final ArrayList<ActiveQuest> getActiveQuests() {
        return activeQuests;
    }

    /*public void updateQuestStatus(){
        for(ActiveQuest activeQuest : activeQuests){
            activeQuest.updateQuestStatus();
            if(activeQuest.isCompleted()){
                giveReward(activeQuest.getQuest());
                questsToRemove.add(activeQuest);
            }
        }
        activeQuests.removeAll(questsToRemove);
        for(ActiveQuest activeQuest2 : questsToRemove) {
            completedQuests.add(new CompletedQuest(activeQuest2.getQuest(), this));
        }

        questsToRemove.clear();
    }*/

    public void giveReward(Quest quest) {
        main.getLogManager().debug("QuestPlayer.giveReward(). Quest: " + quest.getQuestName());
        for (Action action : quest.getRewards()) {
            main.getActionManager().executeActionWithConditions(action, this, null, true, quest);
        }

        final Player player = getPlayer();
        if (player != null) {
            Audience audience = main.adventure().player(player);
            audience.sendMessage(MiniMessage.miniMessage().deserialize(
                    main.getLanguageManager().getString("chat.quest-completed-and-rewards-given", getPlayer(), quest)
            ));
        }

    }

    public void sendMessage(String message) {
        final Player player = getPlayer();
        if (player != null) {
            Audience audience = main.adventure().player(player);
            audience.sendMessage(MiniMessage.miniMessage().deserialize(message));
        }
    }

    public void sendDebugMessage(String message) {
        final Player player = getPlayer();
        if (player != null) {
            if (main.getQuestManager().isDebugEnabledPlayer(player)) {
                main.adventure().player(player).sendMessage(MiniMessage.miniMessage().deserialize(NotQuestColors.debugTitleGradient + "[NotQuests Debug]</gradient> " + NotQuestColors.debugGradient + message + "</gradient>"));
            }

        }
    }

    public final ArrayList<CompletedQuest> getCompletedQuests() {
        return completedQuests;
    }


    public void forceActiveQuestCompleted(ActiveQuest activeQuest) {
        QuestCompletedEvent questCompletedEvent = new QuestCompletedEvent(this, activeQuest, true);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questCompletedEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questCompletedEvent);
        }

        if (questCompletedEvent.isCancelled()) {
            return;
        }

        questsToComplete.add(activeQuest);

        completedQuests.add(new CompletedQuest(activeQuest.getQuest(), this));

        giveReward(activeQuest.getQuest());

        final Player player = getPlayer();
        if (player != null) {
            if (main.getConfiguration().visualTitleQuestCompleted_enabled) {
                Audience audience = main.adventure().player(player);
                audience.showTitle(
                        Title.title(MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-completed.title", player)),
                                MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-completed.subtitle", player, this, activeQuest)),
                                Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                        ));

            }

            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100, 40);

        }


        for (ActiveQuest activeQuest2 : activeQuests) {
            for (ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof OtherQuestObjective) {
                    if (((OtherQuestObjective) (objective.getObjective())).getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1, -1);
                    }
                }
            }
        }
        removeCompletedQuests();
        //activeQuests.removeAll(questsToComplete);

    }

    public void notifyActiveQuestCompleted(ActiveQuest activeQuest) {
        QuestCompletedEvent questCompletedEvent = new QuestCompletedEvent(this, activeQuest, false);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questCompletedEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questCompletedEvent);
        }

        if (questCompletedEvent.isCancelled()) {
            return;
        }

        if (activeQuest.isCompleted()) {
            //Add to completed Quests list. This list will then be used in removeCompletedQuests() to remove all its contests also from the activeQuests lists
            //(Without a ConcurrentModificationException)
            questsToComplete.add(activeQuest);
            //We can safely (without ConcurrentModificationException) add it to the CompletedQuests list already without having to remove it from activeQuests
            completedQuests.add(new CompletedQuest(activeQuest.getQuest(), this));

            //Give Quest completion reward & show Quest completion title
            giveReward(activeQuest.getQuest());
            final Player player = getPlayer();
            if (player != null) {
                if (main.getConfiguration().visualTitleQuestCompleted_enabled) {
                    Audience audience = main.adventure().player(player);
                    audience.showTitle(
                            Title.title(MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-completed.title", player)),
                                    MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-completed.subtitle", player, this, activeQuest)),
                                    Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                            ));
                }
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.MASTER, 100, 40);
            }

        }

        //Handle OtherQuest Objectives for other Quests
        for (ActiveQuest activeQuest2 : activeQuests) {
            for (ActiveObjective objective : activeQuest2.getActiveObjectives()) {
                if (objective.getObjective() instanceof OtherQuestObjective otherQuestObjective) {
                    if (otherQuestObjective.getOtherQuest().equals(activeQuest.getQuest())) {
                        objective.addProgress(1);
                    }
                }
            }
        }
    }

    public void setQuestPoints(long newQuestPoints, boolean notifyPlayer) {
        if (newQuestPoints < 0) { //Prevent questPoints from going below 0
            newQuestPoints = 0;
        }
        QuestPointsChangeEvent questPointsChangeEvent = new QuestPointsChangeEvent(this, newQuestPoints);
        if (Bukkit.isPrimaryThread()) {
            Bukkit.getScheduler().runTaskAsynchronously(main.getMain(), () -> {
                Bukkit.getPluginManager().callEvent(questPointsChangeEvent);
            });
        } else {
            Bukkit.getPluginManager().callEvent(questPointsChangeEvent);
        }


        if (!questPointsChangeEvent.isCancelled()) {
            this.questPoints = questPointsChangeEvent.getNewQuestPointsAmount();


            if (notifyPlayer) {
                final Player player = getPlayer();
                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    audience.sendMessage(MiniMessage.miniMessage().deserialize(
                            "<YELLOW>Your quest points have been set to <AQUA>" + newQuestPoints + "</AQUA>."
                    ));
                }
            }
        }
    }

    public void addQuestPoints(long questPointsToAdd, boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() + questPointsToAdd, false);
        if (notifyPlayer) {
            final Player player = getPlayer();
            if (player != null) {
                Audience audience = main.adventure().player(player);
                audience.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<AQUA>+" + questPointsToAdd + " <GREEN>quest points!"
                ));
            }
        }
    }

    public void removeQuestPoints(final long questPointsToRemove, final boolean notifyPlayer) {
        setQuestPoints(getQuestPoints() - questPointsToRemove, false);
        if (notifyPlayer) {
            final Player player = getPlayer();
            if (player != null) {
                Audience audience = main.adventure().player(player);
                audience.sendMessage(MiniMessage.miniMessage().deserialize(
                        "<AQUA>>-" + questPointsToRemove + " <RED>>quest points!"
                ));
            }
        }
    }




    public final long getQuestPoints() {
        return questPoints;
    }

    public void removeCompletedQuests() {
        if (questsToComplete.size() == 0) {
            return;
        }
        activeQuests.removeAll(questsToComplete);
        activeQuestsCopy.removeAll(questsToComplete);

        questsToComplete.clear();
    }

    public void addCompletedQuest(final CompletedQuest completedQuest) {
        completedQuests.add(completedQuest);

    }


    public void failQuest(ActiveQuest activeQuestToFail) {
        final ArrayList<ActiveQuest> activeQuestsCopy = new ArrayList<>(activeQuests);
        for (ActiveQuest foundActiveQuest : activeQuestsCopy) {
            if (activeQuestToFail.equals(foundActiveQuest)) {

                foundActiveQuest.fail();
                questsToRemove.add(foundActiveQuest);
                final Player player = getPlayer();

                if (player != null) {
                    Audience audience = main.adventure().player(player);
                    if (main.getConfiguration().visualTitleQuestFailed_enabled) {
                        audience.showTitle(
                                Title.title(MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-failed.title", player)),
                                        MiniMessage.miniMessage().deserialize(main.getLanguageManager().getString("titles.quest-failed.subtitle", player, this, activeQuestToFail)),
                                        Title.Times.times(Duration.ofMillis(2), Duration.ofSeconds(3), Duration.ofMillis(8))
                                ));
                    }
                    player.playSound(player.getLocation(), Sound.ENTITY_RAVAGER_DEATH, SoundCategory.MASTER, 100, 1);
                }
            }
        }
        activeQuests.removeAll(questsToRemove);
        activeQuestsCopy.removeAll(questsToComplete);

        questsToComplete.clear();


    }

    public ArrayList<ActiveQuest> getActiveQuestsCopy() {
        return activeQuestsCopy;
    }

    public final boolean hasAcceptedQuest(final Quest quest) {
        for (final ActiveQuest activeQuest : activeQuests) {
            if (activeQuest.getQuest().equals(quest)) {
                return true;
            }
        }
        return false;
    }


    public final Player getPlayer(){
        return Bukkit.getPlayer(uuid);
    }

    public final ActiveQuest getActiveQuest(final Quest quest) {
        for(final ActiveQuest activeQuest : activeQuests){
            if(activeQuest.getQuest().equals(quest)){
                return activeQuest;
            }
        }
        return null;
    }
}
