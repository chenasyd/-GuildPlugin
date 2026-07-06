package com.guild.comm.debug;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Lightweight request tracing context for debugging communication flows.
 *
 * <p>Each trace tracks a logical operation through the bridge,
 * recording timestamps and metadata at each step.
 *
 * <pre>{@code
 *   TraceContext ctx = CommAPI.trace("gui-image-bind");
 *   ctx.step("validate", "guiId=main");
 *   ctx.step("render", "entry=54-default");
 *   ctx.finish("ok");
 *   ctx.dump(logger);  // prints timed trace to console
 * }</pre>
 */
public class TraceContext {

    private final String operation;
    private final List<Step> steps = new ArrayList<>();
    private final long startNanos;
    private String result;
    private long endNanos;

    public TraceContext(String operation) {
        this.operation = operation;
        this.startNanos = System.nanoTime();
    }

    /** Record a step in the trace. */
    public TraceContext step(String name, String detail) {
        steps.add(new Step(name, detail, System.nanoTime()));
        return this;
    }

    /** Record the final result. */
    public TraceContext finish(String result) {
        this.result = result;
        this.endNanos = System.nanoTime();
        return this;
    }

    /** Finish with a formatted result string. */
    public TraceContext finish(String format, Object... args) {
        return finish(String.format(format, args));
    }

    /** Total elapsed time in milliseconds. */
    public double elapsedMs() {
        long end = endNanos > 0 ? endNanos : System.nanoTime();
        return (end - startNanos) / 1_000_000.0;
    }

    /** Dump the trace to a logger at INFO level. */
    public void dump(Logger logger) {
        if (logger == null) return;
        logger.log(Level.INFO, toString());
    }

    /** Dump the trace to a logger at the specified level. */
    public void dump(Logger logger, Level level) {
        if (logger == null) return;
        logger.log(level, toString());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("[Trace] %s (%.2fms) result=%s",
                operation, elapsedMs(), result != null ? result : "pending"));
        if (!steps.isEmpty()) {
            for (Step s : steps) {
                sb.append(String.format("\n  ├─ %s | %s | +%.2fms",
                        s.name, s.detail, s.elapsedFromStart(startNanos)));
            }
        }
        return sb.toString();
    }

    /** @return immutable view of recorded steps. */
    public List<Step> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    // ── Inner class ──────────────────────────────────────────────

    public static class Step {
        public final String name;
        public final String detail;
        public final long timestampNanos;

        Step(String name, String detail, long timestampNanos) {
            this.name = name;
            this.detail = detail;
            this.timestampNanos = timestampNanos;
        }

        double elapsedFromStart(long startNanos) {
            return (timestampNanos - startNanos) / 1_000_000.0;
        }
    }
}
