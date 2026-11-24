package me.nouh.combattag;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CombatTagPlugin extends JavaPlugin implements Listener {

    private final Map<UUID, Long> combatTimers = new HashMap<>();
    private final int combatDurationSeconds = 15;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        startActionBarTask();
        getLogger().info("CombatTagSMP has been enabled!");
    }

    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim) || !(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (event.isCancelled()) return;

        tagPlayer(victim);
        tagPlayer(attacker);
    }@EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (isInCombat(player.getUniqueId())) {
            player.setHealth(0.0); // Eliminate player if they log out during combat
            combatTimers.remove(player.getUniqueId());
            Bukkit.broadcast(Component.text(player.getName() + " logged out while in combat and died!", NamedTextColor.RED));
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        if (isInCombat(player.getUniqueId()) && !player.isOp()) {
            event.setCancelled(true);
            player.sendMessage(Component.text("You cannot use commands while in combat!", NamedTextColor.RED));
        }
    }private void tagPlayer(Player player) {
        long expireTime = System.currentTimeMillis() + (combatDurationSeconds * 1000L);
        if (!isInCombat(player.getUniqueId())) {
            player.sendMessage(Component.text("You are now in combat! Do not log out.", NamedTextColor.RED));
        }
        combatTimers.put(player.getUniqueId(), expireTime);
    }

    private boolean isInCombat(UUID uuid) {
        return combatTimers.containsKey(uuid) && combatTimers.get(uuid) > System.currentTimeMillis();
    }

    private void startActionBarTask() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            long now = System.currentTimeMillis();
            combatTimers.entrySet().removeIf(entry -> {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player == null) return true;

                long remaining = (entry.getValue() - now) / 1000;
                if (remaining <= 0) {
                    player.sendMessage(Component.text("You are no longer in combat.", NamedTextColor.GREEN));
                    return true;
                }
                player.sendActionBar(Component.text("Combat Timer: " + remaining + "s", NamedTextColor.GOLD));
                return false;
            });
        }, 0L, 20L);
    }
}
