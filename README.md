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
