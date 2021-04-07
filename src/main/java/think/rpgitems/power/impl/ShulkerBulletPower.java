package think.rpgitems.power.impl;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.inventory.ItemStack;
import think.rpgitems.Events;
import think.rpgitems.I18n;
import think.rpgitems.data.Context;
import think.rpgitems.power.*;

import java.util.List;

import static think.rpgitems.power.PowerResult.ok;
import static think.rpgitems.power.Utils.*;

/**
 * Power shulker bullet.
 * <p>
 * Launches shulker bullet when right clicked.
 * Target nearest entity
 * </p>
 */
@Meta(defaultTrigger = "RIGHT_CLICK", withSelectors = true, generalInterface = PowerPlain.class, implClass = ShulkerBulletPower.Impl.class)
public class ShulkerBulletPower extends BasePower {

    @Property(order = 1)
    public double range = 10;

    @Override
    public void init(ConfigurationSection s) {
        super.init(s);
    }

    @Override
    public String getName() {
        return "shulkerbullet";
    }

    @Override
    public String displayText() {
        return I18n.formatDefault("power.shulkerbullet", (double) 0 / 20d);
    }

    /**
     * Range of target finding
     */
    public double getRange() {
        return range;
    }

    public class Impl implements PowerRightClick, PowerLeftClick, PowerSneak, PowerSprint, PowerPlain, PowerBowShoot {
        @Override
        public PowerResult<Void> leftClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        @SuppressWarnings("deprecation")
        public PowerResult<Void> fire(Player player, ItemStack stack) {
            Events.registerRPGProjectile(getItem(), stack, player);
            org.bukkit.entity.ShulkerBullet bullet = player.launchProjectile(org.bukkit.entity.ShulkerBullet.class, player.getEyeLocation().getDirection());
            bullet.setPersistent(false);
            List<LivingEntity> entities = getLivingEntitiesInCone(getNearestLivingEntities(getPower(), player.getEyeLocation(), player, getRange(), 0), player.getLocation().toVector(), 30, player.getLocation().getDirection());
            if (!entities.isEmpty()) {
                Context.instance().putExpiringSeconds(player.getUniqueId(), "shulkerbullet.target", entities.get(0), 3);
                bullet.setTarget(entities.get(0));
            }
            return ok();
        }

        @Override
        public Power getPowerClass() {
            return ShulkerBulletPower.this;
        }

        @Override
        public PowerResult<Void> rightClick(Player player, ItemStack stack, PlayerInteractEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sneak(Player player, ItemStack stack, PlayerToggleSneakEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Void> sprint(Player player, ItemStack stack, PlayerToggleSprintEvent event) {
            return fire(player, stack);
        }

        @Override
        public PowerResult<Float> bowShoot(Player player, ItemStack itemStack, EntityShootBowEvent e) {
            return fire(player, itemStack).with(e.getForce());
        }
    }
}
