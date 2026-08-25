package ru.reset.rzero.access;

public interface IRZeroRandomState {
    long[] rzero$getState();
    void rzero$setState(long[] state);
    void rzero$setIsLevelRandom(boolean isLevelRandom);
    boolean rzero$isLevelRandom();
}