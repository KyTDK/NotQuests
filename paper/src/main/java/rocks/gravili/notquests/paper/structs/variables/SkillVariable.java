/*
 * NotQuests - A Questing plugin for Minecraft Servers
 * Copyright (C) 2022 Alessio Gravili
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

package rocks.gravili.notquests.paper.structs.variables;

import cloud.commandframework.arguments.standard.StringArgument;
import com.neostorm.neostorm.Api;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.structs.QuestPlayer;

import java.util.ArrayList;
import java.util.List;

public class SkillVariable extends Variable<Integer> {

    public SkillVariable(NotQuests main) {
        super(main);

        addRequiredString(
                StringArgument.<CommandSender>newBuilder("Skill").withSuggestionsProvider(
                        (context, lastString) -> {
                            final List<String> allArgs = context.getRawInput();
                            main.getUtilManager().sendFancyCommandCompletion(context.getSender(), allArgs.toArray(new String[0]), "[Skill]", "[...]");

                            ArrayList<String> suggestions = new ArrayList<>();
                            suggestions.add("<Enter Skill>");
                            return suggestions;

                        }
                ).single().build()
        );
    }

    @Override
    public Integer getValue(QuestPlayer questPlayer, Object... objects) {
        return Api.getStats(questPlayer.getPlayer(), getRequiredStringValue("Skill"));
    }

    @Override
    public boolean setValueInternally(Integer newValue, QuestPlayer questPlayer, Object... objects) {
        return true;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return List.of(Api.getStatTable());
    }

    @Override
    public String getPlural() {
        return "Skills";
    }

    @Override
    public String getSingular() {
        return "Skill";
    }
}
