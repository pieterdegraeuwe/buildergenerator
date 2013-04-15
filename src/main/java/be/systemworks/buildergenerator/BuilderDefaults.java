package be.systemworks.buildergenerator;

public interface BuilderDefaults<T extends Builder> {
    public void applyDefaults(T builder);
}
