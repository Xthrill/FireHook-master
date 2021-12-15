import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.util.Vector;

import com.projectkorra.projectkorra.GeneralMethods;
import com.projectkorra.projectkorra.ProjectKorra;
import com.projectkorra.projectkorra.ability.AddonAbility;
import com.projectkorra.projectkorra.ability.FireAbility;
import com.projectkorra.projectkorra.command.Commands;
import com.projectkorra.projectkorra.configuration.ConfigManager;
import com.projectkorra.projectkorra.util.DamageHandler;
import com.projectkorra.projectkorra.util.ParticleEffect;

public class FireHook extends FireAbility implements AddonAbility {

    private Random random;

    private Location location;
    private Location origin;
    private Vector direction;

    private static String path = "ExtraAbilities.Xristian_360.FireHook.";
    private long cooldown;
    private double damage;
    private double radius;
    private double speed;
    private double pullFactor;
    private final double pullConstant = -0.375;
    private final double pullReduce = 2.0;
    private int range;
    private boolean negateFall;

    public FireHook(Player player) {
        super(player);

        if (!bPlayer.canBend(this)) {
            return;
        }

        random = new Random();

        // Get config values
        cooldown = ConfigManager.getConfig().getLong(path + "Cooldown");
        damage = ConfigManager.getConfig().getDouble(path + "Damage");
        radius = ConfigManager.getConfig().getDouble(path + "CollisionRadius");
        speed = ConfigManager.getConfig().getDouble(path + "Speed");
        pullFactor = pullConstant * ConfigManager.getConfig().getDouble(path + "PullFactor");
        range = ConfigManager.getConfig().getInt(path + "Range");
        
        // define location variables
        location = GeneralMethods.getRightSide(player.getLocation(), .55).add(0, 0.8, 0);
        origin = player.getEyeLocation();
        direction = player.getEyeLocation().getDirection().normalize();
        
        start();
        bPlayer.addCooldown(this);
    }

    @Override
    public String getName() {
        return "FireHook";
    }

    @Override
    public long getCooldown() {
        return cooldown;
    }

    @Override
    public Location getLocation() {
        return location;
    }

    @Override
    public boolean isHarmlessAbility() {
        return false;
    }

    @Override
    public boolean isSneakAbility() {
        return false;
    }

    @Override
    public double getCollisionRadius() {
        return radius;
    }

    /**
     * Apply an affect on a living entity that comes in contact with
     * the ability instance.
     * 
     * @param entity that comes in contact with ability instance.
     */
    private void affect(Entity entity) {
        if (entity.getUniqueId() != player.getUniqueId()
        && !GeneralMethods.isRegionProtectedFromBuild(this, entity.getLocation())
        && !((entity instanceof Player)
        && Commands.invincible.contains(((Player) entity).getName()))) {
            if (entity instanceof LivingEntity) {
                // Calculate pull vector
                Vector playerLoc = player.getLocation().toVector();
                Vector entityLoc = entity.getLocation().toVector();
                Vector pullDir = new Vector(entityLoc.getX() - playerLoc.getX(),
                    entityLoc.getY() - playerLoc.getY(),
                    entityLoc.getZ() - playerLoc.getZ());
                //? This could be optimized, sqrt function takes a while to calculate
                double magnitude = Math.sqrt(Math.pow(pullDir.getX(), 2) +
                    Math.pow(pullDir.getY(), 2) +
                    Math.pow(pullDir.getZ(), 2));
                
                pullDir = pullDir.normalize();
                Vector pull = new Vector(
                    pullDir.getX() * magnitude * pullFactor,
                    pullDir.getY() * magnitude * pullFactor / pullReduce,
                    pullDir.getZ() * magnitude * pullFactor
                    );

                // Apply damage on entity
                DamageHandler.damageEntity(entity, damage, this);
                // Apply pull velocity on entity
                GeneralMethods.setVelocity(this, entity, pull);
                remove();
            }
        }
    }

    @Override
    public void progress() {
        // remove ability instance if player doesn't have permission.
        if (!bPlayer.canBendIgnoreBindsCooldowns(this)) {
            super.remove();
            return;
        }

        // remove ability instance if its in an area blocked from building (towny, kingdoms, etc.)
        if (GeneralMethods.isRegionProtectedFromBuild(this, location)) {
            super.remove();
            return;
        }

        // remove ability instance if it goes out of range
        if (!player.getWorld().equals(location.getWorld()) || location.distanceSquared(origin) > range * range) {
            super.remove();
            return;
        }

        // remove ability instance when in contact with solid block
        if (!getTransparentMaterialSet().contains(location.getBlock().getType())) {
            super.remove();
            return;
        }

        // remove ability instance if player is dead
        if (player.isDead() || !player.isOnline()) {
            super.remove();
            return;
        }

        // Update ability instance location
        location.add(direction.clone().multiply(speed));

        // Apply particles at location
        playFirebendingParticles(location, 3, 0.275F, 0.275F, 0.275F);
        ParticleEffect.CRIT.display(location, 4, 0.275F, 0.275F, 0.275F);
        // Play sound at location
        if (random.nextInt(4) == 0) playFirebendingSound(location);

        // If ability comes in contact with an entity, apply 'affect' method on entity
        Entity entity = GeneralMethods.getClosestEntity(location, radius);
        if (entity != null) affect(entity);
    }

    @Override
    public String getAuthor() {
        return "Xristian_360";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return "Hook your enemies with fire. Launches a fiery hook to drag your enemies close.";
    }

    @Override
    public String getInstructions() {
        return "Click attack to send out a fiery hook.";
    }

    @Override
    public void load() {
        // Load ability to PK
        ProjectKorra.plugin.getServer().getPluginManager().registerEvents(new FireHookListener(), ProjectKorra.plugin);
        ProjectKorra.log.info(getName() + " " + getVersion() + " by " + getAuthor() + " loaded.");
        
        // Add default config values in conf file
        ConfigManager.getConfig().addDefault(path + "Cooldown", 6000);
        ConfigManager.getConfig().addDefault(path + "Damage", 1.0);
        ConfigManager.getConfig().addDefault(path + "CollisionRadius", 1.0);
        ConfigManager.getConfig().addDefault(path + "Speed", 1.5);
        ConfigManager.getConfig().addDefault(path + "PullFactor", -1.0);
        ConfigManager.getConfig().addDefault(path + "Range", 25);
        // Save values to conf file
        ConfigManager.defaultConfig.save();

        // Apply permission to add FireHook to PK list of abilities
        if (Bukkit.getPluginManager().getPermission("bending.ability.FireHook") == null) {
            Bukkit.getPluginManager().addPermission(new Permission("bending.ability.FireHook"));
            Bukkit.getPluginManager().getPermission("bending.ability.FireHook").setDefault(PermissionDefault.TRUE);
        }
    }

    @Override
    public void stop() {
        ProjectKorra.log.info(getName() + " " + getVersion() + " by " + getAuthor() + " stopped.");
        super.remove();
    }
    
}


// import org.bukkit.plugin.java.JavaPlugin;

// public class FireHook extends JavaPlugin {
    
//     public void onEnable() {
//         getLogger().info("FireHook loaded");
//     }
// }