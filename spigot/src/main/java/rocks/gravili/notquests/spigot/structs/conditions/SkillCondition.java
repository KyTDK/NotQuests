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

package rocks.gravili.notquests.spigot.structs.conditions;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.meta.CommandMeta;
import cloud.commandframework.paper.PaperCommandManager;
import com.neostorm.neostorm.Api;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import rocks.gravili.notquests.spigot.NotQuests;
import rocks.gravili.notquests.spigot.structs.QuestPlayer;

public class SkillCondition extends Condition {

    private String skill;

    public SkillCondition(final NotQuests main) {
        super(main);
    }
    
    public void setSkill(final String skill){
        this.skill = skill;
    }

    public final long getSkillRequirement() {
        return getProgressNeeded();
    }


    public static void handleCommands(NotQuests main, PaperCommandManager<CommandSender> manager, Command.Builder<CommandSender> builder, ConditionFor conditionFor) {
        manager.command(builder.literal("Skill")
                .argument(IntegerArgument.<CommandSender>newBuilder("amount").withMin(1), ArgumentDescription.of("Level required"))
                .argument(IntegerArgument.<CommandSender>newBuilder("skill").withMin(1), ArgumentDescription.of("Skill"))
                .meta(CommandMeta.DESCRIPTION, "Adds a new Skill Requirement to a quest")
                .handler((context) -> {
                    final int amount = context.get("amount");
                    final String skill = context.get("skill");
                    SkillCondition skillCondition = new SkillCondition(main);
                    skillCondition.setProgressNeeded(amount);
                    skillCondition.setSkill(skill);

                    main.getConditionsManager().addCondition(skillCondition, context);
                }));
    }

    @Override
    public String getConditionDescription() {

        return "<GRAY>-- Level needed: " + getSkillRequirement();
    }

    @Override
    public void save(FileConfiguration configuration, String initialPath) {
    }

    @Override
    public void load(FileConfiguration configuration, String initialPath) {

    }


    @Override
    public String check(QuestPlayer questPlayer, boolean enforce) {

        final long skillRequirementAmount = getSkillRequirement();
        final Player player = questPlayer.getPlayer();
        if (player != null) {
             if (Api.getStats(player, skill) < skillRequirementAmount) {
                return "<YELLOW>You need <AQUA>" + (skillRequirementAmount - Api.getStats(player, skill) + "</AQUA> more levels.");
            } else {
                return "";
            }
        } else {
            return "<YELLOW>Error reading skill requirement...";

        }
    }
}
