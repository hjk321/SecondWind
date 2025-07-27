package gg.hjk.secondwind.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

// TODO replace notnull etc with JSpecify
public class PlayerDeathAfterKnockDownEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Player player;

    public PlayerDeathAfterKnockDownEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return player;
    }
}
