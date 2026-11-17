package net.neoforged.vineflower.equals;

import net.neoforged.vineflower.testclasses.equals.TestClass3;
import net.neoforged.vineflower.testclasses.equals.TestClass4;
import net.neoforged.vineflower.testclasses.equals.param.TestClass5;
import net.neoforged.vineflower.testclasses.equals.typeuse.TestClass6;
import org.jetbrains.java.decompiler.main.decompiler.ConsoleDecompiler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NullableEqualsTest {
    @TempDir
    Path tempDir;

    @Test
    void testWithoutPlugin() throws Exception {
        String javaSource = decompileTestClass(TestClass3.class, "--nullable-equals=0");
        assertEquals("""
                package net.neoforged.vineflower.testclasses.equals;
                
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
        String javaSource = decompileTestClass(TestClass3.class);
        assertEquals("""
                package net.neoforged.vineflower.testclasses.equals;
                
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
        String javaSource = decompileTestClass(TestClass4.class);
        assertEquals("""
                package net.neoforged.vineflower.testclasses.equals;
                
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
    void testWithPluginExistingParamNullableAnnotation() throws Exception {
        String javaSource = decompileTestClass(TestClass5.class);
        assertEquals("""
                package net.neoforged.vineflower.testclasses.equals.param;
                
                public class TestClass5 {
                   @Override
                   public boolean equals(@Nullable Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    @Test
    void testWithPluginExistingTypeUseNullableAnnotation() throws Exception {
        String javaSource = decompileTestClass(TestClass6.class);
        assertEquals("""
                package net.neoforged.vineflower.testclasses.equals.typeuse;
                
                public class TestClass6 {
                   @Override
                   public boolean equals(@Nullable Object o) {
                      return false;
                   }
                }
                """, javaSource);
    }

    private String decompileTestClass(Class<?> clazz, String... extraOptions) throws Exception {
        var testClassPath = Path.of(clazz.getProtectionDomain().getCodeSource().getLocation().toURI());
        testClassPath = testClassPath.resolve(clazz.getName().replace('.', '/') + ".class");

        String[] effectiveOptions = Stream.concat(
                Stream.of(extraOptions),
                Stream.of(
                        testClassPath.toAbsolutePath().toString(),
                        tempDir.toAbsolutePath().toString()
                )).toArray(String[]::new);

        ConsoleDecompiler.main(effectiveOptions);

        return Files.readString(tempDir.resolve(clazz.getSimpleName() + ".java"));
    }
}
