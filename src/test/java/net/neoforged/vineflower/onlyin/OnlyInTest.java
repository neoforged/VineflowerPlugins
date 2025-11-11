package net.neoforged.vineflower.onlyin;

import net.neoforged.vineflower.testclasses.TestClass;
import net.neoforged.vineflower.testclasses.TestClass2;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OnlyInTest {
    @TempDir
    Path tempDir;

    @Test
    void testWithoutPlugin() throws Exception {
        decompileTestClass("--add-onlyin=0");

        var javaSource = Files.readString(tempDir.resolve("TestClass.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass {
                   public static void method(String param) {
                   }
                }
                """, javaSource);

        var javaSource2 = Files.readString(tempDir.resolve("TestClass2.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass2 {
                   public static void method() {
                   }
                }
                """, javaSource2);
    }

    @Test
    void testWithPlugin() throws Exception {
        decompileTestClass();

        var javaSource = Files.readString(tempDir.resolve("TestClass.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass {
                   public static void method(String param) {
                   }
                }
                """, javaSource);

        var javaSource2 = Files.readString(tempDir.resolve("TestClass2.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                import net.neoforged.api.distmarker.Dist;
                import net.neoforged.api.distmarker.OnlyIn;
                
                @OnlyIn(Dist.CLIENT)
                public class TestClass2 {
                   public static void method() {
                   }
                }
                """, javaSource2);
    }

    private void decompileTestClass(String... extraOptions) throws URISyntaxException, IOException {
        var testClassPath = Path.of(TestClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        testClassPath = testClassPath.resolve(TestClass.class.getName().replace('.', '/') + ".class");

        var testClassPath2 = Path.of(TestClass2.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        testClassPath2 = testClassPath2.resolve(TestClass2.class.getName().replace('.', '/') + ".class");

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Minecraft-Dists", "server client");

        var clientOnlyAttr = new Attributes();
        clientOnlyAttr.putValue("Minecraft-Dist", "client");
        manifest.getEntries().put(TestClass2.class.getName().replace('.', '/') + ".class", clientOnlyAttr);

        var tempJarPath = tempDir.resolve("temp.jar");
        try (var tempJar = new JarOutputStream(new BufferedOutputStream(Files.newOutputStream(tempJarPath)), manifest)) {
            tempJar.putNextEntry(new JarEntry("TestClass.class"));
            Files.copy(testClassPath, tempJar);
            tempJar.closeEntry();
            tempJar.putNextEntry(new JarEntry("TestClass2.class"));
            Files.copy(testClassPath2, tempJar);
            tempJar.closeEntry();
        }

        String[] effectiveOptions = Stream.concat(
                Stream.of(extraOptions),
                Stream.of(
                        tempJarPath.toAbsolutePath().toString(),
                        tempDir.toAbsolutePath().toString()
                )).toArray(String[]::new);

        ConsoleDecompiler.main(effectiveOptions);

        System.gc();
    }
}
