package com.eternalcode.minions.render;

import com.eternalcode.minions.config.MessagesConfig;
import com.eternalcode.minions.gui.MinionPanel;
import com.eternalcode.minions.minion.MinionId;
import com.eternalcode.minions.minion.Minion;
import com.eternalcode.minions.minion.MinionRegistry;
import com.eternalcode.minions.notice.NoticeService;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientAttack;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import java.util.Optional;
import java.util.function.BiConsumer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MinionInteractionListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final MinionEntityIndex entityIndex;
    private final MinionRegistry minions;
    private final MinionPanel panel;
    private final MessagesConfig messages;
    private final NoticeService notices;
    private final BiConsumer<Player, Minion> pickup;

    public MinionInteractionListener(
        Plugin plugin,
        MinionEntityIndex entityIndex,
        MinionRegistry minions,
        MinionPanel panel,
        MessagesConfig messages,
        NoticeService notices,
        BiConsumer<Player, Minion> pickup
    ) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.entityIndex = entityIndex;
        this.minions = minions;
        this.panel = panel;
        this.messages = messages;
        this.notices = notices;
        this.pickup = pickup;
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
            if (attack) {
                this.pickup(player, minionId.get());
            }
            else {
                this.open(player, minionId.get());
            }
        });
    }

    private void open(Player player, MinionId minionId) {
        Minion minion = this.findOwnedMinion(player, minionId);
        if (minion != null) {
            this.panel.open(player, minion);
        }
    }

    private void pickup(Player player, MinionId minionId) {
        Minion minion = this.findOwnedMinion(player, minionId);
        if (minion != null) {
            this.pickup.accept(player, minion);
        }
    }

    private Minion findOwnedMinion(Player player, MinionId minionId) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            this.notices.create().viewer(player).notice(this.messages.minionNotFound).send();
            return null;
        }
        if (!minion.ownerId().equals(player.getUniqueId())) {
            this.notices.create().viewer(player).notice(this.messages.minionOwnerRequired).send();
            return null;
        }
        return minion;
    }
}
