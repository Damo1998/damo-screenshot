package com.damoscreenshot;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ItemValueOverrideStore {
    private static final String CONFIG_GROUP = "damo-screenshot";
    private static final String CONFIG_KEY = "itemValueOverrides";

    @Inject
    private Gson gson;

    @Inject
    private ConfigManager configManager;

    public Long getOverride(int itemId) {
        JsonElement value = readOverrides().get(Integer.toString(itemId));

        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            return null;
        }

        try {
            long price = value.getAsLong();
            return price >= 0 ? price : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public void setOverride(int itemId, long price)
    {
        if (itemId <= 0 || price < 0)
        {
            return;
        }

        JsonObject overrides = readOverrides();
        overrides.addProperty(Integer.toString(itemId), price);
        saveOverrides(overrides);
    }

    public void removeOverride(int itemId)
    {
        JsonObject overrides = readOverrides();
        overrides.remove(Integer.toString(itemId));
        saveOverrides(overrides);
    }

    private JsonObject readOverrides()
    {
        String json = configManager.getConfiguration(CONFIG_GROUP, CONFIG_KEY);

        if (json == null || json.trim().isEmpty())
        {
            return new JsonObject();
        }

        try
        {
            JsonObject overrides = gson.fromJson(json, JsonObject.class);
            return overrides != null ? overrides : new JsonObject();
        }
        catch (JsonParseException e)
        {
            log.warn("Could not read item value overrides; ignoring them.", e);
            return new JsonObject();
        }
    }

    public Map<Integer, Long> getOverrides() {
        Map<Integer, Long> result = new HashMap<>();

        for (Map.Entry<String, JsonElement> entry : readOverrides().entrySet()) {
            try {
                int itemId = Integer.parseInt(entry.getKey());
                JsonElement value = entry.getValue();

                if (itemId > 0 && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
                    long price = value.getAsLong();

                    if (price >= 0) {
                        result.put(itemId, price);
                    }
                }
            } catch (NumberFormatException e) {
                log.debug("Ignoring invalid item value override: {}", entry.getKey());
            }
        }
        return result;
    }

    private void saveOverrides(JsonObject overrides) {
        configManager.setConfiguration(
                CONFIG_GROUP,
                CONFIG_KEY,
                gson.toJson(overrides)
        );
    }

}
