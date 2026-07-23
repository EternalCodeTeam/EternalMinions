package com.eternalcode.minions.render;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public final class MinionViewerListener implements Listener {

    private final MinionRenderService renderService;

    public MinionViewerListener(MinionRenderService renderService) {
        this.renderService = renderService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        this.renderService.reconcile(event.getPlayer());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.renderService.hideAll(event.getPlayer());
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (event.getFrom().getWorld() == event.getTo().getWorld()
            && event.getFrom().getBlockX() >> 4 == event.getTo().getBlockX() >> 4
            && event.getFrom().getBlockZ() >> 4 == event.getTo().getBlockZ() >> 4) {
            return;
        }
        this.renderService.reconcile(event.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        this.renderService.reconcile(event.getPlayer());
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        this.renderService.reconcile(event.getPlayer());
    }
}
