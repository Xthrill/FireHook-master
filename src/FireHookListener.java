import com.projectkorra.projectkorra.BendingPlayer;
import com.projectkorra.projectkorra.ability.CoreAbility;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class FireHookListener implements Listener {
    
    @EventHandler
    public void click(final PlayerInteractEvent event) {
        // Only check for left click
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        // Can't click block and can't click air
        if (event.getAction() != Action.LEFT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_AIR) {
            return;
        }
        // ? Can't click block and use item
        if (event.getAction() == Action.LEFT_CLICK_BLOCK && (event.isBlockInHand() || event.hasItem())) {
            return;
        }

        final BendingPlayer bPlayer = BendingPlayer.getBendingPlayer(event.getPlayer());

        if (bPlayer == null) {
            return;
        }

        if (bPlayer.canBend(CoreAbility.getAbility(FireHook.class)) && !CoreAbility.hasAbility(event.getPlayer(), FireHook.class)) {
            new FireHook(event.getPlayer());
        }
    }
}
