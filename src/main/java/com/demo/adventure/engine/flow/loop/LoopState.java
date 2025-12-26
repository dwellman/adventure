package com.demo.adventure.engine.flow.loop;

/**
 * Runtime loop state (clock + loop counter).
 */
public final class LoopState {
    private final int maxTicks;
    private int loopCount = 1;
    private int clock = 0;
    private int tickRate = 10;

    public LoopState(int maxTicks) {
        this.maxTicks = Math.max(0, maxTicks);
    }

    public LoopResetReason advance() {
        if (maxTicks <= 0) {
            return null;
        }
        clock += Math.max(1, tickRate);
        if (clock >= maxTicks) {
            return LoopResetReason.TIMEOUT;
        }
        return null;
    }

    public void reset() {
        loopCount = Math.max(1, loopCount + 1);
        clock = 0;
    }

    public int loopCount() {
        return loopCount;
    }

    public int clock() {
        return clock;
    }

    public int maxTicks() {
        return maxTicks;
    }

    public int tickRate() {
        return tickRate;
    }

    public void setTickRate(int tickRate) {
        this.tickRate = Math.max(1, tickRate);
    }
}
