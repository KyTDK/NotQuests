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

package rocks.gravili.notquests.paper.structs.objectives;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.paper.PaperCommandManager;
import com.neostorm.neostorm.Api;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.ActiveObjective;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SkillLevelObjective extends Objective {

    private String mobToKillType;
    private String nameTagContainsAny = "";
    private String nameTagEquals = "";

    public SkillLevelObjective(NotQuests main) {
        super(main);
    }

    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> addObjectiveBuilder) {
        addObjectiveBuilder = addObjectiveBuilder
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
                .argument(IntegerArgument.<CommandSender>newBuilder("level").withMin(1), ArgumentDescription.of("Level to go up"))
                .flag(main.getCommandManager().nametag_equals)
                .flag(main.getCommandManager().nametag_containsany);

        if(main.getIntegrationsManager().isProjectKorraEnabled()){
            addObjectiveBuilder = addObjectiveBuilder.flag(main.getCommandManager().withProjectKorraAbilityFlag);
        }

        addObjectiveBuilder = addObjectiveBuilder.handler((context) -> {

            final String skillName = context.get("skillName");
            final int amountToLevelUp = context.get("level");

            final String[] a = context.flags().getValue(main.getCommandManager().nametag_equals, new String[]{""});
            final String[] b = context.flags().getValue(main.getCommandManager().nametag_containsany, new String[]{""});
            final String nametag_equals = String.join(" ", a);
            final String nametag_containsany = String.join(" ", b);

            SkillLevelObjective SkillMasteryObjective = new SkillLevelObjective(main);

            SkillMasteryObjective.setMobToKillType(skillName);
            SkillMasteryObjective.setProgressNeeded(amountToLevelUp);

            //Add flags
            SkillMasteryObjective.setNameTagEquals(nametag_equals);
            SkillMasteryObjective.setNameTagContainsAny(nametag_containsany);


            main.getObjectiveManager().addObjective(SkillMasteryObjective, context);


            if (!nametag_equals.isBlank()) {
                context.getSender().sendMessage(main.parse(
                        "<main>With nametag_equals flag: <highlight>"
                                + nametag_equals + "</highlight>!"
                ));
            }
            if (!nametag_containsany.isBlank()) {
                context.getSender().sendMessage(main.parse(
                        "main>With nametag_containsany flag: <highlight>"
                                + nametag_containsany + "</highlight>!"
                ));
            }

        });

        manager.command(addObjectiveBuilder);
    }


    @Override
    public String getObjectiveTaskDescription(final QuestPlayer questPlayer) {
        return main.getLanguageManager().getString("chat.objectives.taskDescription.skillLevel.base", questPlayer, Map.of(
                "%SKILLTOLEVELUP%", getSkillToLevelUp()
        ));
    }

    public void setMobToKillType(final String mobToKillType) {
        this.mobToKillType = mobToKillType;
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
        configuration.set(initialPath + ".specifics.skillToLevelUp", getSkillToLevelUp());

        //Extra args
        if (!getNameTagContainsAny().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagContainsAny", getNameTagContainsAny());
        }
        if (!getNameTagEquals().isBlank()) {
            configuration.set(initialPath + ".extras.nameTagEquals", getNameTagEquals());
        }

    }

    @Override
    public void onObjectiveUnlock(final ActiveObjective activeObjective, final boolean unlockedDuringPluginStartupQuestLoadingProcess) {
    }
    @Override
    public void onObjectiveCompleteOrLock(final ActiveObjective activeObjective, final boolean lockedOrCompletedDuringPluginStartupQuestLoadingProcess, final boolean completed) {
    }



    public final String getSkillToLevelUp() {
        return mobToKillType;
    }

    public final long getAmountToLevelUp() {
        return super.getProgressNeeded();
    }


    //Extra args
    public final String getNameTagContainsAny() {
        return nameTagContainsAny;
    }

    public void setNameTagContainsAny(final String nameTagContainsAny) {
        this.nameTagContainsAny = nameTagContainsAny;
    }

    public final String getNameTagEquals() {
        return nameTagEquals;
    }

    public void setNameTagEquals(final String nameTagEquals) {
        this.nameTagEquals = nameTagEquals;
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {
        mobToKillType = configuration.getString(initialPath + ".specifics.skillToLevelUp");

        //Extras
        final String nameTagContains = configuration.getString(initialPath + ".extras.nameTagContainsAny", "");
        if (!nameTagContains.isBlank()) {
            setNameTagContainsAny(nameTagContains);
        }

        final String nameTagEquals = configuration.getString(initialPath + ".extras.nameTagEquals", "");
        if (!nameTagEquals.isBlank()) {
            setNameTagEquals(nameTagEquals);
        }
    }
}
