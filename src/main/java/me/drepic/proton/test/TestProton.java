package me.drepic.proton.test;

import me.drepic.proton.ProtonManager;
import me.drepic.proton.message.MessageAttributes;
import me.drepic.proton.message.MessageHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class TestProton implements Listener {

    /**
     * This class is used for testing messaging. This is sort of what another plugin's setup will look like
     */
    ProtonManager manager;

    public TestProton(ProtonManager manager, Plugin plugin){
        this.manager = manager;
        this.manager.registerMessageHandlers(this, plugin);
    }

    @MessageHandler(namespace="proton", subject="joinAlert", async=true)
    public void onPlayerJoinAlert(String player, MessageAttributes attributes){
        Bukkit.getServer().broadcastMessage(String.format("%s joined on %s", player, attributes.getSenderName()));
    }

    @MessageHandler(namespace="proton", subject="setTime")
    public void onSetTimeCommand(Integer ticks, MessageAttributes attributes){
        System.out.printf("%s wants to set the ticks to %d\n", attributes.getSenderName(), ticks);
        Bukkit.getServer().getWorld("world").setTime(ticks);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        //Broadcast player joined (should only see on alternate server)
        manager.broadcast("proton", "joinAlert", event.getPlayer().getName());

        //Should only set time on the alternate server
        manager.broadcast("proton", "setTime", 0);

        //Both should receive this one regardless of who sent
        manager.send("proton", "setTime", 15000, "client1");
        manager.send("proton", "setTime", 15000, "client2");
    }

}
