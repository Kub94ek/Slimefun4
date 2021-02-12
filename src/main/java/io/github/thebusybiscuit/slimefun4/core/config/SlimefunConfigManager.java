package io.github.thebusybiscuit.slimefun4.core.config;

import java.util.function.Supplier;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.lang.Validate;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;

import io.github.thebusybiscuit.cscorelib2.config.Config;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.core.researching.Research;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;

/**
 * This class manages our {@link Config}, it caches values for faster access
 * and provides means to reload it.
 * 
 * @author TheBusyBiscuit
 *
 */
public final class SlimefunConfigManager {

    /**
     * Our {@link SlimefunPlugin} instance
     */
    private final SlimefunPlugin plugin;

    /**
     * Our {@link Plugin} {@link Config} (config.yml)
     */
    private final Config pluginConfig;

    /**
     * Our {@link SlimefunItem} {@link Config} (Items.yml)
     */
    private final Config itemsConfig;

    /**
     * Our {@link Research} {@link Config} (Researches.yml)
     */
    private final Config researchesConfig;

    // Various booleans we want to cache instead of re-parsing everytime
    private boolean isBackwardsCompatibilityEnabled;
    private boolean isResearchingEnabled;
    private boolean isResearchingFreeInCreativeMode;
    private boolean isResearchFireworkEnabled;
    private boolean isDuplicateBlockLoggingEnabled;
    private boolean isVanillaRecipeShowingEnabled;
    private boolean isSlimefunGuideGivenOnJoin;
    private boolean isUpdaterEnabled;
    private boolean isTalismanMessageInActionbar;
    private boolean isExcessCommandItemsDroppingEnabled;

    /**
     * This constructs a new {@link SlimefunConfigManager} for the given instance
     * of {@link SlimefunPlugin}.
     * 
     * @param plugin
     *            The {@link SlimefunPlugin} instance
     */
    public SlimefunConfigManager(@Nonnull SlimefunPlugin plugin) {
        Validate.notNull(plugin, "The Plugin instance cannot be null");

        this.plugin = plugin;
        pluginConfig = getConfig(plugin, "config", () -> new Config(plugin));
        itemsConfig = getConfig(plugin, "Items", () -> new Config(plugin, "Items.yml"));
        researchesConfig = getConfig(plugin, "Researches", () -> new Config(plugin, "Researches.yml"));
    }

    @Nullable
    @ParametersAreNonnullByDefault
    private Config getConfig(SlimefunPlugin plugin, String name, Supplier<Config> supplier) {
        try {
            return supplier.get();
        } catch (Exception x) {
            plugin.getLogger().log(Level.SEVERE, x, () -> "An Exception was thrown while loading the config file \"" + name + ".yml\" for Slimefun v" + plugin.getDescription().getVersion());
            return null;
        }
    }

    /**
     * This method (re)loads all relevant config values into our cache.
     * <p>
     * <strong>Note that this method is not guaranteed to reload all settings.</strong>
     * 
     * @return Whether the reloading was successful and completed without any errors
     */
    public boolean reload() {
        boolean isSuccessful = true;

        try {
            pluginConfig.reload();
            itemsConfig.reload();
            researchesConfig.reload();

            researchesConfig.setDefaultValue("enable-researching", true);

            isBackwardsCompatibilityEnabled = pluginConfig.getBoolean("options.backwards-compatibility");
            isResearchingEnabled = researchesConfig.getBoolean("enable-researching");
            isResearchingFreeInCreativeMode = pluginConfig.getBoolean("researches.free-in-creative-mode");
            isResearchFireworkEnabled = pluginConfig.getBoolean("researches.enable-fireworks");
            isDuplicateBlockLoggingEnabled = pluginConfig.getBoolean("options.log-duplicate-block-entries");
            isVanillaRecipeShowingEnabled = pluginConfig.getBoolean("guide.show-vanilla-recipes");
            isSlimefunGuideGivenOnJoin = pluginConfig.getBoolean("guide.receive-on-first-join");
            isUpdaterEnabled = pluginConfig.getBoolean("options.auto-update");
            isTalismanMessageInActionbar = pluginConfig.getBoolean("talismans.use-actionbar");
            isExcessCommandItemsDroppingEnabled = pluginConfig.getBoolean("options.drop-excess-sf-give-items");
        } catch (Exception x) {
            plugin.getLogger().log(Level.SEVERE, x, () -> "An Exception was caught while (re)loading the config files for Slimefun v" + plugin.getDescription().getVersion());
            isSuccessful = false;
        }

        // Reload Research costs
        for (Research research : SlimefunPlugin.getRegistry().getResearches()) {
            try {
                NamespacedKey key = research.getKey();
                int cost = researchesConfig.getInt(key.getNamespace() + '.' + key.getKey() + ".cost");
                research.setCost(cost);
            } catch (Exception x) {
                plugin.getLogger().log(Level.SEVERE, x, () -> "Something went wrong while trying to update the cost of a research: " + research);
                isSuccessful = false;
            }
        }

        for (SlimefunItem item : SlimefunPlugin.getRegistry().getAllSlimefunItems()) {
            // Reload Item Settings
            try {
                for (ItemSetting<?> setting : item.getItemSettings()) {
                    // Make sure that the setting was loaded properly
                    if (!setting.load(item)) {
                        isSuccessful = false;
                    }
                }
            } catch (Exception x) {
                item.error("Something went wrong while updating the settings for this item!", x);
                isSuccessful = false;
            }

            // Reload permissions
            try {
                SlimefunPlugin.getPermissionsService().update(item, false);
            } catch (Exception x) {
                item.error("Something went wrong while updating the permission node for this item!", x);
                isSuccessful = false;
            }
        }

        return isSuccessful;
    }

    @Nonnull
    public Config getPluginConfig() {
        return pluginConfig;
    }

    @Nonnull
    public Config getItemsConfig() {
        return itemsConfig;
    }

    @Nonnull
    public Config getResearchConfig() {
        return researchesConfig;
    }

    /**
     * This method saves all our {@link Config} files.
     */
    public void saveFiles() {
        pluginConfig.save();
        itemsConfig.save();
        researchesConfig.save();
    }

    /**
     * This method returns whether backwards-compatibility is enabled.
     * Backwards compatibility allows Slimefun to recognize items from older versions but comes
     * at a huge performance cost.
     * 
     * @return Whether backwards compatibility is enabled
     */
    public boolean isBackwardsCompatible() {
        return isBackwardsCompatibilityEnabled;
    }

    /**
     * This method sets the status of backwards compatibility.
     * Backwards compatibility allows Slimefun to recognize items from older versions but comes
     * at a huge performance cost.
     * 
     * @param compatible
     *            Whether backwards compatibility should be enabled
     */
    public void setBackwardsCompatible(boolean compatible) {
        isBackwardsCompatibilityEnabled = compatible;
    }

    public void setResearchingEnabled(boolean enabled) {
        isResearchingEnabled = enabled;
    }

    public boolean isResearchingEnabled() {
        return isResearchingEnabled;
    }

    public void setFreeCreativeResearchingEnabled(boolean enabled) {
        isResearchingFreeInCreativeMode = enabled;
    }

    public boolean isFreeCreativeResearchingEnabled() {
        return isResearchingFreeInCreativeMode;
    }

    public boolean isResearchFireworkEnabled() {
        return isResearchFireworkEnabled;
    }

    public boolean isDuplicateBlockLoggingEnabled() {
        return isDuplicateBlockLoggingEnabled;
    }

    public boolean isVanillaRecipeShown() {
        return isVanillaRecipeShowingEnabled;
    }

    public boolean isSlimefunGuideGivenOnJoin() {
        return isSlimefunGuideGivenOnJoin;
    }

    public boolean isUpdaterEnabled() {
        return isUpdaterEnabled;
    }

    public boolean isTalismanMessageInActionbar() {
        return isTalismanMessageInActionbar;
    }

    public boolean isExcessCommandItemsDroppingEnabled() {
        return isExcessCommandItemsDroppingEnabled;
    }

}