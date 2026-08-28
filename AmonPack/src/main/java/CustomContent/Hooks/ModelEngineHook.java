package CustomContent.Hooks;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

public class ModelEngineHook {

    private static Boolean available = null;

    public static boolean isAvailable() {
        if (available == null) {
            Plugin p = Bukkit.getPluginManager().getPlugin("ModelEngine");
            available = (p != null && p.isEnabled());
        }
        return available;
    }

    public static boolean attachModel(Entity entity, String modelId) {
        if (!isAvailable() || entity == null || modelId == null || modelId.isEmpty()) return false;
        try {
            Class<?> apiClass = Class.forName("com.ticxo.modelengine.api.ModelEngineAPI");
            Method getModeledEntityMethod = apiClass.getMethod("getOrCreateModeledEntity", Entity.class);
            Method createActiveModelMethod = apiClass.getMethod("createActiveModel", String.class);

            Object activeModel = createActiveModelMethod.invoke(null, modelId);
            if (activeModel == null) return false;

            Object modeledEntity = getModeledEntityMethod.invoke(null, entity);
            if (modeledEntity == null) return false;

            for (Method m : modeledEntity.getClass().getMethods()) {
                if (m.getName().equals("addModel") && m.getParameterCount() == 2) {
                    m.invoke(modeledEntity, activeModel, true);
                    return true;
                }
            }
            return true;
        } catch (Throwable t) {
            Bukkit.getLogger().warning("[AmonPack] Błąd przy podpinaniu modelu ModelEngine (" + modelId + "): " + t.getMessage());
            return false;
        }
    }
}
