package net.neoforged.vineflower.testclasses.equals;

public record TestClass10() {
    @Override
    public boolean equals(Object o) {
        return o instanceof TestClass10;
    }
}
