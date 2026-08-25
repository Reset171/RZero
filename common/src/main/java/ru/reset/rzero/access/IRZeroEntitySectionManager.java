package ru.reset.rzero.access;

import java.util.UUID;

public interface IRZeroEntitySectionManager {
    boolean rzero$eradicate(UUID uuid);
    void rzero$eradicateAllExcept(java.util.Set<UUID> keep);
}
