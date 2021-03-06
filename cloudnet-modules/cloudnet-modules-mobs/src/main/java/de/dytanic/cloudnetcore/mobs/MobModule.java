/*
 * Copyright (c) Tarek Hosni El Alaoui 2017
 */

package de.dytanic.cloudnetcore.mobs;

import de.dytanic.cloudnet.event.IEventListener;
import de.dytanic.cloudnet.lib.network.protocol.packet.PacketRC;
import de.dytanic.cloudnet.lib.server.ServerGroupMode;
import de.dytanic.cloudnetcore.api.CoreModule;
import de.dytanic.cloudnetcore.api.event.network.ChannelInitEvent;
import de.dytanic.cloudnetcore.api.event.network.UpdateAllEvent;
import de.dytanic.cloudnetcore.mobs.config.ConfigMobs;
import de.dytanic.cloudnetcore.mobs.database.MobDatabase;
import de.dytanic.cloudnetcore.mobs.packet.in.PacketInAddMob;
import de.dytanic.cloudnetcore.mobs.packet.in.PacketInRemoveMob;
import de.dytanic.cloudnetcore.mobs.packet.out.PacketOutMobSelector;
import de.dytanic.cloudnetcore.network.components.MinecraftServer;
import lombok.Getter;

/**
 * Created by Tareko on 16.10.2017.
 */
@Getter
public class MobModule extends CoreModule implements IEventListener<UpdateAllEvent> {

    private ConfigMobs configMobs;

    private MobDatabase mobDatabase;

    @Getter
    private static MobModule instance;

    @Override
    public void onLoad()
    {
        instance = this;
    }

    @Override
    public void onBootstrap()
    {
        configMobs = new ConfigMobs();
        mobDatabase = new MobDatabase(getCloud().getDatabaseManager().getDatabase("cloud_internal_cfg"));

        if (getCloud().getPacketManager().buildHandlers(PacketRC.SERVER_SELECTORS + 3).size() == 0)
        {
            getCloud().getPacketManager().registerHandler(PacketRC.SERVER_SELECTORS + 3, PacketInAddMob.class);
        }
        if (getCloud().getPacketManager().buildHandlers(PacketRC.SERVER_SELECTORS + 4).size() == 0)
        {
            getCloud().getPacketManager().registerHandler(PacketRC.SERVER_SELECTORS + 4, PacketInRemoveMob.class);
        }

        getCloud().getEventManager().registerListener(this, this);
        getCloud().getEventManager().registerListener(this, new ListenerImpl());
    }

    @Override
    public void onCall(UpdateAllEvent event)
    {
        if (event.isOnlineCloudNetworkUpdate())
            getCloud().getNetworkManager().sendToLobbys(new PacketOutMobSelector(configMobs.load(), mobDatabase.loadAll()));
    }

    private class ListenerImpl implements IEventListener<ChannelInitEvent> {

        @Override
        public void onCall(ChannelInitEvent event)
        {
            if (event.getINetworkComponent() instanceof MinecraftServer && ((MinecraftServer) event.getINetworkComponent()).getGroupMode().equals(ServerGroupMode.LOBBY))
            {
                event.getINetworkComponent().sendPacket(new PacketOutMobSelector(configMobs.load(), mobDatabase.loadAll()));
            }
        }
    }

}