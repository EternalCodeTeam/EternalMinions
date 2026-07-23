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
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public final class MinionInteractionListener extends PacketListenerAbstract {

    private final Plugin plugin;
    private final MinionEntityIndex entityIndex;
    private final MinionRegistry minions;
    private final MinionPanel panel;
    private final MessagesConfig messages;
    private final NoticeService notices;

    public MinionInteractionListener(
        Plugin plugin,
        MinionEntityIndex entityIndex,
        MinionRegistry minions,
        MinionPanel panel,
        MessagesConfig messages,
        NoticeService notices
    ) {
        super(PacketListenerPriority.NORMAL);
        this.plugin = plugin;
        this.entityIndex = entityIndex;
        this.minions = minions;
        this.panel = panel;
        this.messages = messages;
        this.notices = notices;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        int entityId;
        if (event.getPacketType() == PacketType.Play.Client.INTERACT_ENTITY) {
            entityId = new WrapperPlayClientInteractEntity(event).getEntityId();
        }
        else if (event.getPacketType() == PacketType.Play.Client.ATTACK) {
            entityId = new WrapperPlayClientAttack(event).getEntityId();
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
        this.plugin.getServer().getScheduler().runTask(this.plugin, () -> this.open(player, minionId.get()));
    }

    private void open(Player player, MinionId minionId) {
        Minion minion = this.minions.findMinion(minionId).orElse(null);
        if (minion == null) {
            this.notices.create().viewer(player).notice(this.messages.minionNotFound).send();
            return;
        }
        if (!minion.ownerId().equals(player.getUniqueId())) {
            this.notices.create().viewer(player).notice(this.messages.minionOwnerRequired).send();
            return;
        }
        this.panel.open(player, minion);
    }
}
