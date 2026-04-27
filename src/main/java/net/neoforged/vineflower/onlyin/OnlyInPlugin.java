package net.neoforged.vineflower.onlyin;

import org.jetbrains.java.decompiler.api.java.ClassPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.ClassPass;
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
    public void beforeDecompile() {
        manifest = loadManifest();
    }

    @Override
    public void close() {
        manifest = null;
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        // Passes run per method, so we need to track which we added the annotation to already.
        registrar.registerClassPass(ClassPassLocation.BEFORE_PROCESSING, node -> {
            if (!DecompilerContext.getOption(OnlyInOptions.ADD_ONLYIN)) {
                return false;
            }

            if (manifest == null) {
                return false;
            }

            StructClass cl = node.classStruct;

            var entryAttr = manifest.getEntries().get(cl.qualifiedName + ".class");
            if (entryAttr == null) {
                return false; // No dist-specificity
            }

            var entryDist = entryAttr.getValue("Minecraft-Dist");
            if (entryDist != null) {
                AnnotationExprent annotation;
                if ("client".equals(entryDist)) {
                    annotation = ANNOTATION_CLIENT;
                } else if ("server".equals(entryDist)) {
                    annotation = ANNOTATION_SERVER;
                } else {
                    return false; // Unsupported dist.
                }

                // Only add the annotation if it's not already present.
                var annotations = cl.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS);
                if (annotations != null && annotations.getAnnotations().stream().anyMatch(a -> a.getClassName().equals(annotation.getClassName()))) {
                    return false;
                }
                if (annotations == null) {
                    annotations = new StructAnnotationAttribute();
                    cl.getAttributes().put(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_ANNOTATIONS, annotations);
                }
                if (annotations.getAnnotations() == null) {
                    annotations.setAnnotations(new ArrayList<>());
                }
                annotations.getAnnotations().add(annotation);
            }

            return true;
        });
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
        var structContext = DecompilerContext.getStructContext();
        for (ContextUnit unit : structContext.getUnits()) {
            if (!unit.isOwn()) {
                continue;
            }
            for (IContextSource.Entry other : unit.getOtherEntries()) {
                if (other.path().equals("META-INF/MANIFEST.MF")) {
                    DecompilerContext.getLogger().writeMessage("Loading Minecraft-Dist manifest data from " + unit.getName(), IFernflowerLogger.Severity.WARN);
                    try (var input = unit.getStream(other)) {
                        return new Manifest(input);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            }
        }
        return new Manifest();
    }
}
