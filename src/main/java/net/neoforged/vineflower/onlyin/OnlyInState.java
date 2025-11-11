package net.neoforged.vineflower.onlyin;

import org.jetbrains.java.decompiler.main.DecompilerContext;
import org.jetbrains.java.decompiler.main.extern.IContextSource;
import org.jetbrains.java.decompiler.struct.ContextUnit;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.jar.Manifest;

class OnlyInState {
    final Manifest manifest;

    public OnlyInState() {
        manifest = loadManifest();
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
