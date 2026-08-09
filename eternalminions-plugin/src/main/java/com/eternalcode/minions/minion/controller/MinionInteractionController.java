package com.eternalcode.minions.minion.controller;

import com.eternalcode.minions.access.MinionAccessAction;
import com.eternalcode.minions.minion.access.MinionAccessGuard;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.MinionPickupService;
import com.eternalcode.minions.minion.rotation.MinionRotationService;
import com.eternalcode.minions.render.MinionEntityIndex;
import com.eternalcode.minions.render.MinionInteractionAction;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import java.util.Optional;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MinionInteractionController extends PacketListenerAbstract {

    private final Plugin plugin;
    private final MinionEntityIndex entityIndex;
    private final MinionAccessGuard access;
    private final MinionPanel panel;
    private final MinionRotationService rotations;
    private final MinionPickupService pickups;

    public MinionInteractionController(
        Plugin plugin,
        MinionEntityIndex entityIndex,
        MinionAccessGuard access,
        MinionPanel panel,
        MinionRotationService rotations,
        MinionPickupService pickups
    ) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.entityIndex = entityIndex;
        this.access = access;
        this.panel = panel;
        this.rotations = rotations;
        this.pickups = pickups;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int entityId;
        boolean attack;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            entityId = new WrapperPlayClientInteractEntity(event).getEntityId();
            attack = false;
        }
        else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
            attack = true;
        }
        else {
            return;
        }

        Optional<MinionId> minionId = this.entityIndex.find(entityId);
        if (minionId.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        Player player = event.getPlayer();
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> {
            MinionInteractionAction action =
                    MinionInteractionAction.resolve(
                            attack,
                            player.isSneaking()
                    );

            switch (action) {
                case PICK_UP -> this.pickups.pickup(player, minionId.get());
                case ROTATE -> this.rotations.rotate(player, minionId.get());
                case OPEN_PANEL -> this.open(player, minionId.get());
            }
        });
    }

    private void open(Player player, MinionId minionId) {
        this.access.findAccessible(
                player,
                minionId,
                MinionAccessAction.OPEN_PANEL
        ).ifPresent(minion -> this.panel.open(player, minion));
    }

}
