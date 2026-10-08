package com.damoscreenshot;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("damo-screenshot")
public interface DamoScreenshotConfig extends Config
{
    @ConfigSection(
            name = "Screenshot Events",
            description = "Choose which events trigger screenshots",
            position = 0
    )
    String screenshotEventSection = "screenshotEvents";

    @ConfigSection(
            name = "Discord upload",
            description = "Configure screenshot uploads to Discord",
            position = 1,
            closedByDefault = true
    )
    String discordUploadSection = "discordUpload";

    @ConfigSection(
            name = "Configs",
            description = "Section for various configurable values",
            position = 1,
            closedByDefault = true
    )
    String configurationSection = "configurationSection";

    @ConfigItem(
            keyName = "uploadEnabled",
            name = "Enable Discord Uploads",
            description = "Send captured screenshots to the configured Discord webhook",
            warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
            section = discordUploadSection
    )
    default boolean uploadEnabled() {
        return false;
    }

    @ConfigItem(
            keyName = "Discord webhook URL",
            name = "Discord webhook URL",
            description = "The webhook URL that receives screenshots",
            section = discordUploadSection,
            secret = true
    )
    default String webhookUrl() {
        return "";
    }

    @ConfigItem(
            keyName = "screenshotExamines",
            name = "Screenshot examines",
            description = "Take and upload a screenshot when you examine an object",
            warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
            section = screenshotEventSection
    )
    default boolean screenshotObjectExamines() {
        return false;
    }

    @ConfigItem(
            keyName = "screenshotValuableLoot",
            name = "Screenshot valuable loot",
            description = "Capture a screenshot when a ground pickup meets the minimum value",
            warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
            section = screenshotEventSection
    )
    default boolean valuableLootEnabled() {
        return false;
    }

    @ConfigItem(
            keyName = "minimumLootValue",
            name = "Minimum loot value",
            description = "Minimum value of a ground pickup, in GP",
            section = screenshotEventSection
    )
    default int minimumLootValue()
    {
        return 100_000;
    }

    @ConfigItem(
            keyName = "itemValueOverrides",
            name = "Item value overrides",
            description = "Per-item GP values managed by the sidebar",
            hidden = true
    )
    default String itemValueOverrides() {
        return "{}";
    }
}
