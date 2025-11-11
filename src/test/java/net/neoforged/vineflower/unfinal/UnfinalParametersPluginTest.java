package net.neoforged.vineflower.unfinal;

import net.neoforged.vineflower.testclasses.TestClass;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnfinalParametersPluginTest {
    @TempDir
    Path tempDir;

    @Test
    void testWithoutPlugin() throws Exception {
        decompileTestClass("--unfinal-params=0");

        var javaSource = Files.readString(tempDir.resolve("TestClass.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass {
                   public static void method(final String param) {
                   }
                }
                """, javaSource);
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
    }

    private void decompileTestClass(String... extraOptions) throws URISyntaxException {
        var testClassPath = Path.of(TestClass.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        testClassPath = testClassPath.resolve(TestClass.class.getName().replace('.', '/') + ".class");

        String[] effectiveOptions = Stream.concat(
                Stream.of(extraOptions),
                Stream.of(
                        testClassPath.toAbsolutePath().toString(),
                        tempDir.toAbsolutePath().toString()
                )).toArray(String[]::new);

        ConsoleDecompiler.main(effectiveOptions);
    }
}
