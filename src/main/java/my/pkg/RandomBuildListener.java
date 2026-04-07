package my.pkg;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Set;

public class RandomBuildListener implements Listener {

    private final RandomBuildManager manager;

    public RandomBuildListener(RandomBuildManager manager) {
        this.manager = manager;
    }

    private boolean isBuilder(Player player) {
        return manager.isBuilding(player);
    }

    private boolean isAllowed(Player player, ItemStack item) {
        if (item == null || item.getType().isAir()) return true;
        Set<Material> allowed = manager.getAllowedBlocks(player);
        return allowed.contains(item.getType());
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        if (!isBuilder(player)) return;

        if (!isAllowed(player, event.getItemInHand())) {
            event.setCancelled(true);
            player.sendMessage("§c[랜덤건축] 지급된 9개의 블록만 설치할 수 있습니다.");
        }
    }

    @EventHandler
    public void onCreativeInventory(InventoryCreativeEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isBuilder(player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        // 허용 블록 외 아이템을 크리에이티브에서 꺼내는 행위 차단
        if (!isAllowed(player, cursor) || !isAllowed(player, current)) {
            event.setCancelled(true);
            player.sendMessage("§c[랜덤건축] 다른 아이템을 꺼낼 수 없습니다.");
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isBuilder(player)) return;

        ItemStack current = event.getCurrentItem();
        ItemStack cursor = event.getCursor();

        if (!isAllowed(player, current) || !isAllowed(player, cursor)) {
            event.setCancelled(true);
            return;
        }

        // 방어적으로, 크리에이티브 중 인벤토리 조작 대부분 금지
        if (player.getGameMode() == GameMode.CREATIVE) {
            switch (event.getAction()) {
                case HOTBAR_SWAP, HOTBAR_MOVE_AND_READD, MOVE_TO_OTHER_INVENTORY,
                     COLLECT_TO_CURSOR, UNKNOWN, CLONE_STACK -> event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!isBuilder(player)) return;

        ItemStack oldCursor = event.getOldCursor();
        if (!isAllowed(player, oldCursor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (!isBuilder(player)) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isBuilder(player)) return;

        manager.endBuild(player, false);
    }
}