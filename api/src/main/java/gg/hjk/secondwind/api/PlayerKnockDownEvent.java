package gg.hjk.secondwind.api;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityDamageEvent;
import org.jetbrains.annotations.NotNull;

// TODO replace notnull etc with JSpecify
public class PlayerKnockDownEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }

    private final Player player;
    private final EntityDamageEvent.DamageCause cause;
    private final DamageSource source;
    private boolean cancelled = false;

    public PlayerKnockDownEvent(Player player, EntityDamageEvent.DamageCause cause, DamageSource source) {
        this.player = player;
        this.cause = cause;
        this.source = source;
    }

    public Player getPlayer() {
        return player;
    }

    public EntityDamageEvent.DamageCause getDamageCause() {
        return cause;
    }

    public DamageSource getDamageSource() {
        return source;
    }

    /// If true, the player will die instead of getting knocked down.
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /// If cancelled, the player will die instead of being knocked down.
    /// Developers wishing to prevent death entirely should instead cancel the
    /// EntityDamageEvent in a handler with a priority lower than HIGH,
    @Override
    public void setCancelled(boolean cancel) {
        cancelled = cancel;
    }
}
