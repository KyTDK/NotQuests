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
import java.util.Arrays;
import java.util.List;

public class AllSkillsVariable extends Variable<Integer[]> {

    public AllSkillsVariable(NotQuests main) {
        super(main);
    }

    @Override
    public Integer[] getValue(QuestPlayer questPlayer, Object... objects) {
        List<Integer> skills = new ArrayList<>();
        for (String skill : Api.getStatTable()) {
           skills.add(Api.getStats(questPlayer.getPlayer(), skill));
        }
        Integer[] stockArr = new Integer[Api.getStatTable().length];
        stockArr = skills.toArray(stockArr);
        return stockArr;
    }

    @Override
    public boolean setValueInternally(Integer[] newValue, QuestPlayer questPlayer, Object... objects) {
        return false;
    }


    @Override
    public List<String> getPossibleValues(QuestPlayer questPlayer, Object... objects) {
        return null;
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
