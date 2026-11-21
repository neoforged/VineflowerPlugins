package net.neoforged.vineflower.equals;

import org.jetbrains.java.decompiler.api.DecompilerOption;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public interface NullableEqualsOptions {
    @IFernflowerPreferences.Name("Add Nullable annotations to Object#equals() parameter")
    @IFernflowerPreferences.Type(DecompilerOption.Type.BOOLEAN)
    String NULLABLE_EQUALS = "nullable-equals";

    @IFernflowerPreferences.Name("Nullable annotation type")
    @IFernflowerPreferences.Type(DecompilerOption.Type.STRING)
    String NULLABLE_TYPE = "nullable-equals-anno-type";

    static void addDefaults(PluginOptions.AddDefaults cons) {
        cons.addDefault(NULLABLE_EQUALS, "1");
        cons.addDefault(NULLABLE_TYPE, "org/jspecify/annotations/Nullable");
    }
}
