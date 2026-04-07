package my.pkg;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class RandomBuildManager {

    private final JavaPlugin plugin;

    private final Map<UUID, BuildSession> sessions = new HashMap<>();

    // 랜덤 지급 후보 블록들
    private final List<Material> blockPool = Arrays.stream(Material.values())
            .filter(Material::isBlock)
            .filter(Material::isItem)
            .filter(mat -> !mat.isAir())
            .filter(mat -> !mat.name().contains("POTTED_"))
            .filter(mat -> !mat.name().contains("WALL_"))
            .filter(mat -> !mat.name().contains("SIGN"))
            .filter(mat -> !mat.name().contains("HANGING_SIGN"))
            .filter(mat -> !mat.name().contains("HEAD"))
            .filter(mat -> !mat.name().contains("SKULL"))
            .filter(mat -> !mat.name().contains("COMMAND_BLOCK"))
            .filter(mat -> !mat.name().contains("STRUCTURE_BLOCK"))
            .filter(mat -> !mat.name().contains("JIGSAW"))
            .filter(mat -> !mat.name().contains("END_PORTAL"))
            .filter(mat -> !mat.name().contains("NETHER_PORTAL"))
            .filter(mat -> !mat.name().contains("LIGHT"))
            .filter(mat -> !mat.name().contains("MOVING_PISTON"))
            .filter(mat -> !mat.name().contains("FROGSPAWN"))
            .filter(mat -> !mat.name().contains("INFESTED"))
            .collect(Collectors.toList());

    public RandomBuildManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBuilding(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    public BuildSession getSession(Player player) {
        return sessions.get(player.getUniqueId());
    }

    public Set<Material> getAllowedBlocks(Player player) {
        BuildSession session = sessions.get(player.getUniqueId());
        return session == null ? Collections.emptySet() : session.allowedBlocks;
    }

    public void startBuild(Player player) {
        endBuild(player, false); // 이미 진행 중이면 정리

        List<Material> chosen = getRandomBlocks(9);
        Set<Material> allowed = new HashSet<>(chosen);

        ItemStack[] oldContents = player.getInventory().getContents().clone();
        GameMode oldGameMode = player.getGameMode();

        player.getInventory().clear();
        player.getInventory().setArmorContents(null);
        player.getInventory().setExtraContents(null);
        player.setGameMode(GameMode.CREATIVE);

        for (int i = 0; i < chosen.size(); i++) {
            player.getInventory().setItem(i, new ItemStack(chosen.get(i), 64));
        }

        player.updateInventory();

        Bukkit.broadcastMessage("§a[랜덤건축] §f" + player.getName() + "님이 건축을 시작했습니다!");
        Bukkit.broadcastMessage("§7사용 가능한 블록:");
        for (Material mat : chosen) {
            Bukkit.broadcastMessage("§f- " + pretty(mat));
        }

        player.sendTitle("§a랜덤 건축 시작!", "§f60초 안에 완성하세요", 10, 40, 10);

        BuildSession session = new BuildSession(
                player.getUniqueId(),
                allowed,
                oldContents,
                oldGameMode
        );

        sessions.put(player.getUniqueId(), session);

        // 시작 직후 모두에게 액션바 60초 표시
        sendActionBarToAll(player, 60);

        // 1초마다 액션바 갱신
        BukkitTask actionBarTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            BuildSession current = sessions.get(player.getUniqueId());
            if (current == null) return;

            current.remainingSeconds--;

            if (current.remainingSeconds <= 0) {
                endBuild(player, true);
                return;
            }

            sendActionBarToAll(player, current.remainingSeconds);
        }, 20L, 20L);

        session.actionBarTask = actionBarTask;
    }

    public void endBuild(Player player, boolean timeUp) {
        BuildSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;

        if (session.actionBarTask != null) {
            session.actionBarTask.cancel();
        }

        player.getInventory().clear();
        player.getInventory().setContents(session.oldContents);
        player.setGameMode(session.oldGameMode);
        player.updateInventory();

        // 액션바 전체 지우기
        clearActionBarForAll();

        if (timeUp) {
            player.sendTitle("§c시간 종료!", "§7건축이 끝났습니다", 10, 50, 10);
            Bukkit.broadcastMessage("§c[랜덤건축] §f" + player.getName() + "님의 건축 시간이 종료되었습니다.");
        } else {
            Bukkit.broadcastMessage("§e[랜덤건축] §f" + player.getName() + "님의 게임이 종료되었습니다.");
        }
    }

    private void sendActionBarToAll(Player builder, int seconds) {
        String color;
        if (seconds <= 10) {
            color = "§c";
        } else if (seconds <= 30) {
            color = "§e";
        } else {
            color = "§a";
        }

        String message = "§a[랜덤건축] §f" + builder.getName() + " §7| " + color + "남은 시간: §f" + seconds + "초";

        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendActionBar(Component.text(message));
        }
    }

    private void clearActionBarForAll() {
        for (Player online : Bukkit.getOnlinePlayers()) {
            online.sendActionBar(Component.text(""));
        }
    }

    private List<Material> getRandomBlocks(int count) {
        List<Material> copy = new ArrayList<>(blockPool);
        Collections.shuffle(copy, ThreadLocalRandom.current());
        return copy.subList(0, Math.min(count, copy.size()));
    }

    private String pretty(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    public static class BuildSession {
        private final UUID playerId;
        private final Set<Material> allowedBlocks;
        private final ItemStack[] oldContents;
        private final GameMode oldGameMode;

        private BukkitTask actionBarTask;
        private int remainingSeconds = 60;

        public BuildSession(UUID playerId, Set<Material> allowedBlocks, ItemStack[] oldContents,
                            GameMode oldGameMode) {
            this.playerId = playerId;
            this.allowedBlocks = allowedBlocks;
            this.oldContents = oldContents;
            this.oldGameMode = oldGameMode;
        }

        public UUID getPlayerId() {
            return playerId;
        }

        public Set<Material> getAllowedBlocks() {
            return allowedBlocks;
        }

        public ItemStack[] getOldContents() {
            return oldContents;
        }

        public GameMode getOldGameMode() {
            return oldGameMode;
        }
    }
}