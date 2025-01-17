package ovh.mythmc.banco.api.storage;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.simpleyaml.configuration.ConfigurationSection;
import org.simpleyaml.configuration.file.YamlFile;
import ovh.mythmc.banco.api.Banco;
import ovh.mythmc.banco.api.logger.LoggerWrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

@Getter
public final class BancoConfig {

    static final LoggerWrapper logger = new LoggerWrapper() {
        @Override
        public void info(String message, Object... args) {
            Banco.get().getLogger().info("[config] " + message, args);
        }

        @Override
        public void warn(String message, Object... args) {
            Banco.get().getLogger().warn("[config] " + message, args);
        }

        @Override
        public void error(String message, Object... args) {
            Banco.get().getLogger().error("[config] " + message, args);
        }
    };

    private final YamlFile yamlFile;
    private final Settings settings = new Settings();

    public BancoConfig(final @NotNull File pluginFolder) {
        try {
            Files.createDirectories(pluginFolder.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        this.yamlFile = new YamlFile(new File(pluginFolder, "config.yml"));
    }

    public void load() {
        try {
            if (!yamlFile.exists())
                Files.copy(Objects.requireNonNull(Banco.class.getResourceAsStream("/config.yml")),
                        Path.of(yamlFile.getFilePath()));

            yamlFile.load();
        } catch (Exception e) {
            throw new IllegalStateException("Error while loading configuration", e);
        }

        loadValues();
    }

    public void loadValues() {
        settings.debug = yamlFile.getBoolean("debug");
        settings.currency = new Settings.Currency(
                yamlFile.getString("currency.name.singular", "Dollar"),
                yamlFile.getString("currency.name.plural", "Dollars"),
                yamlFile.getString("currency.symbol", "$"),
                yamlFile.getBoolean("currency.remove-drops", true),
                yamlFile.getBoolean("currency.count-ender-chest", true),
                yamlFile.getString("currency.format", "#,###.#"),
                yamlFile.getConfigurationSection("currency.value")
        );

        settings.autoSave = new Settings.AutoSave(
                yamlFile.getBoolean("auto-save.enabled", true),
                yamlFile.getInt("auto-save.frequency", 300)
        );

        settings.updateTracker = new Settings.UpdateTracker(
                yamlFile.getBoolean("update-tracker.enabled", true)
        );

        settings.commands = new Settings.Commands(
                yamlFile.getBoolean("commands.balance.enabled", true),
                yamlFile.getBoolean("commands.pay.enabled", true)
        );

        Banco.get().getEconomyManager().registerAll(settings.currency.value());
    }

    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static final class Settings {
        private boolean debug;

        public record Currency(@NotNull String nameSingular,
                        @NotNull String namePlural,
                        @NotNull String symbol,
                        boolean removeDrops,
                        boolean countEnderChest,
                        @NotNull String format,
                        @NotNull ConfigurationSection value) { }

        public record AutoSave(boolean enabled,
                        int frequency) { }

        public record UpdateTracker(boolean enabled) { }

        public record Commands(boolean balanceEnabled,
                        boolean payEnabled) { }

        private Currency currency;
        private AutoSave autoSave;
        private UpdateTracker updateTracker;
        private Commands commands;
    }

}
