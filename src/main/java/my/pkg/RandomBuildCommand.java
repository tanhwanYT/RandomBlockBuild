package my.pkg;

import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class RandomBuildCommand implements CommandExecutor, TabCompleter {

    private final RandomBuildManager manager;

    public RandomBuildCommand(RandomBuildManager manager) {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("§cOP만 사용할 수 있습니다.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§e사용법: /randombuild <플레이어>");
            return true;
        }

        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) {
            sender.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return true;
        }

        manager.startBuild(target);
        sender.sendMessage("§a" + target.getName() + " 에게 랜덤 건축을 시작시켰습니다.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> list = new ArrayList<>();
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getName().toLowerCase().startsWith(input)) {
                    list.add(p.getName());
                }
            }
        }
        return list;
    }
}