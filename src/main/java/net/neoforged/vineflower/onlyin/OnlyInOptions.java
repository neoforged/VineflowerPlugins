package net.neoforged.vineflower.onlyin;

import org.jetbrains.java.decompiler.api.DecompilerOption;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public interface OnlyInOptions {
    @IFernflowerPreferences.Name("Add OnlyIn annotations based on manifest")
    @IFernflowerPreferences.Type(DecompilerOption.Type.BOOLEAN)
    String ADD_ONLYIN = "add-onlyin";

    static void addDefaults(PluginOptions.AddDefaults cons) {
        cons.addDefault(ADD_ONLYIN, "1");
    }
}
