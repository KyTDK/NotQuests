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

package rocks.gravili.notquests.paper.structs.actions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import com.neostorm.neostorm.Api;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.commands.arguments.MaterialOrHandArgument;
import rocks.gravili.notquests.paper.commands.arguments.wrappers.MaterialOrHand;
import rocks.gravili.notquests.paper.managers.items.NQItem;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class GiveSkillLevelAction extends Action {

    private String nqSkill = "";
    private int nqLevelAmount = 1;

    public GiveSkillLevelAction(final NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ActionFor rewardFor) {
        manager.command(builder
                .argument(StringArgument.<CommandSender>newBuilder("skillName").withSuggestionsProvider((context, lastString) -> {
                    ArrayList<String> completions = new ArrayList<>();
                    for (String skill : Api.getStatTable()) {
                        completions.add("" + skill);
                    }
                    completions.add("skill");
                    final List<String> allArgs = context.getRawInput();
                    main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "['Skill']", "");

                    return completions;
                }).build(), ArgumentDescription.of("The name of the skill"))
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("How many levels to give"))
                .handler((context) -> {
                    final String skillName = context.get("skillName");
                    final int levelAmount = context.get("amount");
                    GiveSkillLevelAction giveSkillLevelAction = new GiveSkillLevelAction(main);
                    giveSkillLevelAction.setNqSkill(skillName);
                    giveSkillLevelAction.setNqItemAmount(levelAmount);
                    if (skillName.equalsIgnoreCase("any")) {
                        context.getSender().sendMessage(main.parse(
                                "<error>You cannot use <highlight>'any'</highlight> here!"
                        ));
                        return;
                    }

                    main.getActionManager().addAction(giveSkillLevelAction, context);
                }));
    }
    public void setNqSkill(final String nqItemName){
        this.nqSkill = nqItemName;
    }

    public void setNqItemAmount(final int nqItemAmount){
        this.nqLevelAmount = nqItemAmount;
    }
    public final String getNqSkill(){
        return nqSkill;
    }

    public final int getNqLevelAmount(){
        return nqLevelAmount;
    }

    @Override
    public void executeInternally(final QuestPlayer questPlayer, Object... objects) {
        if (getSkillReward() == null) {
            main.getLogManager().warn("Tried to give item reward with invalid reward item");
            return;
        }
        if (questPlayer.getPlayer() == null) {
            main.getLogManager().warn("Tried to give item reward with invalid player object");
            return;
        }
        int newLevel = Api.getStats(questPlayer.getPlayer(), getNqSkill()) + getNqLevelAmount();
        Api.setNewLevel(questPlayer.getPlayer(), getNqSkill(), newLevel);
        Api.setSkillPoints(questPlayer.getPlayer(), getNqSkill(), Api.getSkillPoints(questPlayer.getPlayer(), getNqSkill()) + getNqLevelAmount());
        Api.setGoalExp(questPlayer.getPlayer(), getNqSkill(), newLevel);
    }

    @Override
    public String getActionDescription(final QuestPlayer questPlayer, final Object... objects) {
        return "Skill levels: " + getSkillReward();
    }

    @Override
    public void save(final FileConfiguration configuration, String initialPath) {
            configuration.set(initialPath + ".specifics.nqSkill", getNqSkill());
            configuration.set(initialPath + ".specifics.nqLevelAmount", getNqLevelAmount());
    }


    public final String getSkillReward() {
        return nqSkill;
    }

    @Override
    public void load(final FileConfiguration configuration, String initialPath) {
        this.nqSkill = configuration.getString(initialPath + ".specifics.nqSkill", "");
        this.nqLevelAmount = configuration.getInt(initialPath + ".specifics.nqLevelAmount", 1);
    }

    @Override
    public void deserializeFromSingleLineString(ArrayList<String> arguments) {
        this.nqSkill = arguments.get(0);
    }
}