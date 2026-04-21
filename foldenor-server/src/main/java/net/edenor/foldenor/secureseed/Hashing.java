package net.edenor.foldenor.secureseed;

public class Hashing {
    private static final long[] BLAKE3_IV = {
            0xbb67ae856a09e667L, 0xa54ff53a3c6ef372L,
            0x9b05688c510e527fL, 0x5be0cd191f83d9abL
    };

    private static volatile long[] cachedSaltHash = null;
    private static volatile String lastSalt = null;

    private static long[] getSaltHash() {
        String currentSalt = Globals.getSecureSeedSalt();
        if (cachedSaltHash == null || !currentSalt.equals(lastSalt)) {
            long[] saltLongs = new long[8];
            byte[] saltBytes = currentSalt.getBytes();

            for (int i = 0; i < Math.min(saltBytes.length, 64); i++) {
                int longIndex = i / 8;
                int byteIndex = i % 8;
                saltLongs[longIndex] |= ((long) (saltBytes[i] & 0xFF)) << (byteIndex * 8);
            }

            cachedSaltHash = hashWorldSeedInternal(saltLongs);
            lastSalt = currentSalt;
        }
        return cachedSaltHash;
    }

    private static long[] hashWorldSeedInternal(long[] worldSeed) {
        long[] state = new long[8];
        System.arraycopy(BLAKE3_IV, 0, state, 0, 4);

        long[] paddedInput = new long[8];
        System.arraycopy(worldSeed, 0, paddedInput, 0, Math.min(worldSeed.length, 8));

        processBlock(paddedInput, state);

        return state;
    }

    private static void processBlock(long[] input, long[] state) {
        long[] v = new long[16];

        System.arraycopy(state, 0, v, 0, 4);
        System.arraycopy(BLAKE3_IV, 0, v, 4, 4);
        System.arraycopy(input, 0, v, 8, 8);

        for (int round = 0; round < 5; round++) {
            G(v, 0, 4, 8, 12, input[0], input[1]);
            G(v, 1, 5, 9, 13, input[2], input[3]);
            G(v, 2, 6, 10, 14, input[4], input[5]);
            G(v, 3, 7, 11, 15, input[6], input[7]);

            G(v, 0, 5, 10, 15, input[1], input[2]);
            G(v, 1, 6, 11, 12, input[3], input[4]);
            G(v, 2, 7, 8, 13, input[5], input[6]);
            G(v, 3, 4, 9, 14, input[7], input[0]);

            if (round < 4) {
                long temp = input[0];
                for (int i = 0; i < 7; i++) {
                    input[i] = input[i + 1];
                }
                input[7] = temp;
            }
        }

        for (int i = 0; i < 4; i++) {
            state[i] = v[i] ^ v[i + 8];
        }

        for (int i = 4; i < 8; i++) {
            state[i] = v[i] ^ v[i + 4];
        }
    }

    private static void G(long[] v, int a, int b, int c, int d, long mx, long my) {
        v[a] = v[a] + v[b] + mx;
        v[d] = Long.rotateRight(v[d] ^ v[a], 32);
        v[c] = v[c] + v[d];
        v[b] = Long.rotateRight(v[b] ^ v[c], 24);
        v[a] = v[a] + v[b] + my;
        v[d] = Long.rotateRight(v[d] ^ v[a], 16);
        v[c] = v[c] + v[d];
        v[b] = Long.rotateRight(v[b] ^ v[c], 63);
    }

    public static long[] hashWorldSeed(long[] worldSeed) {
        if (!Globals.isSecureSeedEnabled()) {
            return worldSeed.clone();
        }

        long[] saltHashValue = getSaltHash();
        long[] saltedSeed = new long[worldSeed.length];

        for (int i = 0; i < worldSeed.length; i++) {
            saltedSeed[i] = worldSeed[i] ^ saltHashValue[i % saltHashValue.length];
        }

        return hashWorldSeedInternal(saltedSeed);
    }

    public static long[] expandLevelSeedTo1024Bits(long levelSeed) {
        if (!Globals.isSecureSeedEnabled()) {
            long[] result = new long[Globals.WORLD_SEED_LONGS];
            for (int i = 0; i < Globals.WORLD_SEED_LONGS; i++) {
                result[i] = levelSeed ^ (i * 0x9E3779B97F4A7C15L);
            }
            return result;
        }

        long[] result = new long[Globals.WORLD_SEED_LONGS];
        long[] saltHashValue = getSaltHash();

        for (int segment = 0; segment < Globals.WORLD_SEED_LONGS; segment++) {
            long[] segmentInput = new long[8];
            segmentInput[0] = levelSeed ^ saltHashValue[segment % saltHashValue.length];
            segmentInput[1] = (0x243F6A8885A308D3L + segment) ^ saltHashValue[(segment + 1) % saltHashValue.length];
            segmentInput[2] = (0x13198A2E03707344L + segment) ^ saltHashValue[(segment + 2) % saltHashValue.length];
            segmentInput[3] = (0xA4093822299F31D0L + segment) ^ saltHashValue[(segment + 3) % saltHashValue.length];
            segmentInput[4] = (segment * 0x9E3779B97F4A7C15L) ^ saltHashValue[(segment + 4) % saltHashValue.length];
            segmentInput[5] = (~segment) ^ saltHashValue[(segment + 5) % saltHashValue.length];
            segmentInput[6] = Long.rotateLeft(levelSeed, segment % 64) ^ saltHashValue[(segment + 6) % saltHashValue.length];
            segmentInput[7] = (levelSeed ^ (segment << 32)) ^ saltHashValue[(segment + 7) % saltHashValue.length];

            long[] segmentHash = hashWorldSeedInternal(segmentInput);

            result[segment] = segmentHash[0];

            if (segment > 0) {
                segmentInput[0] = result[segment - 1];
                segmentInput[1] = segmentHash[1];
                segmentHash = hashWorldSeedInternal(segmentInput);
                result[segment] ^= segmentHash[0];
            }
        }
        return hashWorldSeedInternal(result);
    }

    public static long getTerrainSeed(long[] hashedSeed, TerrainType type) {
        return hashedSeed[type.ordinal() % hashedSeed.length];
    }

    public enum TerrainType {
        BASE_TERRAIN,
        BIOME_NOISE,
        CLIMATE,
        AQUIFER,
        ORE,
        SURFACE,
        VEGETATION,
        SHIFT
    }

    public static void hash(long[] message, long[] output, long[] state, int outputBytes, boolean finalBlock) {
        long[] result = hashWorldSeedInternal(message);
        System.arraycopy(result, 0, output, 0, Math.min(result.length, output.length));
    }
}