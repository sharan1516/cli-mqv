package org.example;

public record Pair<F, S>(F first, S second) {
    @Override
    public String toString() {
        return "Pair{" +
                "first=" + first +
                ", second=" + second +
                '}';
    }
}
