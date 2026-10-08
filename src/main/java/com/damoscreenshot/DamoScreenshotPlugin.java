package com.damoscreenshot;

import com.google.gson.JsonObject;
import com.google.inject.Provides;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InventoryID;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import okhttp3.*;

@Slf4j
@PluginDescriptor(
	name = "Damo Screenshot",
	description = "A starter for user-triggered screenshot capture",
	 tags = {"screenshot", "capture"}
)
public class DamoScreenshotPlugin extends Plugin
{

	@Inject
	private Client client;

	@Inject
	private DamoScreenshotConfig config;

	@Inject
	private ItemManager itemManager;

	@Inject
	private DrawManager drawManager;

	@Inject
	private OkHttpClient httpClient;

	/**
	 * The ScheduledExecutorService is a worker pool. RuneLite delivers the screenshot callback on the game thread,
	 * and encoding an image takes work. Submitting that work to the executor keeps it off the game thread.
	 *  RuneLite's Screenshot plugin uses the same pattern.
	 */
	@Inject
	private ScheduledExecutorService screenshotExecutor;

	@Inject
	private ItemValueOverrideStore itemValueOverrideStore;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private ConfigManager configManager;

	@Inject
	private ClientThread clientThread;

	private DamoScreenshotPanel panel;
	private NavigationButton navButton;

	private Map<Integer, Integer> previousInventoryCounts = new HashMap<>();
	private boolean inventoryInitialized;

	@Override
	protected void startUp() {
		panel = new DamoScreenshotPanel(
				config,
				configManager,
				itemValueOverrideStore,
				itemManager,
				clientThread
		);

		navButton = NavigationButton.builder()
				.tooltip("Damo Screenshot")
				.icon(createPanelIcon())
				.priority(50)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
		log.debug("Damo Screenshot started");
	}

	@Override
	protected void shutDown() {
		if (navButton != null) {
			clientToolbar.removeNavigation(navButton);
		}

		panel = null;
		navButton = null;
		log.debug("Damo Screenshot stopped");
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged) {
		if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
			// client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", config.greeting(), null);
		}
	}

	// 2026-10-04 14:26:37 EDT [Client] DEBUG c.d.DamoScreenshotPlugin - [CHAT-MESSAGE] Chat message is: ChatMessage(messageNode=by@486c7982, type=GAMEMESSAGE, name=, message=You don't have enough inventory space to hold that item., sender=null, timestamp=1791138397)

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage) {
		/*
			Example of a pickup chat message that failed because inventory was full.

			if (chatMessage.getType() == ChatMessageType.GAMEMESSAGE
					&& chatMessage.getMessage().equalsIgnoreCase("You don't have enough inventory space to hold that item.")) {

			}
		 */
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event) {
		log.debug("onItemContainerChanged event called.");

		if (event.getContainerId() != InventoryID.INV) {
			return;
		}

		Map<Integer, Integer> currentCounts = new HashMap<>();

		for (Item item : event.getItemContainer().getItems()) {
			if (item.getId() > 0 && item.getQuantity() > 0) {
				currentCounts.merge(item.getId(), item.getQuantity(), Integer::sum);
			}
		}

		// The first event establishes a baseline; it doesn't log everything
		// that was already in your inventory.
		if (inventoryInitialized) {
			log.debug("[DAMO] client tick count: {} ; pendingPickupTick: {} ; PICKUP_TIMEOUT_TICKS: {}", client.getTickCount(), pendingPickupTick, PICKUP_TIMEOUT_TICKS);
			if (pendingPickupItemId != -1 && client.getTickCount() - pendingPickupTick > PICKUP_TIMEOUT_TICKS) {
				pendingPickupItemId = -1;
				pendingPickupTick = -1;
			}

			currentCounts.forEach((itemId, quantity) -> {
				String itemName = itemManager.getItemComposition(itemId).getName();
				int gained = quantity - previousInventoryCounts.getOrDefault(itemId, 0);
				int removed = previousInventoryCounts.getOrDefault(itemId, 0) - quantity;

				if (gained > 0 && pendingPickupItemId == itemId) {
					if (config.valuableLootEnabled() && meetsMinimumValue(itemId, gained)) {
						requestScreenshot();
					}
					pendingPickupItemId = -1;
					pendingPickupTick = -1;
				}

				if (removed > 0) {
					log.debug("Inventory lost {} x {} (item id {})", removed, itemName, itemId);
				}

				log.debug("You now have a total of {} x {} (item id {}) in your inventory.", quantity, itemName, itemId);
			});

		}

		previousInventoryCounts = currentCounts;
		inventoryInitialized = true;
	}

	private static final int PICKUP_TIMEOUT_TICKS = 3;
	private int pendingPickupItemId = -1;
	private int pendingPickupTick = -1;

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event) {
		MenuAction action = event.getMenuAction();
		String option = event.getMenuOption();
		String target = event.getMenuTarget();

		if (action == MenuAction.GROUND_ITEM_FIRST_OPTION // GROUND_ITEM_FIRST_OPTION means the first action shown for a ground item.
			|| action == MenuAction.GROUND_ITEM_SECOND_OPTION
			|| action == MenuAction.GROUND_ITEM_THIRD_OPTION
			|| action == MenuAction.GROUND_ITEM_FOURTH_OPTION
			|| action == MenuAction.GROUND_ITEM_FIFTH_OPTION) {

			if (option.equalsIgnoreCase("take")) {
				pendingPickupItemId = event.getId();
				pendingPickupTick = client.getTickCount();
			}
		} else if (action == MenuAction.EXAMINE_NPC
				|| action == MenuAction.EXAMINE_ITEM_GROUND
				|| action == MenuAction.EXAMINE_OBJECT
				|| action == MenuAction.EXAMINE_WORLD_ENTITY) {
			if (config.screenshotObjectExamines()) {
				requestScreenshot();
			}
			log.debug("[EXAMINE] {} {}", option, target);
		}


	}

	// Helper functions

	private long getEffectiveUnitPrice(int itemId) {
		Long override = itemValueOverrideStore.getOverride(itemId);

		if (override != null) {
			return override;
		}

		return Math.max(0L, itemManager.getItemPrice(itemId));
	}

	private boolean meetsMinimumValue(int itemId, int quantityGained) {
		long unitPrice = getEffectiveUnitPrice(itemId);
		log.debug("[PRICE] Item id: {} Price {}", itemId, unitPrice);

		if (unitPrice <= 0 || quantityGained <= 0) {
			return false;
		}

		if (unitPrice > Long.MAX_VALUE / quantityGained) {
			return true;
		}

		long valueGained = unitPrice * quantityGained;
		return valueGained >= config.minimumLootValue();
	}

	private void requestScreenshot() {
		drawManager.requestNextFrameListener(image -> {
			// Move off of the game thread.
			screenshotExecutor.submit(() -> {
				BufferedImage bufferedImage = ImageUtil.bufferedImageFromImage(image);

				try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
					boolean encoded = ImageIO.write(bufferedImage, "png", outputStream);

					if (!encoded) {
						log.warn("[SCREENSHOT] No PNG encoder was available.");
						return;
					}

					byte[] pngBytes = outputStream.toByteArray();
					log.debug("[SCREENSHOT] Encoded PNG: {} bytes", pngBytes.length);

					if (!config.uploadEnabled()) {
						log.debug("[UPLOAD] Disabled; skipping screenshot");
						return;
					}

					String webhookUrl = config.webhookUrl().trim();

					if (webhookUrl.isEmpty()) {
						log.warn("[UPLOAD] No webhook URL is configured");
						return;
					}

					uploadScreenshot(webhookUrl, pngBytes);
				} catch (IOException e) {
					log.warn("[SCREENSHOT] Could not encode image", e);
				}
			});
		});
	}

	private BufferedImage createPanelIcon() {
		BufferedImage icon = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

		Graphics2D graphics = icon.createGraphics();
		graphics.setColor(new Color(190, 145, 55));
		graphics.fillRoundRect(1, 4, 14, 10, 3, 3);

		graphics.setColor(new Color(55, 45, 30));
		graphics.fillOval(5, 6, 6, 6);

		graphics.dispose();
		return icon;
	}

	// Networking helpers

	private RequestBody createScreenshotBody(byte[] pngBytes) {
		return new MultipartBody.Builder()
				.setType(MultipartBody.FORM)
				.addFormDataPart(
						"files[0]",
						"screenshot.png",
						RequestBody.create(MediaType.parse("image/png"), pngBytes)
				).build();
	}

	private void uploadScreenshot(String webhookUrl, byte[] pngBytes) {
		Request request = new Request.Builder()
				.url(webhookUrl)
				.post(createScreenshotBody(pngBytes))
				.build();

		// enqueue runs the network request async, so it won't block the game thread.
		httpClient.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				log.warn("[UPLOAD] Discord request failed", e);
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				// Try-with-resources is an alternative to closing the connection yourself.
				// It will automatically call response.close() instead of having to do it manually.
				try (Response ignored = response) {
					if (response.isSuccessful()) {
						log.debug("[UPLOAD] Discord acceptd the screenshot");
					} else {
						log.warn("[UPLOAD] DIscord returned HTTP {}", response.code());
					}
				}
			}
		});
	}

	@Provides
	DamoScreenshotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DamoScreenshotConfig.class);
	}
}
