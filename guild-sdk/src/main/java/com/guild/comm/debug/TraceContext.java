package com.guild.comm.debug;

/**
 * SDK stub for {@link TraceContext}.
 */
public class TraceContext {

    public TraceContext(String operation) {}

    public TraceContext step(String name, String detail) { return this; }
    public TraceContext finish(String result) { return this; }
    public TraceContext finish(String format, Object... args) { return this; }
    public double elapsedMs() { return 0; }
    public void dump(java.util.logging.Logger logger) {}
    public void dump(java.util.logging.Logger logger, java.util.logging.Level level) {}
    public java.util.List<Step> getSteps() { return java.util.Collections.emptyList(); }

    public static class Step {
        public final String name;
        public final String detail;
        public final long timestampNanos;
        Step(String name, String detail, long timestampNanos) {
            this.name = name; this.detail = detail; this.timestampNanos = timestampNanos;
        }
        double elapsedFromStart(long startNanos) { return 0; }
    }
}
