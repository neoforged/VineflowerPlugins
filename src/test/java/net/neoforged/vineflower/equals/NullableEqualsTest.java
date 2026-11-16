package net.neoforged.vineflower.equals;

import net.neoforged.vineflower.testclasses.TestClass3;
import net.neoforged.vineflower.testclasses.TestClass4;
import net.neoforged.vineflower.testclasses.TestClass5;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NullableEqualsTest {
    @TempDir
    Path tempDir;

    @Test
    void testWithoutPlugin() throws Exception {
        decompileTestClass(TestClass3.class, "--nullable-equals=0");

        var javaSource = Files.readString(tempDir.resolve("TestClass3.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass3 {
                   @Override
                   public boolean equals(Object o) {
                      return false;
                   }
                
                   public boolean equals(Integer i) {
                      return false;
                   }
                
                   public boolean method(Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    @Test
    void testWithPlugin() throws Exception {
        decompileTestClass(TestClass3.class);

        var javaSource = Files.readString(tempDir.resolve("TestClass3.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                import org.jspecify.annotations.Nullable;
                
                public class TestClass3 {
                   @Override
                   public boolean equals(@Nullable Object o) {
                      return false;
                   }
                
                   public boolean equals(Integer i) {
                      return false;
                   }
                
                   public boolean method(Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    @Test
    void testWithPluginExistingOtherAnnotation() throws Exception {
        decompileTestClass(TestClass4.class);

        var javaSource = Files.readString(tempDir.resolve("TestClass4.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                import org.jspecify.annotations.Nullable;
                
                public class TestClass4 {
                   @Override
                   public boolean equals(@Deprecated @Nullable Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    @Test
    void testWithPluginExistingNullableAnnotation() throws Exception {
        decompileTestClass(TestClass5.class);

        var javaSource = Files.readString(tempDir.resolve("TestClass5.java"));
        assertEquals("""
                package net.neoforged.vineflower.testclasses;
                
                public class TestClass5 {
                   @Override
                   public boolean equals(@Nullable Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    private void decompileTestClass(Class<?> clazz, String... extraOptions) throws URISyntaxException {
        var testClassPath = Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        testClassPath = testClassPath.resolve(clazz.getName().replace('.', '/') + ".class");

        String[] effectiveOptions = Stream.concat(
                Stream.of(extraOptions),
                Stream.of(
                        testClassPath.toAbsolutePath().toString(),
                        tempDir.toAbsolutePath().toString()
                )).toArray(String[]::new);

        ConsoleDecompiler.main(effectiveOptions);
    }
}
