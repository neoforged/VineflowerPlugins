package net.neoforged.vineflower.unfinal;

import org.jetbrains.java.decompiler.api.DecompilerOption;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences;

public interface UnfinalParametersOptions {
    @IFernflowerPreferences.Name("Remove final from Method Parameters")
    @IFernflowerPreferences.Type(DecompilerOption.Type.BOOLEAN)
    String UNFINAL_PARAMS = "unfinal-params";

    static void addDefaults(PluginOptions.AddDefaults cons) {
        cons.addDefault(UNFINAL_PARAMS, "1");
    }
}
