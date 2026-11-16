package net.neoforged.vineflower.equals;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationParameterAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class NullableEqualsPlugin implements Plugin {
    private static final String EQUALS_NAME = "equals";
    private static final String EQUALS_DESC = "(Ljava/lang/Object;)Z";
    private static final String NULLABLE_SUFFIX = "/Nullable";

    @Override
    public String id() {
        return "neoforge-vineflower-nullable-equals";
    }

    @Override
    public String description() {
        return "Add Nullable annotation to parameter of Object#equals() overrides";
    }

    @Override
    public void registerJavaPasses(JavaPassRegistrar registrar) {
        registrar.register(JavaPassLocation.AT_END, new NamedPass(id(), context -> {
            if (!DecompilerContext.getOption(NullableEqualsOptions.NULLABLE_EQUALS)) {
                return false;
            }

            String annoType = (String) DecompilerContext.getProperty(NullableEqualsOptions.NULLABLE_TYPE);
            StructMethod method = context.getMethod();
            if (method.getName().equals(EQUALS_NAME) && method.getDescriptor().equals(EQUALS_DESC)) {
                var paramAnnotations = method.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS);
                if (paramAnnotations == null) {
                    paramAnnotations = new StructAnnotationParameterAttribute();
                    method.getAttributes().put(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS, paramAnnotations);
                }
                List<List<AnnotationExprent>> annotations = paramAnnotations.getParamAnnotations();
                List<AnnotationExprent> firstParamAnnotations;
                if (annotations == null) {
                    annotations = new ArrayList<>(1);
                    paramAnnotations.setParamAnnotations(annotations);
                }
                if (annotations.isEmpty()) {
                    firstParamAnnotations = new ArrayList<>(1);
                    annotations.add(firstParamAnnotations);
                } else {
                    firstParamAnnotations = annotations.getFirst();
                }
                if (firstParamAnnotations.stream().noneMatch(anno -> anno.getClassName().endsWith(NULLABLE_SUFFIX))) {
                    firstParamAnnotations.add(new AnnotationExprent(annoType, List.of(), List.of()));
                }
            }

            return true;
        }));
    }

    @Override
    public PluginOptions getPluginOptions() {
        return () -> Pair.of(NullableEqualsOptions.class, NullableEqualsOptions::addDefaults);
    }
}
