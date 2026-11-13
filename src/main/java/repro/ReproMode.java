package repro;

public enum ReproMode {
    OFF, MANUAL, AUTO;

    private static volatile ReproMode current = OFF;

    public static ReproMode current() {
        return current;
    }

    public static void set(ReproMode mode) {
        current = mode;
    }
}
