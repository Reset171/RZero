package ru.reset.rzero;

import net.minecraft.world.level.GameRules;

public class ModGameRules {
    public static GameRules.Key<GameRules.BooleanValue> RULE_AUTO_SAVE;
    public static GameRules.Key<GameRules.IntegerValue> RULE_FIXED_INTERVAL;
    public static GameRules.Key<GameRules.BooleanValue> RULE_USE_RANDOM_INTERVAL;
    public static GameRules.Key<GameRules.IntegerValue> RULE_RANDOM_MIN;
    public static GameRules.Key<GameRules.IntegerValue> RULE_RANDOM_MAX;
    public static GameRules.Key<GameRules.BooleanValue> RULE_PLAY_ROLLBACK_SOUND;
    public static GameRules.Key<GameRules.BooleanValue> RULE_USE_ADAPTIVE_MODE;
    public static GameRules.Key<GameRules.IntegerValue> RULE_ADAPTIVE_INTERVAL;
}