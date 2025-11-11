package net.neoforged.vineflower.onlyin;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FieldExprent;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.util.Key;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class OnlyInPlugin implements Plugin {
    private static final Key<OnlyInState> STATE = Key.of("ONLYIN_STATE");
    private static final AnnotationExprent ANNOTATION_CLIENT = makeAnnotation("CLIENT");
    private static final AnnotationExprent ANNOTATION_SERVER = makeAnnotation("DEDICATED_SERVER");

    @Override
    public String id() {
        return "neoforged-vineflower-onlyin";
    }

    @Override
    public String description() {
        return "Remove the final modifier from all method parameters.";
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        // Passes run per method, so we need to track which we added the annotation to already.
        registrar.register(JavaPassLocation.BEFORE_MAIN, new NamedPass(id(), context -> {
            if (!DecompilerContext.getOption(OnlyInOptions.ADD_ONLYIN)) {
                return false;
            }

            OnlyInState state;
            synchronized (DecompilerContext.class) {
                state = DecompilerContext.getContextProperty(STATE);
                if (state == null) {
                    state = DecompilerContext.getContextProperty(STATE);
                    if (state == null) {
                        state = new OnlyInState();
                        DecompilerContext.setProperty(STATE, state);
                    }
                }
            }

            StructClass parent = context.getEnclosingClass();
            if (parent.qualifiedName.contains("$")) {
                return true; // dont handle inner classes etc.
            }

            var entryAttr = state.manifest.getEntries().get(parent.qualifiedName + ".class");
            if (entryAttr == null) {
                return true; // No dist-specificity
            }

            var entryDist = entryAttr.getValue("Minecraft-Dist");
            if (entryDist != null) {
                AnnotationExprent annotation;
                if ("client".equals(entryDist)) {
                    annotation = ANNOTATION_CLIENT;
                } else if ("server".equals(entryDist)) {
                    annotation = ANNOTATION_SERVER;
                } else {
                    return true; // Unsupported dist.
                }

                // Only add the annotation if it's not already present.
                var annotations = parent.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS);
                if (annotations != null && annotations.getAnnotations().stream().anyMatch(a -> a.getClassName().equals(annotation.getClassName()))) {
                    return true;
                }
                if (annotations == null) {
                    annotations = new StructAnnotationAttribute();
                    parent.getAttributes().put(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS.name, annotations);
                }
                if (annotations.getAnnotations() == null) {
                    annotations.setAnnotations(new ArrayList<>());
                }
                annotations.getAnnotations().add(annotation);
            }

            return true;
        }));
    }

    @Override
    public PluginOptions getPluginOptions() {
        return () -> Pair.of(OnlyInOptions.class, OnlyInOptions::addDefaults);
    }

    private static AnnotationExprent makeAnnotation(String dist) {
        return new AnnotationExprent(
                "net/neoforged/api/distmarker/OnlyIn",
                List.of("value"),
                List.of(
                        new FieldExprent(
                                dist,
                                "net/neoforged/api/distmarker/Dist",
                                true,
                                null,
                                FieldDescriptor.parseDescriptor("Lnet/neoforged/api/distmarker/Dist;"),
                                null
                        )
                )
        );
    }
}
