package net.neoforged.vineflower.onlyin;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.main.extern.IFernflowerLogger;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.FieldExprent;
import org.jetbrains.java.decompiler.struct.ContextUnit;
import org.jetbrains.java.decompiler.struct.StructClass;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.gen.FieldDescriptor;
import org.jetbrains.java.decompiler.util.Key;
import org.jetbrains.java.decompiler.util.Pair;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Manifest;

public class OnlyInPlugin implements Plugin {
    private static final AnnotationExprent ANNOTATION_CLIENT = makeAnnotation("CLIENT");
    private static final AnnotationExprent ANNOTATION_SERVER = makeAnnotation("DEDICATED_SERVER");

    private volatile Manifest manifest;

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

            Manifest manifest = getOrLoadManifest();

            StructClass parent = context.getEnclosingClass();
            if (parent.qualifiedName.contains("$")) {
                return true; // dont handle inner classes etc.
            }

            var entryAttr = manifest.getEntries().get(parent.qualifiedName + ".class");
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

    private Manifest getOrLoadManifest() {
        Manifest manifest = this.manifest;
        if (manifest == null) {
            synchronized (this) {
                manifest = this.manifest;
                if (manifest == null) {
                    manifest = this.manifest = loadManifest();
                }
            }
        }
        return manifest;
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

    private static Manifest loadManifest() {
        try {
            var structContext = DecompilerContext.getStructContext();
            var unitsField = structContext.getClass().getDeclaredField("units");
            unitsField.setAccessible(true);
            List<?> units = (List<?>) unitsField.get(structContext);
            for (Object unitObj : units) {
                if (!((ContextUnit) unitObj).isOwn()) {
                    continue;
                }
                var sourceField = unitObj.getClass().getDeclaredField("source");
                sourceField.setAccessible(true);
                var source = (IContextSource) sourceField.get(unitObj);
                for (IContextSource.Entry other : source.getEntries().others()) {
                    if (other.path().equals("META-INF/MANIFEST.MF")) {
                        DecompilerContext.getLogger().writeMessage("Loading Minecraft-Dist manifest data from " + source.getName(), IFernflowerLogger.Severity.WARN);
                        try (var input = source.getInputStream(other)) {
                            return new Manifest(input);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return new Manifest();
    }
}
