package com.edeathstash.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public final class ItemSerializer {

    private ItemSerializer() {
    }

    public static String serialize(List<ItemStack> items) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BukkitObjectOutputStream oos = new BukkitObjectOutputStream(baos)) {
            oos.writeInt(items.size());
            for (ItemStack item : items) {
                oos.writeObject(item);
            }
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            return "";
        }
    }

    public static List<ItemStack> deserialize(String data) {
        List<ItemStack> result = new ArrayList<>();
        if (data == null || data.isEmpty()) {
            return result;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(Base64.getDecoder().decode(data));
             BukkitObjectInputStream ois = new BukkitObjectInputStream(bais)) {
            int size = ois.readInt();
            for (int i = 0; i < size; i++) {
                Object obj = ois.readObject();
                if (obj instanceof ItemStack stack) {
                    result.add(stack);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
