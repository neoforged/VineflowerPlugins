package net.neoforged.vineflower.equals;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.TypeAnnotation;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationParameterAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructTypeAnnotationAttribute;
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

            StructMethod method = context.getMethod();
            if (!method.getName().equals(EQUALS_NAME) || !method.getDescriptor().equals(EQUALS_DESC)) {
                return true;
            }

            StructTypeAnnotationAttribute typeAnnotations = method.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_TYPE_ANNOTATIONS);
            if (hasNullableTypeAnnotation(typeAnnotations)) {
                return true;
            }

            StructAnnotationParameterAttribute paramAnnotations = method.getAttribute(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS);
            if (paramAnnotations == null) {
                paramAnnotations = new StructAnnotationParameterAttribute();
                method.getAttributes().put(StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS, paramAnnotations);
            }
            List<List<AnnotationExprent>> annotations = paramAnnotations.getParamAnnotations();
            if (annotations == null) {
                annotations = new ArrayList<>(1);
                paramAnnotations.setParamAnnotations(annotations);
            }
            List<AnnotationExprent> firstParamAnnotations;
            if (annotations.isEmpty()) {
                firstParamAnnotations = new ArrayList<>(1);
                annotations.add(firstParamAnnotations);
            } else {
                firstParamAnnotations = annotations.getFirst();
            }
            if (firstParamAnnotations.stream().noneMatch(NullableEqualsPlugin::isNullableAnnotation)) {
                // Whether this needs to be added to parameter or type annotations depends on the configured annotation, but we have no guaranteed way to check
                // that it is a type-use annotation (JSpecify and JetBrains are, JSR305 is not), so we go for the safe option and hope no plugin relies on this
                String annoType = (String) DecompilerContext.getProperty(NullableEqualsOptions.NULLABLE_TYPE);
                firstParamAnnotations.add(new AnnotationExprent(annoType, List.of(), List.of()));
            }

            return true;
        }));
    }

    private static boolean hasNullableTypeAnnotation(StructTypeAnnotationAttribute annotationAttribute) {
        if (annotationAttribute == null) {
            return false;
        }
        List<TypeAnnotation> annotations = annotationAttribute.getAnnotations();
        if (annotations == null || annotations.isEmpty()) {
            return false;
        }
        return annotations.stream().anyMatch(anno -> anno.getTargetType() == TypeAnnotation.METHOD_PARAMETER && isNullableAnnotation(anno.getAnnotation()));
    }

    private static boolean isNullableAnnotation(AnnotationExprent annotation) {
        return annotation.getClassName().endsWith(NULLABLE_SUFFIX);
    }

    @Override
    public PluginOptions getPluginOptions() {
        return () -> Pair.of(NullableEqualsOptions.class, NullableEqualsOptions::addDefaults);
    }
}
