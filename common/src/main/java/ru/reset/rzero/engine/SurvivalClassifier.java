package ru.reset.rzero.engine;

public final class SurvivalClassifier {

    private SurvivalClassifier() {}

    private static double[] danger() { return new double[] { 1.0, 0.0 }; }
    private static double[] safe()   { return new double[] { 0.0, 1.0 }; }
    private static double[] mix(double pSafe) {
        if (pSafe < 0.0) pSafe = 0.0;
        if (pSafe > 1.0) pSafe = 1.0;
        return new double[] { 1.0 - pSafe, pSafe };
    }

    private static final int F_DIM = 0;
    private static final int F_Y = 1;
    private static final int F_LIGHT = 2;
    private static final int F_VOID_DIST = 3;
    private static final int F_ANCHOR_DIST = 4;
    private static final int F_NEAR_PORTAL = 5;
    private static final int F_BOSS_DEAD = 6;
    private static final int F_HP = 7;
    private static final int F_ARMOR = 8;
    private static final int F_FOOD = 9;
    private static final int F_MOBS_TRACK = 10;
    private static final int F_MOBS_ATTACK = 11;
    private static final int F_SPEED = 12;
    private static final int F_LIFETIME = 13;
    private static final int F_HEALS = 14;
    private static final int F_EXPLOSIVE = 15;
    private static final int F_BUNKER = 16;
    private static final int F_THREAT_DPS = 17;
    private static final int F_WEP_TIER = 18;
    private static final int F_JITTER = 19;
    private static final int F_DMG_DEALT = 20;
    private static final int F_DMG_TAKEN = 21;
    private static final int F_COMBAT_SEC = 22;
    private static final int F_ITEM_BURN = 23;
    private static final int F_FREE_SPACE = 24;
    private static final int F_CORNERED = 25;
    private static final int F_CLIFFS = 26;
    private static final int F_VIS_THREATS = 27;
    private static final int F_BLIND_THREATS = 28;
    private static final int F_PROJ_AIR = 29;
    private static final int F_HIGH_GROUND = 30;
    private static final int F_STEALTH_TENS = 31;
    private static final int F_DEATH_STREAK = 32;
    private static final int F_DMG_BEHIND = 33;
    private static final int F_SHIELD_OFF = 34;
    private static final int F_VISION_DIST = 35;
    private static final int F_SIM_LETHAL = 36;

    private static final int N = 37;

    private static final double[] BASELINE = new double[N];
    private static final double[] SCALE    = new double[N];
    private static final int[]    SIGN     = new int[N];

    static {
        set(F_DIM,            -0.162,    0.855,  -1);
        set(F_Y,              68.44,    73.11,   +1);
        set(F_LIGHT,           1.87,     9.45,   +1);
        set(F_VOID_DIST,     234.23,   889.52,   +1);
        set(F_ANCHOR_DIST,   140.74,   835.38,   -1);
        set(F_NEAR_PORTAL,     0.000,    0.095,  +1);
        set(F_BOSS_DEAD,       0.000,    1.000,   0);
        set(F_HP,             13.31,   11.43,   -1);
        set(F_ARMOR,          15.64,    9.71,   -1);
        set(F_FOOD,           17.21,    5.18,   +1);
        set(F_MOBS_TRACK,      0.100,    1.989,  +1);
        set(F_MOBS_ATTACK,     0.001,    0.675,  +1);
        set(F_SPEED,           0.099,    0.662,  +1);
        set(F_LIFETIME,      137.08,   268.31,   -1);
        set(F_HEALS,           1.80,     2.69,   -1);
        set(F_EXPLOSIVE,       0.000,    0.312,  +1);
        set(F_BUNKER,          0.021,    0.259,  -1);
        set(F_THREAT_DPS,      0.063,    7.359,  +1);
        set(F_WEP_TIER,        4.70,     5.60,   -1);
        set(F_JITTER,         76.71,   253.06,   -1);
        set(F_DMG_DEALT,       0.145,    4.82,   +1);
        set(F_DMG_TAKEN,       0.009,    9.86,   +1);
        set(F_COMBAT_SEC,      0.000,   15.92,   +1);
        set(F_ITEM_BURN,       0.065,    0.456,  -1);
        set(F_FREE_SPACE,     46.48,    55.05,   -1);
        set(F_CORNERED,        0.037,    0.304,  +1);
        set(F_CLIFFS,          0.786,   10.16,   +1);
        set(F_VIS_THREATS,     0.087,    2.15,   +1);
        set(F_BLIND_THREATS,   0.179,    2.09,   +1);
        set(F_PROJ_AIR,        0.248,    2.04,   -1);
        set(F_HIGH_GROUND,     0.287,    2.76,   -1);
        set(F_STEALTH_TENS,    0.117,    4.79,   -1);
        set(F_DEATH_STREAK,    7.46,    30.91,   +1);
        set(F_DMG_BEHIND,      0.000,    2.53,   +1);
        set(F_SHIELD_OFF,      0.000,    0.050,  +1);
        set(F_VISION_DIST,     6.81,    12.62,   +1);
        set(F_SIM_LETHAL,      0.006,    0.185,  +1);
    }

    private static void set(int i, double mu, double scale, int sign) {
        BASELINE[i] = mu;
        SCALE[i]    = (scale < 1.0e-3) ? 1.0e-3 : scale;
        SIGN[i]     = sign;
    }

    private static final double[] M_OPEN_SUSTAINED = {
        -0.974, 41.62, 14.02, 819.79, 34.12, 0.000, 0.000, 11.12, 11.14, 16.96,
         0.016,  0.001,  0.116, 35.31, 0.032, 0.000, 0.002,  0.143,  3.72,  1.83,
         0.042,  3.57,   5.21,  0.002, 0.803, 0.001, 0.158,  0.029,  0.053, 0.004,
        -0.022,  0.000,  4.99,  0.023, 0.000,14.35,  0.005,
    };
    private static final double[] M_EXPLOSIVE = {
         0.000, 67.89,  0.58,128.47, 64.36, 0.063, 0.000, 14.24, 15.97, 17.36,
         0.416, 0.032,  0.094, 85.59, 1.151, 1.000, 0.008,  1.186,  5.66,  9.45,
         0.333, 0.152,  6.72,  0.009,14.11,  0.006, 0.059,  0.941,  0.500, 0.233,
         0.028, 0.000,  2.21,  0.027, 0.000, 8.05,  0.017,
    };
    private static final double[] M_SHIELD_SWARM = {
        -0.014, 74.33,  6.37,319.70, 20.81, 0.000, 0.000, 10.80, 14.97, 17.66,
         1.860, 0.567,  0.149, 24.96, 1.191, 0.003, 0.059,  8.353,  4.92, 94.64,
         1.424,11.97,   4.29,  0.010,44.20,  0.167, 1.757,  0.914,  1.558, 0.596,
        -0.123, 0.000, 45.85,  1.648, 0.022, 5.91,  0.154,
    };
    private static final double[] M_FALL = {
        -0.328, 99.86,  0.71, 56.63,117.79, 0.000, 0.000, 15.89, 17.98, 17.73,
         0.003, 0.000,  1.588, 44.14, 0.626, 0.000, 0.001,  0.079,  3.08,  0.797,
         0.008, 0.030,  2.36,  0.002, 0.027, 0.001, 0.005,  0.023,  0.069, 0.007,
        -0.033, 0.000, 11.06,  0.004, 0.000,16.73,  0.005,
    };
    private static final double[] M_SWARM = {
        -0.947, 71.46,  2.78,335.75, 36.37, 0.006, 0.000,  9.50, 16.38, 15.67,
         1.148, 0.180,  0.136, 41.01, 0.161, 0.002, 0.017,  2.151,  5.15,123.44,
         1.428, 1.971,  9.50,  0.015,39.79,  0.045, 3.621,  0.605,  0.333, 0.021,
        -0.188, 0.032,  7.35,  0.267, 0.000, 9.80,  0.024,
    };
    private static final double[] M_ATTRITION = {
        -0.106, 66.96,  1.16,584.79,617.47, 0.000, 0.000,  7.31,  8.79, 18.39,
         0.421, 0.070,  0.153,427.46, 1.211, 0.005, 0.012,  0.896,  2.70, 34.63,
         0.317, 0.506, 31.59,  0.024,18.87,  0.043, 0.346,  0.140,  0.095, 0.044,
         0.071, 0.000,  1.43,  0.046, 0.000, 6.16,  0.015,
    };

    private static final double[][] MEMORIES = {
        M_OPEN_SUSTAINED, M_EXPLOSIVE, M_SHIELD_SWARM, M_FALL, M_SWARM, M_ATTRITION,
    };

    private static final double W_DREAD       = 0.22;
    private static final double W_HOPE        = 0.40;
    private static final double W_MEMORY      = 2.10;
    private static final double LOGIT_BIAS    = +0.30;
    private static final double RBF_BANDWIDTH = 60.0;

    public static double[] score(double[] in) {
        if (in == null || in.length < N) {
            return mix(0.5);
        }

        double hp        = in[F_HP];
        double simLethal = in[F_SIM_LETHAL];
        double speed     = in[F_SPEED];
        double voidDist  = in[F_VOID_DIST];
        double y         = in[F_Y];
        double explosive = in[F_EXPLOSIVE];
        double combatSec = in[F_COMBAT_SEC];
        double mobsAtk   = in[F_MOBS_ATTACK];
        double dmgTaken  = in[F_DMG_TAKEN];
        double threatDps = in[F_THREAT_DPS];

        if (hp <= 1.0)                                   return danger();
        if (simLethal >= 0.95)                           return danger();
        if (speed >= 1.5 && hp < 12.0)                   return danger();
        if (explosive >= 0.5 && hp < 8.0)                return danger();
        if (explosive >= 0.5 && combatSec >= 1.0)        return danger();
        if (voidDist <= 1.0 && y < 10.0)                 return danger();
        if (in[F_SHIELD_OFF] >= 0.5 && mobsAtk >= 1.0)   return danger();

        if (combatSec == 0.0 && mobsAtk == 0.0 && dmgTaken == 0.0
                && threatDps == 0.0 && explosive < 0.5
                && speed < 0.15 && hp >= 18.0
                && in[F_DMG_BEHIND] == 0.0
                && in[F_VIS_THREATS] == 0.0
                && in[F_BLIND_THREATS] == 0.0) {
            return safe();
        }

        double dread = 0.0;
        double hope  = 0.0;
        for (int i = 0; i < N; i++) {
            int s = SIGN[i];
            if (s == 0) continue;
            double z = (in[i] - BASELINE[i]) / SCALE[i] * s;
            if (z >  6.0) z =  6.0;
            if (z < -6.0) z = -6.0;
            if (z > 0.0) dread += z;
            else         hope  += -z;
        }

        double worstMemory = 0.0;
        for (double[] m : MEMORIES) {
            double d2 = 0.0;
            for (int i = 0; i < N; i++) {
                if (SIGN[i] == 0) continue;
                double dx = (in[i] - m[i]) / SCALE[i];
                d2 += dx * dx;
            }
            double sim = Math.exp(-d2 / RBF_BANDWIDTH);
            if (sim > worstMemory) worstMemory = sim;
        }

        double dimAdj = 0.0;
        double dim = in[F_DIM];
        if (dim < -0.5)      dimAdj -= 0.30;
        else if (dim > 0.5)  dimAdj -= 0.40 - 0.30 * in[F_BOSS_DEAD];
        if (in[F_NEAR_PORTAL] >= 0.5) dimAdj -= 0.10;

        double logit = W_HOPE * hope - W_DREAD * dread - W_MEMORY * worstMemory
                     + dimAdj + LOGIT_BIAS;

        double pSafe = 1.0 / (1.0 + Math.exp(-logit));
        return mix(pSafe);
    }
}