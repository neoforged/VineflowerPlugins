# NeoForge Vineflower Plugins

This repository contains two plugins for [Vineflower](https://github.com/Vineflower/vineflower). They are used
for decompiling Minecraft with the purpose of preparing the decompiled source code for making it recompilable and
easier to patch.

## Unfinal Parameters

This plugin will remove `final` from all method parameters, for two reasons:

- To make them easier to read when reading through Minecraft source code.
- To make it easier to patch methods (i.e. overwrite the value of a method parameter as the first thing in the method).

Since we don't write large amounts of code in Minecraft itself, the original purpose of final parameters is lost on us.

This plugin is enabled by default, but can be disabled using the `-unfinal-params=0` command line option.

## Add OnlyIn Annotations

We generally decompile a "joined" Minecraft, which is client and server jars combined into a single jar-file.
During that process, Java Jar Manifest entries get added to still be able to identify files that were only present
in either one jar (and not common between both).

This Vineflower plugin will use that Manifest information to add `@OnlyIn` annotations on classes that were exclusive
to client or server. These annotations are now purely for documentation purposes.

This plugin is enabled by default, but can be disabled using the `-add-onlyin=0` command line option.

## Add Nullable Annotation to Object#equals() Overrides

This plugin adds `@Nullable` annotations to the parameter of `Object#equals()` overrides to satisfy null-safety checks
by static analysis tools when the containing class or its containing package is null-marked.
If the parameter of such an override already specifies any runtime-visible or runtime-invisible parameter or type-use
annotation named `Nullable` then it is skipped.

By default, this plugin will use the JSpecify `@Nullable` annotation, but this can be changed by passing the
fully-qualified, slash-separated name of the annotation class to the `-nullable-equals-anno-type` command line option.

This plugin is enabled by default, but can be disabled using the `-nullable-equals=0` command line option.
