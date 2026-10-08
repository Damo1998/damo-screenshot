package com.damoscreenshot;

import net.runelite.api.ItemComposition;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.http.api.item.ItemPrice;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DamoScreenshotPanel extends PluginPanel {
    // Member variables
    private final ItemValueOverrideStore mOverrideStore;
    private final ItemManager mItemManager;
    private final JPanel mOverrideListPanel = new JPanel();
    private final JTextField mItemSearchField = new JTextField(12);
    private final JComboBox<String> mItemResults = new JComboBox<>();
    private final JTextField mOverridePriceField = new JTextField(10);
    private final JLabel mOverrideStatusLabel = new JLabel(
            "<html>Search for an item.</html>"
    );
    private final List<ItemPrice> mSearchMatches = new ArrayList<ItemPrice>();
    private final ClientThread mClientThread;


    public DamoScreenshotPanel(DamoScreenshotConfig config, ConfigManager configManager, ItemValueOverrideStore overrideStore, ItemManager itemManager, ClientThread clientThread) {
        mOverrideStore = overrideStore;
        mItemManager = itemManager;
        mClientThread = clientThread;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        add(content, BorderLayout.NORTH);

        JLabel title = new JLabel("DAMO SCREENSHOT");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setForeground(Color.WHITE);
        content.add(title);
        content.add(Box.createVerticalStrut(12));

        JPanel lootSection = new JPanel();
        lootSection.setLayout(new BoxLayout(lootSection, BoxLayout.Y_AXIS));
        lootSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        lootSection.setBorder(new EmptyBorder(8, 8, 8, 8));

        JLabel lootTitle = new JLabel("VALUABLE LOOT");
        lootTitle.setFont(lootTitle.getFont().deriveFont(Font.BOLD, 12f));
        lootTitle.setForeground(Color.WHITE);
        lootSection.add(lootTitle);
        lootSection.add(Box.createVerticalStrut(8));

        JCheckBox enabledCheckbox = new JCheckBox("Capture valuable loot");
        enabledCheckbox.setSelected(config.valuableLootEnabled());
        enabledCheckbox.addActionListener(event -> {
            configManager.setConfiguration(
                    "damo-screenshot",
                    "screenshotValuableLoot",
                    enabledCheckbox.isSelected()
            );
        });
        enabledCheckbox.setOpaque(false);
        enabledCheckbox.setForeground(Color.WHITE);
        lootSection.add(enabledCheckbox);

        JTextField minimumValueField = new JTextField(Integer.toString(config.minimumLootValue()), 10);
        minimumValueField.addActionListener(event -> {
            try {
                int value = Integer.parseInt(minimumValueField.getText().trim());

                if (value < 0) {
                    throw new NumberFormatException();
                }
                configManager.setConfiguration(
                        "damo-screenshot",
                        "minimumLootValue",
                        value
                );
            } catch (NumberFormatException e) {
                // Restore the saved value if the entry wasn't a valid non-negative integer.
                minimumValueField.setText(Integer.toString(config.minimumLootValue()));
            }
        });

        JLabel minimumValueLabel = new JLabel("Minimum value (GP)");
        minimumValueLabel.setForeground(Color.LIGHT_GRAY);

        lootSection.add(Box.createVerticalStrut(6));
        lootSection.add(minimumValueLabel);
        lootSection.add(Box.createVerticalStrut(4));
        lootSection.add(minimumValueField);

        content.add(lootSection);
        content.add(Box.createVerticalStrut(12));

        content.add(Box.createVerticalStrut(12));

        JPanel overridesSection = new JPanel();
        overridesSection.setLayout(new BoxLayout(overridesSection, BoxLayout.Y_AXIS));
        overridesSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        overridesSection.setBorder(new EmptyBorder(8,8,8,8));

        JPanel searchRow = new JPanel(new BorderLayout(4, 0));
        searchRow.setOpaque(false);
        searchRow.add(mItemSearchField, BorderLayout.CENTER);

        JButton searchButton = new JButton("Search");
        searchButton.setFocusPainted(false);
        searchButton.setForeground(Color.WHITE);
        searchButton.addActionListener(event -> searchItems());
        searchRow.add(searchButton, BorderLayout.EAST);

        mItemResults.setMaximumRowCount(8);

        JButton saveButton = new JButton("Set value");
        saveButton.addActionListener(event -> saveOverride());

        JLabel overridesTitle = new JLabel("ITEM VALUE OVERRIDES");
        overridesTitle.setFont(overridesTitle.getFont().deriveFont(Font.BOLD, 12f));
        overridesTitle.setForeground(Color.WHITE);

        overridesSection.add(overridesTitle);
        overridesSection.add(Box.createVerticalStrut(8));

        overridesSection.add(searchRow);
        overridesSection.add(Box.createVerticalStrut(4));
        overridesSection.add(mItemResults);
        overridesSection.add(Box.createVerticalStrut(6));

        JLabel customValueLabel = new JLabel("Custom unit value (GP)");
        customValueLabel.setForeground(Color.LIGHT_GRAY);

        overridesSection.add(customValueLabel);
        overridesSection.add(Box.createVerticalStrut(4));
        overridesSection.add(mOverridePriceField);
        overridesSection.add(Box.createVerticalStrut(4));
        overridesSection.add(saveButton);
        overridesSection.add(Box.createVerticalStrut(4));

        mOverrideStatusLabel.setForeground(Color.LIGHT_GRAY);
        mOverrideStatusLabel.setFont(mOverrideStatusLabel.getFont().deriveFont(10f));
        overridesSection.add(mOverrideStatusLabel);

        mOverrideListPanel.setLayout(new BoxLayout(mOverrideListPanel, BoxLayout.Y_AXIS));
        mOverrideListPanel.setOpaque(false);

        overridesSection.add(mOverrideListPanel);

        refreshOverrideList();

        content.add(overridesSection);
        content.add(Box.createVerticalStrut(12));

        JPanel discordSection = new JPanel();
        discordSection.setLayout(new BoxLayout(discordSection, BoxLayout.Y_AXIS));
        discordSection.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        discordSection.setBorder(new EmptyBorder(6,8,6,8));

        JLabel discordTitle = new JLabel("DISCORD");
        discordTitle.setFont(discordTitle.getFont().deriveFont(Font.BOLD, 12f));
        discordTitle.setForeground(Color.WHITE);

        JLabel discordDescription = new JLabel(
                "<html>Upload settings are configured<br>separately for now.</html>"
        );
        discordDescription.setForeground(Color.LIGHT_GRAY);
        discordDescription.setFont(discordDescription.getFont().deriveFont(11f));

        discordSection.add(discordTitle);
        discordSection.add(Box.createVerticalStrut(8));
        discordSection.add(discordDescription);

        content.add(discordSection);
    }

    private void searchItems() {
        String query = mItemSearchField.getText().trim();

        mSearchMatches.clear();
        mItemResults.removeAllItems();

        if (query.isEmpty()) {
            mOverrideStatusLabel.setText("Enter an item name first.");
            return;
        }

        mSearchMatches.addAll(mItemManager.search(query));

        for (ItemPrice item : mSearchMatches) {
            mItemResults.addItem(item.getName());
        }

        if (mSearchMatches.isEmpty()) {
            mOverrideStatusLabel.setText("No matching tradable items found.");
        } else {
            mOverrideStatusLabel.setText("Choose an item and enter its custom value.");
        }
    }

    private void saveOverride() {
        int selectedIndex = mItemResults.getSelectedIndex();

        if (selectedIndex < 0 || selectedIndex >= mSearchMatches.size()) {
            mOverrideStatusLabel.setText("Search for and choose an item first.");
            return;
        }

        long price;
        try {
            price = Long.parseLong(mOverridePriceField.getText().trim());
            if (price < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mOverrideStatusLabel.setText("Enter a whole GP value of zero or more.");
            return;
        }

        ItemPrice selectedItem = mSearchMatches.get(selectedIndex);
        mOverrideStore.setOverride(selectedItem.getId(), price);
        refreshOverrideList();

        mOverrideStatusLabel.setText(
                "<html>Saved custom value for<br>"
                        + selectedItem.getName()
                        + ".</html>"
        );

    }

   private void refreshOverrideList() {
        Map<Integer, Long> overrides = new TreeMap<>(mOverrideStore.getOverrides());

        mClientThread.invokeLater(() -> {
            Map<Integer, String> itemNames = new TreeMap<>();

            for (int itemId : overrides.keySet()) {
                String itemName = mItemManager.getItemComposition(itemId).getName();
                itemNames.put(itemId, itemName);
            }

            SwingUtilities.invokeLater(
                    () -> renderOverrideList(overrides, itemNames)
            );
        });
   }

   private void renderOverrideList(Map<Integer, Long> overrides, Map<Integer, String> itemNames) {
        mOverrideListPanel.removeAll();

        if (overrides.isEmpty()) {
            JLabel emptyLabel = new JLabel("No custom item values yet.");
            emptyLabel.setForeground(Color.LIGHT_GRAY);
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(11f));

            mOverrideListPanel.add(emptyLabel);

        } else {
            for (Map.Entry<Integer, Long> entry : overrides.entrySet()) {
                int itemId = entry.getKey();
                long price = entry.getValue();

                String itemName = itemNames.getOrDefault(itemId, "Item " + itemId);

                JPanel row = new JPanel(new BorderLayout(6, 0));
                row.setOpaque(false);
                row.setBorder(new EmptyBorder(4, 0, 4, 0));

                JPanel itemInfo = new JPanel();
                itemInfo.setLayout(new BoxLayout(itemInfo, BoxLayout.Y_AXIS));
                itemInfo.setOpaque(false);

                JLabel itemNameLabel = new JLabel(itemName);
                itemNameLabel.setForeground(Color.WHITE);
                itemNameLabel.setFont(
                        itemNameLabel.getFont().deriveFont(Font.BOLD, 11f)
                );

                JLabel priceLabel = new JLabel(String.format("%,d GP", price));
                priceLabel.setForeground(Color.LIGHT_GRAY);
                priceLabel.setFont(priceLabel.getFont().deriveFont(10f));

                itemInfo.add(itemNameLabel);
                itemInfo.add(Box.createVerticalStrut(2));
                itemInfo.add(priceLabel);

                row.add(itemInfo, BorderLayout.CENTER);

                JButton removeButton = new JButton("Remove");
                removeButton.setFocusPainted(false);
                removeButton.setMargin(new Insets(2, 4, 4, 4));

                row.add(
                        new JLabel(itemName + " - " + price + " GP"),
                        BorderLayout.CENTER
                );

                removeButton.addActionListener(event -> {
                    mOverrideStore.removeOverride(itemId);
                    refreshOverrideList();
                });

                row.add(removeButton, BorderLayout.EAST);
                mOverrideListPanel.add(row);
            }
        }

        mOverrideListPanel.revalidate();
        mOverrideListPanel.repaint();
   }
}
