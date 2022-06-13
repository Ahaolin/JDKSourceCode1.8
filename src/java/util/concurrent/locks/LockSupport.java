/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent.locks;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * <p>
 *      JDK中的rt.jar包里面的LockSupport是个工具类，它的主要作用是挂起和唤醒线程，工具类是创建锁和其他同步类的基础。<br>
 *      LockSupport类与每个使用它的线程都会关联一个许可证，在默认情况下调用LockSupport类的方法的线程是不持有许可证的。LockSupport是使用{@link Unsafe}类实现的 <br>
 *      示例如下：{@link TestLockSupport}、{@link FIFOMutex}
 * </p><br>
 *
 * Basic thread blocking primitives for creating locks and other
 * synchronization classes.
 *
 * <p>This class associates, with each thread that uses it, a permit
 * (in the sense of the {@link java.util.concurrent.Semaphore
 * Semaphore} class). A call to {@code park} will return immediately
 * if the permit is available, consuming it in the process; otherwise
 * it <em>may</em> block.  A call to {@code unpark} makes the permit
 * available, if it was not already available. (Unlike with Semaphores
 * though, permits do not accumulate. There is at most one.)
 *
 * <p>Methods {@code park} and {@code unpark} provide efficient
 * means of blocking and unblocking threads that do not encounter the
 * problems that cause the deprecated methods {@code Thread.suspend}
 * and {@code Thread.resume} to be unusable for such purposes: Races
 * between one thread invoking {@code park} and another thread trying
 * to {@code unpark} it will preserve liveness, due to the
 * permit. Additionally, {@code park} will return if the caller's
 * thread was interrupted, and timeout versions are supported. The
 * {@code park} method may also return at any other time, for "no
 * reason", so in general must be invoked within a loop that rechecks
 * conditions upon return. In this sense {@code park} serves as an
 * optimization of a "busy wait" that does not waste as much time
 * spinning, but must be paired with an {@code unpark} to be
 * effective.
 *
 * <p>The three forms of {@code park} each also support a
 * {@code blocker} object parameter. This object is recorded while
 * the thread is blocked to permit monitoring and diagnostic tools to
 * identify the reasons that threads are blocked. (Such tools may
 * access blockers using method {@link #getBlocker(Thread)}.)
 * The use of these forms rather than the original forms without this
 * parameter is strongly encouraged. The normal argument to supply as
 * a {@code blocker} within a lock implementation is {@code this}.
 *
 * <p>These methods are designed to be used as tools for creating
 * higher-level synchronization utilities, and are not in themselves
 * useful for most concurrency control applications.  The {@code park}
 * method is designed for use only in constructions of the form:
 *
 *  <pre> {@code
 * while (!canProceed()) { ... LockSupport.park(this); }}</pre>
 *
 * where neither {@code canProceed} nor any other actions prior to the
 * call to {@code park} entail locking or blocking.  Because only one
 * permit is associated with each thread, any intermediary uses of
 * {@code park} could interfere with its intended effects.
 *
 * <p><b>Sample Usage.</b> Here is a sketch of a first-in-first-out
 * non-reentrant lock class:
 *  <pre> {@code
 * class FIFOMutex {
 *   private final AtomicBoolean locked = new AtomicBoolean(false);
 *   private final Queue<Thread> waiters
 *     = new ConcurrentLinkedQueue<Thread>();
 *
 *   public void lock() {
 *     boolean wasInterrupted = false;
 *     Thread current = Thread.currentThread();
 *     waiters.add(current);
 *
 *     // Block while not first in queue or cannot acquire lock
 *     while (waiters.peek() != current ||
 *            !locked.compareAndSet(false, true)) {
 *       LockSupport.park(this);
 *       if (Thread.interrupted()) // ignore interrupts while waiting
 *         wasInterrupted = true;
 *     }
 *
 *     waiters.remove();
 *     if (wasInterrupted)          // reassert interrupt status on exit
 *       current.interrupt();
 *   }
 *
 *   public void unlock() {
 *     locked.set(false);
 *     LockSupport.unpark(waiters.peek());
 *   }
 * }}</pre>
 *
 * @see #park()
 * @see #unpark(Thread)
 * @see #parkNanos(long)
 * @see #park(Object)
 * @see #parkUntil(long)
 */
public class LockSupport {
    private LockSupport() {} // Cannot be instantiated.

    private static void setBlocker(Thread t, Object arg) {
        // Even though volatile, hotspot doesn't need a write barrier here.
        // 将arg
        UNSAFE.putObject(t, parkBlockerOffset, arg);
    }

    /**
     * <p>
     * 当一个线程调用unpark时，如果参数thread线程没有持有thread与LockSupport类关联的许可证，则让thread线程持有。
     * 如果thread之前因调用park()而被挂起，则调用unpark后，该线程会被唤醒。
     * 如果thread之前没有调用park,则调用unpark方法后，再调用park方法，其会立刻返回。
     * </p><br>
     *
     * Makes available the permit for the given thread, if it
     * was not already available.  If the thread was blocked on
     * {@code park} then it will unblock.  Otherwise, its next call
     * to {@code park} is guaranteed not to block. This operation
     * is not guaranteed to have any effect at all if the given
     * thread has not been started.
     *
     * @param thread the thread to unpark, or {@code null}, in which case
     *        this operation has no effect
     */
    public static void unpark(Thread thread) {
        if (thread != null)
            UNSAFE.unpark(thread);
    }

    /**
     * 建议使用
     *
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call returns
     * immediately; otherwise
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @since 1.6
     */
    public static void park(Object blocker) {
        Thread t = Thread.currentThread(); // 获取调用线程
        setBlocker(t, blocker); // 获取该线程的 blocker变量
        UNSAFE.park(false, 0L); // 挂起线程
        setBlocker(t, null); // 线程被激活后清除blocker变量，因为一般都是在线程阻塞时才分析原因
    }

    /**
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param nanos the maximum number of nanoseconds to wait
     * @since 1.6
     */
    public static void parkNanos(Object blocker, long nanos) {
        if (nanos > 0) {
            Thread t = Thread.currentThread();
            setBlocker(t, blocker);
            UNSAFE.park(false, nanos);
            setBlocker(t, null);
        }
    }

    /**
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param blocker the synchronization object responsible for this
     *        thread parking
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until
     * @since 1.6
     */
    public static void parkUntil(Object blocker, long deadline) {
        Thread t = Thread.currentThread();
        setBlocker(t, blocker);
        UNSAFE.park(true, deadline);
        setBlocker(t, null);
    }

    /**
     * Returns the blocker object supplied to the most recent
     * invocation of a park method that has not yet unblocked, or null
     * if not blocked.  The value returned is just a momentary
     * snapshot -- the thread may have since unblocked or blocked on a
     * different blocker object.
     *
     * @param t the thread
     * @return the blocker
     * @throws NullPointerException if argument is null
     * @since 1.6
     */
    public static Object getBlocker(Thread t) {
        if (t == null)
            throw new NullPointerException();
        return UNSAFE.getObjectVolatile(t, parkBlockerOffset);
    }

    /**
     * <p>
     *     如果调用park方法的线程己经拿到了与LockSupport关联的许可证，则调用LockSupport.park()时会马上返回，
     *     否则调用线程会被禁止参与线程的调度，也就是会被阻寨挂起，直到发生下面3种情况中的某一个: （所以在调用park()方法时最好也使用循环判断的方式） <br>
     *     - 其他线程以当前线程为目标调用{@link #unpark};             <br>
     *     - 其他线程{@linkplain Thread#interrupt 中断}当前线程;     <br>
     *     - 虚拟唤醒
     *     <br>
     *
     *     需要注意的是，因调用park()方法而被阻塞的线程被其他线程中断而返回时并不会抛出InterruptedException异常。
     * </p><br>
     *
     *
     * Disables the current thread for thread scheduling purposes unless the
     * permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of three
     * things happens:
     *
     * <ul>
     *
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread upon return.
     */
    public static void park() {
        UNSAFE.park(false, 0L);
    }

    /**
     * <p>
     *     如果调用park方法的线程己经拿到了与LockSupport关联的许可证，则调用LockSupport.park()时会马上返回，
     *     否则调用线程会被禁止参与线程的调度，也就是会被阻寨挂起，直到发生下面4种情况中的某一个: （所以在调用park()方法时最好也使用循环判断的方式） <br>
     *     - 其他线程以当前线程为目标调用{@link #unpark};             <br>
     *     - 其他线程{@linkplain Thread#interrupt 中断}当前线程;     <br>
     *     - 虚拟唤醒                                              <br>
     *     - 调用线程被挂起nocos时间后修改为自动返回
     *     <br>
     *
     *     需要注意的是，因调用park()方法而被阻塞的线程被其他线程中断而返回时并不会抛出InterruptedException异常。
     * </p><br>
     *
     * Disables the current thread for thread scheduling purposes, for up to
     * the specified waiting time, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified waiting time elapses; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the elapsed time
     * upon return.
     *
     * @param nanos the maximum number of nanoseconds to wait
     */
    public static void parkNanos(long nanos) {
        if (nanos > 0)
            UNSAFE.park(false, nanos);
    }

    /**
     * <p>
     *      这个方法和{@link #parkNanos(long)}的区别是，后者是从当前算等待nanos秒时间，而前者是指定一个时间点，
     *      比如需要等到2017.12.11 11:00，则把议个时间点转换为从1970年到议个时问点的总毫秒数。
     * </p><br>
     *
     * Disables the current thread for thread scheduling purposes, until
     * the specified deadline, unless the permit is available.
     *
     * <p>If the permit is available then it is consumed and the call
     * returns immediately; otherwise the current thread becomes disabled
     * for thread scheduling purposes and lies dormant until one of four
     * things happens:
     *
     * <ul>
     * <li>Some other thread invokes {@link #unpark unpark} with the
     * current thread as the target; or
     *
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     *
     * <li>The specified deadline passes; or
     *
     * <li>The call spuriously (that is, for no reason) returns.
     * </ul>
     *
     * <p>This method does <em>not</em> report which of these caused the
     * method to return. Callers should re-check the conditions which caused
     * the thread to park in the first place. Callers may also determine,
     * for example, the interrupt status of the thread, or the current time
     * upon return.
     *
     * @param deadline the absolute time, in milliseconds from the Epoch,
     *        to wait until <br> 时间单位为ms，该时间是从1970年到现在某一个时间点的毫秒值。
     */
    public static void parkUntil(long deadline) {
        UNSAFE.park(true, deadline);
    }

    /**
     * Returns the pseudo-randomly initialized or updated secondary seed.
     * Copied from ThreadLocalRandom due to package access restrictions.
     */
    static final int nextSecondarySeed() {
        int r;
        Thread t = Thread.currentThread();
        if ((r = UNSAFE.getInt(t, SECONDARY)) != 0) {
            r ^= r << 13;   // xorshift
            r ^= r >>> 17;
            r ^= r << 5;
        }
        else if ((r = java.util.concurrent.ThreadLocalRandom.current().nextInt()) == 0)
            r = 1; // avoid zero
        UNSAFE.putInt(t, SECONDARY, r);
        return r;
    }

    // Hotspot implementation via intrinsics API
    private static final sun.misc.Unsafe UNSAFE;
    private static final long parkBlockerOffset;
    private static final long SEED;
    private static final long PROBE;
    private static final long SECONDARY;
    static {
        try {
            UNSAFE = sun.misc.Unsafe.getUnsafe();
            Class<?> tk = Thread.class;
            parkBlockerOffset = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("parkBlocker"));
            SEED = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSeed"));
            PROBE = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomProbe"));
            SECONDARY = UNSAFE.objectFieldOffset
                    (tk.getDeclaredField("threadLocalRandomSecondarySeed"));
        } catch (Exception ex) { throw new Error(ex); }
    }


    public class TestLockSupport {
        @Test
        public void test1(){
            System.out.println("begin park");
            LockSupport.park(); // 只会输出 “begin park” 就会阻塞
            System.out.println("end park");
        }

        @Test
        public void test2(){
            System.out.println("begin park");
            // 使当前线程获取先获取通行证
            LockSupport.unpark(Thread.currentThread());
            // 再吃锁住
            LockSupport.park();
            System.out.println("end park");
            // 输出 "begin park"  "end park"
        }

        // 为了说明调用park方法后的线程被中断后会返回
        @Test
        public void test3() throws InterruptedException {
            Thread thread = new Thread(()->{
                System.out.println("child thread begin park!");
                // 调用park方法，挂起自己，只有自己被中断才会退出循环
                while (!Thread.currentThread().isInterrupted()){
                    LockSupport.park();
                }
                System.out.println("child thread unpark!");
            });

            // 启动子线程
            thread.start();
            // 主线程休眠1s
            Thread.sleep(1_000);
            System.out.println("main thread begin park!");
            // 中断子线程
            thread.interrupt();
        /*
            输出
            child thread begin park！
            main thread begin unpark！
            child thread unpark!
         */
        }
    }

    /**
     * 先入先出的锁
     */
    public class FIFOMutex {

        private final AtomicBoolean locked = new AtomicBoolean(false);
        private final Queue<Thread> waiters = new ConcurrentLinkedQueue<>();

        public void lock() {
            boolean wasInterrupted = false;
            Thread current = Thread.currentThread();
            waiters.add(current);

            // 只有队首的线程可以获取锁
            while (waiters.peek() != current || !locked.compareAndSet(false, true)) {
                // 如果当前线程不是队首或者当前锁己经被其他线程获取，则调用 park 方法挂起自己
                LockSupport.park(this);

                if (Thread.interrupted())
                    wasInterrupted = true; //如果park方法是因为被中断而返回，则忽略中断，并且重置中断标志，做个标记
            }
            waiters.remove();
            if (wasInterrupted)
                current.interrupt(); // 判断标记，如果标记为true,则中断线程. 恢复中断
        }

        public void unlock() {
            locked.set(false);
            LockSupport.unpark(waiters.peek());
        }

    }
}
