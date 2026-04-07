package my.pkg;

import my.pkg.RandomBuildCommand;
import my.pkg.RandomBuildListener;
import my.pkg.RandomBuildManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class MyPlugin extends JavaPlugin {

    private RandomBuildManager randomBuildManager;

    @Override
    public void onEnable() {
        this.randomBuildManager = new RandomBuildManager(this);

        getServer().getPluginManager().registerEvents(new RandomBuildListener(randomBuildManager), this);

        RandomBuildCommand cmd = new RandomBuildCommand(randomBuildManager);
        getCommand("randombuild").setExecutor(cmd);
        getCommand("randombuild").setTabCompleter(cmd);
    }
}