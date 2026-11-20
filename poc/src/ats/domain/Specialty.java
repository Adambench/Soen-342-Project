package ats.domain;

public class Specialty {
    private final String name;

    public Specialty(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}
