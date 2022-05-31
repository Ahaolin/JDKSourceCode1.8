//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package sun.misc;

import java.io.IOException;
import java.lang.Thread.State;
import java.util.Properties;
import sun.misc.Launcher.ExtClassLoader;

public class VM {
    private static boolean suspended = false;
    /** @deprecated */
    @Deprecated
    public static final int STATE_GREEN = 1;
    /** @deprecated */
    @Deprecated
    public static final int STATE_YELLOW = 2;
    /** @deprecated */
    @Deprecated
    public static final int STATE_RED = 3;
    private static volatile boolean booted = false;
    private static final Object lock = new Object();
    private static long directMemory = 67108864L;
    private static boolean pageAlignDirectMemory;
    private static boolean defaultAllowArraySyntax = false;
    private static boolean allowArraySyntax;
    private static final Properties savedProps;
    private static volatile int finalRefCount;
    private static volatile int peakFinalRefCount;
    private static final int JVMTI_THREAD_STATE_ALIVE = 1;
    private static final int JVMTI_THREAD_STATE_TERMINATED = 2;
    private static final int JVMTI_THREAD_STATE_RUNNABLE = 4;
    private static final int JVMTI_THREAD_STATE_BLOCKED_ON_MONITOR_ENTER = 1024;
    private static final int JVMTI_THREAD_STATE_WAITING_INDEFINITELY = 16;
    private static final int JVMTI_THREAD_STATE_WAITING_WITH_TIMEOUT = 32;

    public VM() {
    }

    /** @deprecated */
    @Deprecated
    public static boolean threadsSuspended() {
        return suspended;
    }

    public static boolean allowThreadSuspension(ThreadGroup var0, boolean var1) {
        return var0.allowThreadSuspension(var1);
    }

    /** @deprecated */
    @Deprecated
    public static boolean suspendThreads() {
        suspended = true;
        return true;
    }

    /** @deprecated */
    @Deprecated
    public static void unsuspendThreads() {
        suspended = false;
    }

    /** @deprecated */
    @Deprecated
    public static void unsuspendSomeThreads() {
    }

    /** @deprecated */
    @Deprecated
    public static final int getState() {
        return 1;
    }

    /** @deprecated */
    @Deprecated
    public static void registerVMNotification(VMNotification var0) {
    }

    /** @deprecated */
    @Deprecated
    public static void asChange(int var0, int var1) {
    }

    /** @deprecated */
    @Deprecated
    public static void asChange_otherthread(int var0, int var1) {
    }

    public static void booted() {
        synchronized(lock) {
            booted = true;
            lock.notifyAll();
        }
    }

    public static boolean isBooted() {
        return booted;
    }

    public static void awaitBooted() throws InterruptedException {
        synchronized(lock) {
            while(!booted) {
                lock.wait();
            }

        }
    }

    public static long maxDirectMemory() {
        return directMemory;
    }

    public static boolean isDirectMemoryPageAligned() {
        return pageAlignDirectMemory;
    }

    public static boolean allowArraySyntax() {
        return allowArraySyntax;
    }

    public static boolean isSystemDomainLoader(ClassLoader var0) {
        return var0 == null;
    }

    public static String getSavedProperty(String var0) {
        if (savedProps.isEmpty()) {
            throw new IllegalStateException("Should be non-empty if initialized");
        } else {
            return savedProps.getProperty(var0);
        }
    }

    public static void saveAndRemoveProperties(Properties var0) {
        if (booted) {
            throw new IllegalStateException("System initialization has completed");
        } else {
            savedProps.putAll(var0);
            String var1 = (String)var0.remove("sun.nio.MaxDirectMemorySize");
            if (var1 != null) {
                if (var1.equals("-1")) {
                    directMemory = Runtime.getRuntime().maxMemory();
                } else {
                    long var2 = Long.parseLong(var1);
                    if (var2 > -1L) {
                        directMemory = var2;
                    }
                }
            }

            var1 = (String)var0.remove("sun.nio.PageAlignDirectMemory");
            if ("true".equals(var1)) {
                pageAlignDirectMemory = true;
            }

            var1 = var0.getProperty("sun.lang.ClassLoader.allowArraySyntax");
            allowArraySyntax = var1 == null ? defaultAllowArraySyntax : Boolean.parseBoolean(var1);
            var0.remove("java.lang.Integer.IntegerCache.high");
            var0.remove("sun.zip.disableMemoryMapping");
            var0.remove("sun.java.launcher.diag");
            var0.remove("sun.cds.enableSharedLookupCache");
        }
    }

    public static void initializeOSEnvironment() {
        if (!booted) {
            OSEnvironment.initialize();
        }

    }

    public static int getFinalRefCount() {
        return finalRefCount;
    }

    public static int getPeakFinalRefCount() {
        return peakFinalRefCount;
    }

    public static void addFinalRefCount(int var0) {
        finalRefCount += var0;
        if (finalRefCount > peakFinalRefCount) {
            peakFinalRefCount = finalRefCount;
        }

    }

    public static State toThreadState(int var0) {
        if ((var0 & 4) != 0) {
            return State.RUNNABLE;
        } else if ((var0 & 1024) != 0) {
            return State.BLOCKED;
        } else if ((var0 & 16) != 0) {
            return State.WAITING;
        } else if ((var0 & 32) != 0) {
            return State.TIMED_WAITING;
        } else if ((var0 & 2) != 0) {
            return State.TERMINATED;
        } else {
            return (var0 & 1) == 0 ? State.NEW : State.RUNNABLE;
        }
    }

    public static native ClassLoader latestUserDefinedLoader0();

    public static ClassLoader latestUserDefinedLoader() {
        ClassLoader var0 = latestUserDefinedLoader0();
        if (var0 != null) {
            return var0;
        } else {
            try {
                return ExtClassLoader.getExtClassLoader();
            } catch (IOException var2) {
                return null;
            }
        }
    }

    private static native void initialize();

    static {
        allowArraySyntax = defaultAllowArraySyntax;
        savedProps = new Properties();
        finalRefCount = 0;
        peakFinalRefCount = 0;
        initialize();
    }
}
