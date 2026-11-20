package net.neoforged.vineflower.testclasses.equals.typeuse.invisible;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.CLASS)
public @interface Nullable {
}
