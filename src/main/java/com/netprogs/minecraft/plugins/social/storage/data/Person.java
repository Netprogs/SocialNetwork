package com.netprogs.minecraft.plugins.social.storage.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.netprogs.minecraft.plugins.social.SocialPerson.Gender;
import com.netprogs.minecraft.plugins.social.SocialPerson.Status;
import com.netprogs.minecraft.plugins.social.SocialPerson.WaitState;
import com.netprogs.minecraft.plugins.social.SocialPerson.WeddingVows;
import com.netprogs.minecraft.plugins.social.command.ISocialNetworkCommand.ICommandType;
import com.netprogs.minecraft.plugins.social.command.SocialNetworkCommandType;
import com.netprogs.minecraft.plugins.social.storage.IMessage;

/*
 * Copyright (C) 2012 Scott Milne
 * 
 * "Social Network" is a Craftbukkit Minecraft server modification plug-in. It attempts to add a 
 * social environment to your server by allowing players to be placed into different types of social groups.
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

public class Person {

    private String name;

    private Gender gender;
    private Status socialStatus;

    // <MessageClassName, <PlayerName, List<MessageInstance>>>
    private Map<String, Map<String, List<? extends IMessage>>> messageQueue =
            new HashMap<String, Map<String, List<? extends IMessage>>>();

    private Map<String, Friend> friends = new HashMap<String, Friend>();
    private Map<String, Relationship> relationships = new HashMap<String, Relationship>();
    private Map<String, Affair> affairs = new HashMap<String, Affair>();
    private Map<String, Child> children = new HashMap<String, Child>();

    private List<String> ignoreList = new ArrayList<String>();

    private String childOf;

    private Engagement engagement;

    private Marriage marriage;
    private WeddingVows weddingVows;

    private Divorce divorce;

    private boolean lawyer;
    private boolean priest;

    // we can't save this type so we need to store it as a string instead and convert back later
    private transient ICommandType waitCommand;
    private String waitCommandType;
    private WaitState waitState;
    private IMessage waitData;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Status getSocialStatus() {
        return socialStatus;
    }

    public void setSocialStatus(Status socialStatus) {
        this.socialStatus = socialStatus;
    }

    public Map<String, Friend> getFriends() {
        return friends;
    }

    public Map<String, Relationship> getRelationships() {
        return relationships;
    }

    public Map<String, Affair> getAffairs() {
        return affairs;
    }

    public Map<String, Child> getChildren() {
        return children;
    }

    public void setChildren(Map<String, Child> children) {
        this.children = children;
    }

    public String getChildOf() {
        return childOf;
    }

    public void setChildOf(String childOf) {
        this.childOf = childOf;
    }

    public Engagement getEngagement() {
        return engagement;
    }

    public void setEngagement(Engagement engagement) {
        this.engagement = engagement;
    }

    public Marriage getMarriage() {
        return marriage;
    }

    public void setMarriage(Marriage marriage) {
        this.marriage = marriage;
    }

    public WeddingVows getWeddingVows() {
        return weddingVows;
    }

    public void setWeddingVows(WeddingVows weddingVows) {
        this.weddingVows = weddingVows;
    }

    public Divorce getDivorce() {
        return divorce;
    }

    public void setDivorce(Divorce divorce) {
        this.divorce = divorce;
    }

    public boolean isLawyer() {
        return lawyer;
    }

    public void setLawyer(boolean lawyer) {
        this.lawyer = lawyer;
    }

    public boolean isPriest() {
        return priest;
    }

    public void setPriest(boolean priest) {
        this.priest = priest;
    }

    public void setWaitCommand(ICommandType waitCommand) {
        this.waitCommand = waitCommand;
    }

    public ICommandType getWaitCommand() {
        if (waitCommandType != null) {
            return SocialNetworkCommandType.valueOf(waitCommandType);
        }
        return waitCommand;
    }

    public WaitState getWaitState() {
        return waitState;
    }

    public void setWaitState(WaitState waitState) {
        this.waitState = waitState;
    }

    @SuppressWarnings("unchecked")
    public <U extends IMessage> U getWaitData() {
        return (U) waitData;
    }

    public <U extends IMessage> void setWaitData(U waitData) {
        this.waitData = waitData;
    }

    public Map<String, Map<String, List<? extends IMessage>>> getMessageQueue() {
        return messageQueue;
    }

    public List<String> getIgnoreList() {
        return ignoreList;
    }

    public void setIgnoreList(List<String> ignoreList) {
        this.ignoreList = ignoreList;
    }
}
