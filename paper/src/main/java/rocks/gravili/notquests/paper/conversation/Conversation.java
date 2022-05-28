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

package rocks.gravili.notquests.paper.conversation;


import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.Nullable;
import rocks.gravili.notquests.paper.NotQuests;
import rocks.gravili.notquests.paper.managers.data.Category;
import rocks.gravili.notquests.paper.managers.integrations.citizens.QuestGiverNPCTrait;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

public class Conversation {
    private final NotQuests main;
    private final YamlConfiguration config;
    private final File configFile;

    private final String identifier;
    private CopyOnWriteArrayList<Integer> npcIDs; //-1: no NPC
    private final ArrayList<ConversationLine> start;

    final ArrayList<Speaker> speakers;

    private Category category;



    public Conversation(final NotQuests main, final File configFile, final YamlConfiguration config, final String identifier, @Nullable final ArrayList<Integer> npcIDsToAdd, final Category category) {
        this.main = main;
        this.configFile = configFile;
        this.config = config;
        this.identifier = identifier;
        npcIDs = new CopyOnWriteArrayList<>();
        if(npcIDsToAdd != null){
            for(int npcID : npcIDsToAdd){
                addNPC(npcID);
            }
        }
        start = new ArrayList<>();
        speakers = new ArrayList<>();
        this.category = category;
    }

    public final Category getCategory() {
        return category;
    }

    public void setCategory(final Category category) {
        this.category = category;
    }

    public final ArrayList<Speaker> getSpeakers() {
        return speakers;
    }

    public final boolean hasSpeaker(final Speaker speaker) {
        if (speakers.contains(speaker)) {
            return true;
        }
        for (final Speaker speakerToCheck : speakers) {
            if (speakerToCheck.getSpeakerName().equalsIgnoreCase(speaker.getSpeakerName())) {
                return true;
            }
        }
        return false;
    }

    public final boolean removeSpeaker(final Speaker speaker, final boolean save) {
        if (!hasSpeaker(speaker)) {
            return false;
        }

        speakers.remove(speaker);
        if (save) {
            if (configFile == null || config == null) {
                return false;
            }
            if (config.get(speaker.getSpeakerName()) != null) {
                return false;
            }
            config.set("Lines." + speaker.getSpeakerName(), null);
            try {
                config.save(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                main.getLogManager().severe("There was an error saving the configuration of Conversation <highlight>" + identifier + "</highlight>.");
                return false;
            }
        } else {
            return true;
        }
    }


    public boolean addSpeaker(final Speaker speaker, final boolean save) {
        if (hasSpeaker(speaker)) {
            return false;
        }

        speakers.add(speaker);
        if (save) {
            if (configFile == null || config == null) {
                return false;
            }
            if (config.get(speaker.getSpeakerName()) != null) {
                return false;
            }
            config.set("Lines." + speaker.getSpeakerName() + ".color", speaker.getColor());
            try {
                config.save(configFile);
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                main.getLogManager().severe("There was an error saving the configuration of Conversation <highlight>" + identifier + "</highlight>.");
                return false;
            }
        } else {
            return true;
        }

    }

    public final YamlConfiguration getConfig() {
        return config;
    }

    public void bindToAllCitizensNPCs(){
        for(int npcID : npcIDs){
            bindToCitizensNPC(npcID);
        }
    }
    public void bindToCitizensNPC(int npcID) {
        if (npcID < 0) {
            return;
        }
        if (!main.getIntegrationsManager().isCitizensEnabled()) {
            main.getLogManager().warn("The binding to NPC in Conversation <highlight>" + identifier + "</highlight> has been cancelled, because the Citizens plugin is not installed on this server. You will need the Citizens plugin to do NPC stuff.");
            return;
        }


        final NPC npc = CitizensAPI.getNPCRegistry().getById(npcID);
        if (npc != null) {
            boolean hasTrait = false;
            for (Trait trait : npc.getTraits()) {
                if (trait.getName().contains("questgiver")) {
                    hasTrait = true;
                    break;
                }
            }
            if (!npc.hasTrait(QuestGiverNPCTrait.class) && !hasTrait) {
                main.getLogManager().info("Trying to add Conversation <highlight>" + identifier + "</highlight> to NPC with ID <highlight>" + npc.getId() + "</highlight>...");

                npc.addTrait(QuestGiverNPCTrait.class);
            }
        }


    }

    public final String getIdentifier() {
        return identifier;
    }

    public final CopyOnWriteArrayList<Integer> getNPCIDs() {
        return npcIDs;
    }

    public final boolean hasCitizensNPC() {
        return npcIDs.size() > 0 && npcIDs.get(0) != -1;
    }

    public void addStarterConversationLine(final ConversationLine conversationLine) {
        start.add(conversationLine);
    }


    public final ArrayList<ConversationLine> getStartingLines() {
        return start;
    }


    public void addNPC(int npcID) {
        this.npcIDs.add(npcID);
        bindToCitizensNPC(npcID);

        if (configFile == null || config == null) {
            return;
        }
        config.set("npcIDs", npcIDs);
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
            main.getLogManager().severe("There was an error saving the configuration of Conversation <highlight>" + identifier + "</highlight>.");
        }

    }


    public void switchCategory(final Category category) {


        getCategory().getConversationsConfigs().remove(config);
        setCategory(category);
        category.getConversationsConfigs().add(config);

        if(!configFile.renameTo(new File(category.getConversationsFolder(), configFile.getName()))){
            main.getLogManager().severe("There was an error changing the category of conversation <highlight>" + getIdentifier() + "</highlight>. The conversation file could not be moved.");
        }


    }


}
