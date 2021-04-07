/*
 *  This file is part of RPG Items.
 *
 *  RPG Items is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  RPG Items is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with RPG Items.  If not, see <http://www.gnu.org/licenses/>.
 */
package think.rpgitems.power.impl;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import think.rpgitems.I18n;
import think.rpgitems.RPGItems;
import think.rpgitems.power.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static think.rpgitems.power.Utils.getNearbyEntities;

/**
 * Power fire.
 * <p>
 * The fire power will fire an fireblock on right click
 * that burns entities hit for {@link #burnduration duration} in ticks.
 * Furthermore it sends out a burning trail into the aimed direction for {@link #distance} blocks.
 * </p>
 */
@SuppressWarnings("WeakerAccess")
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = Fire.Impl.class)
public class Fire extends BasePower {

    @Property(order = 1)
    public int distance = 15;
    @Property(order = 2)
    public int burnduration = 40;

    /**
     * Duration of the fire, in ticks
     */
    public int getBurnduration() {
        return burnduration;
    }

    /**
     * Maximum distance
     */
    public int getDistance() {
        return distance;
    }

    @Override
    public String getName() {
        return "fire";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.fire", (0) / 20d);
    }

    public static class Impl implements PowerRightClick<Fire>, PowerLeftClick<Fire>, PowerSneak<Fire>, PowerSprint<Fire>, PowerPlain<Fire> {

        @Override
        public PowerResult<Void> rightClick(Fire power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> fire(Fire power, Player player, ItemStack stack) {
            player.playSound(player.getLocation(), Sound.ITEM_FLINTANDSTEEL_USE, 1.0f, 1.2f);
            final List<Block> fireblocks = new ArrayList<>();
            final FallingBlock block = player.getWorld().spawnFallingBlock(player.getLocation().add(0, 1.8, 0), Material.FIRE.createBlockData());
            block.setVelocity(player.getLocation().getDirection().multiply(2d));
            block.setDropItem(false);

            final Location location = player.getLocation().add(0, 0.5, 0).getBlock().getLocation();
            final Vector direction = player.getLocation().getDirection();
            direction.setY(0);
            direction.normalize();
            location.add(direction.multiply(4));

            BukkitRunnable run = new BukkitRunnable() {
                private boolean finishedFire = false, blockDead = false;
                private int count = 0;

                public void run() {
                    if (!finishedFire) {
                        if (!location.getBlock().getType().equals(Material.AIR)) {
                            finishedFire = true;
                        }

                        Location temp = location.clone();
                        for (int x = -1; x <= 1; x++) {
                            for (int z = -1; z <= 1; z++) {
                                temp.setX(x + location.getBlockX());
                                temp.setZ(z + location.getBlockZ());
                                Block block = temp.getBlock();
                                if (block.getType().equals(Material.AIR) && !block.getRelative(0, -1, 0).getType().isBurnable()) {
                                    block.setType(Material.FIRE);
                                    fireblocks.add(block);
                                }
                            }
                        }
                        location.add(direction);
                        if (count >= power.getDistance()) {
                            finishedFire = true;
                        }
                        count++;

                        if (finishedFire) {
                            (new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (fireblocks.isEmpty()) {
                                        cancel();
                                        return;
                                    }
                                    Block fb = fireblocks.get(0);
                                    fb.getWorld().playEffect(fb.getLocation(), Effect.EXTINGUISH, 1);
                                    fb.setType(Material.AIR);
                                    fireblocks.remove(fb);
                                }
                            }).runTaskTimer(RPGItems.plugin, 4 * 20 + new Random().nextInt(40), 3);
                        }
                    }

                    if (!blockDead) {
                        if (block.isDead()) {
                            block.remove();
                            if (block.getLocation().getBlock().getType().equals(Material.FIRE))
                                block.getLocation().getBlock().setType(Material.AIR);
                            blockDead = true;
                        }
                    } else {
                        List<Entity> ents = getNearbyEntities(power, block.getLocation(), player, 1, 0, 1, 0);
                        for (Entity ent : ents)
                            if (ent instanceof Damageable)
                                ent.setFireTicks(power.getBurnduration());
                    }

                    if (finishedFire && blockDead)
                        cancel();
                }
            };
            run.runTaskTimer(RPGItems.plugin, 0, 1);
            return PowerResult.ok();
        }

        @Override
        public Class<? extends Fire> getPowerClass() {
            return Fire.class;
        }

        @Override
        public PowerResult<Void> leftClick(Fire power, final Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Fire power, Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(power, player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Fire power, Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(power, player, stack);
        }
    }
}
