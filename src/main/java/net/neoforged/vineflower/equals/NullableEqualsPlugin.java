package net.neoforged.vineflower.equals;

import org.jetbrains.java.decompiler.api.java.JavaPassLocation;
import org.jetbrains.java.decompiler.api.java.JavaPassRegistrar;
import org.jetbrains.java.decompiler.api.plugin.Plugin;
import org.jetbrains.java.decompiler.api.plugin.PluginOptions;
import org.jetbrains.java.decompiler.api.plugin.pass.NamedPass;
import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.modules.decompiler.RecordHelper;
import org.jetbrains.java.decompiler.modules.decompiler.exps.AnnotationExprent;
import org.jetbrains.java.decompiler.modules.decompiler.exps.TypeAnnotation;
import org.jetbrains.java.decompiler.struct.StructMethod;
import org.jetbrains.java.decompiler.struct.attr.StructAnnotationParameterAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructGeneralAttribute;
import org.jetbrains.java.decompiler.struct.attr.StructTypeAnnotationAttribute;
import org.jetbrains.java.decompiler.util.Key;
import org.jetbrains.java.decompiler.util.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Stream;

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
                return false;
            }

            // Skip adding the annotation in the case of a compiler-generated equals for records.
            //
            // Records are special because the equals method is filled in by the compiler if it is not explicitly defined in source.
            // For compiler-generated equals, the JSpecify spec (v1.0.0) holds that it will always accept a null as an argument (UNION_NULL).
            //
            // Additionally, a bug exists where even if the sole use of the annotation is in the compiler-generated equals which is omitted
            // by the printer, the import for the annotation is still added. This works around that bug.
            if (context.getEnclosingClass().getRecordComponents() != null && RecordHelper.isHiddenRecordMethod(context.getEnclosingClass(), context.getMethod(), context.getRoot())) {
                return false;
            }

            if (hasNullableTypeAnnotation(method, StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_TYPE_ANNOTATIONS)) {
                return false;
            }
            if (hasNullableTypeAnnotation(method, StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_TYPE_ANNOTATIONS)) {
                return false;
            }
            if (hasNullableParameterAnnotation(method, StructGeneralAttribute.ATTRIBUTE_RUNTIME_VISIBLE_PARAMETER_ANNOTATIONS)) {
                return false;
            }
            if (hasNullableParameterAnnotation(method, StructGeneralAttribute.ATTRIBUTE_RUNTIME_INVISIBLE_PARAMETER_ANNOTATIONS)) {
                return false;
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
            // Whether this needs to be added to parameter or type annotations and runtime-visible or runtime-invisible annotations depends on the configured
            // annotation, but we have no guaranteed way to check that it is a type-use annotation (JSpecify and JetBrains are, JSR305 is not), so we go for
            // the safe option (runtime-visible parameter annotations) and hope no plugin relies on this being 100% correct
            String annoType = (String) DecompilerContext.getProperty(NullableEqualsOptions.NULLABLE_TYPE);
            firstParamAnnotations.add(new AnnotationExprent(annoType, List.of(), List.of()));

            return true;
        }));
    }

    private static boolean hasNullableTypeAnnotation(StructMethod method, Key<StructTypeAnnotationAttribute> attribute) {
        return hasNullableAnnotation(
                method,
                attribute,
                StructTypeAnnotationAttribute::getAnnotations,
                (ignored, anno) -> anno.getTargetType() == TypeAnnotation.METHOD_PARAMETER && anno.getIndex() == 0,
                anno -> Stream.of(anno.getAnnotation())
        );
    }

    private static boolean hasNullableParameterAnnotation(StructMethod method, Key<StructAnnotationParameterAttribute> attribute) {
        return hasNullableAnnotation(
                method,
                attribute,
                StructAnnotationParameterAttribute::getParamAnnotations,
                (idx, ignored) -> idx == 0,
                List::stream
        );
    }

    private static <ATTR extends StructGeneralAttribute, ENTRY> boolean hasNullableAnnotation(
            StructMethod method,
            Key<ATTR> attributeKey,
            Function<ATTR, List<ENTRY>> entryListGetter,
            BiPredicate<Integer, ENTRY> entryFilter,
            Function<ENTRY, Stream<AnnotationExprent>> exprentGetter
    ) {
        ATTR attribute = method.getAttribute(attributeKey);
        if (attribute == null) {
            return false;
        }
        List<ENTRY> entries = entryListGetter.apply(attribute);
        if (entries == null || entries.isEmpty()) {
            return false;
        }
        for (int i = 0; i < entries.size(); i++) {
            ENTRY entry = entries.get(i);
            if (!entryFilter.test(i, entry)) {
                continue;
            }
            if (exprentGetter.apply(entry).anyMatch(exprent -> exprent.getClassName().endsWith(NULLABLE_SUFFIX))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public PluginOptions getPluginOptions() {
        return () -> Pair.of(NullableEqualsOptions.class, NullableEqualsOptions::addDefaults);
    }
}
