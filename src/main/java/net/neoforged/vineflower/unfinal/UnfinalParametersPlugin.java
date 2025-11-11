package net.neoforged.vineflower.unfinal;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.code.CodeConstants;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructMethodParametersAttribute;
import org.jetbrains.java.decompiler.util.Pair;

public class UnfinalParametersPlugin implements Plugin {
    @Override
    public String id() {
        return "neoforged-vineflower-unfinal-params";
    }

    @Override
    public String description() {
        return "Remove the final modifier from all method parameters.";
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        registrar.register(JavaPassLocation.AT_END, new NamedPass(id(), context -> {
            if (!DecompilerContext.getOption(UnfinalParametersOptions.UNFINAL_PARAMS)) {
                return false;
            }

            var method = context.getMethod();
            var methodParameters = method.getAttribute(StructGeneralAttribute.ATTRIBUTE_METHOD_PARAMETERS);
            if (methodParameters != null) {
                var newEntries = methodParameters.getEntries().stream()
                        .map(entry -> new StructMethodParametersAttribute.Entry(
                                entry.myName,
                                entry.myAccessFlags & ~CodeConstants.ACC_FINAL
                        )).toList();
                methodParameters.setEntries(newEntries);
            }

            return true;
        }));
    }

    @Override
    public PluginOptions getPluginOptions() {
        return () -> Pair.of(UnfinalParametersOptions.class, UnfinalParametersOptions::addDefaults);
    }
}
