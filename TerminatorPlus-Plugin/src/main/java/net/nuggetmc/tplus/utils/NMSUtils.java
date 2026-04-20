package net.nuggetmc.tplus.utils;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.syncher.SynchedEntityData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

public class NMSUtils {
    // Strategy 1: packAll() method (Paper 26.x / Mojang 1.21.5+)
    private static Method packAllMethod;
    // Strategy 2/3: field-based (Paper 1.21.x)
    private static Field itemsByIdField;
    private static boolean fieldIsMap;

    static {
        // Strategy 1: try packAll() (Paper 26.x)
        try {
            packAllMethod = SynchedEntityData.class.getDeclaredMethod("packAll");
            packAllMethod.setAccessible(true);
        } catch (NoSuchMethodException ignored) {}

        if (packAllMethod == null) {
            // Strategy 2: private final Int2ObjectMap field (Paper 1.21.x)
            for (Field field : SynchedEntityData.class.getDeclaredFields()) {
                if (!Modifier.isPrivate(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) continue;
                if (field.getType().equals(Int2ObjectMap.class)) {
                    field.setAccessible(true);
                    itemsByIdField = field;
                    fieldIsMap = true;
                    break;
                }
            }
        }

        if (packAllMethod == null && itemsByIdField == null) {
            // Strategy 3: private final array field (post-1.21.4 structural change)
            for (Field field : SynchedEntityData.class.getDeclaredFields()) {
                if (!Modifier.isPrivate(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) continue;
                if (field.getType().isArray()) {
                    field.setAccessible(true);
                    itemsByIdField = field;
                    fieldIsMap = false;
                    break;
                }
            }
        }

        if (packAllMethod == null && itemsByIdField == null) {
            throw new RuntimeException("Could not find entity data source in SynchedEntityData");
        }
    }

    @SuppressWarnings("unchecked")
    public static List<SynchedEntityData.DataValue<?>> getEntityData(SynchedEntityData synchedEntityData) {
        if (packAllMethod != null) {
            try {
                return (List<SynchedEntityData.DataValue<?>>) packAllMethod.invoke(synchedEntityData);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        try {
            List<SynchedEntityData.DataValue<?>> result = new ArrayList<>();
            if (fieldIsMap) {
                Int2ObjectMap<SynchedEntityData.DataItem<?>> map =
                        (Int2ObjectMap<SynchedEntityData.DataItem<?>>) itemsByIdField.get(synchedEntityData);
                for (SynchedEntityData.DataItem<?> item : map.values()) {
                    result.add(item.value());
                }
            } else {
                Object[] arr = (Object[]) itemsByIdField.get(synchedEntityData);
                if (arr != null) {
                    for (Object obj : arr) {
                        if (obj instanceof SynchedEntityData.DataItem<?> item) {
                            result.add(item.value());
                        }
                    }
                }
            }
            return result;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
