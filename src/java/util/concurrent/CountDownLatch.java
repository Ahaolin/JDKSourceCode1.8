/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package java.util.concurrent;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * <pre>
 *     这里总结下CountDownLatch与join方法的区别。
 *     1. 调用一个子线程的join()方法后，该线程会一直被阻塞直到子线程运行完毕，而CountDownLatch则使用计数器来允许子线程运行完毕或者在运行中递减计数，
 *     也就是CountDownLatch可以在子线程运行的任何时候让await方法返回而不一定必须等到线程结束。
 *     2. 另外，使用线程池来管理线程时一般都是直接添加Runnable到线程池，这时候就没有办法再调用线程的join方法了，就是说countDownLatch相比join方法让我们对线程同步有更灵活的控制。
 * </pre>
 *
 * <pre>
 * CountDownLatch内部应该有一个计数器(递减)。
 *   1.使用AQS实现，通过{@link #CountDownLatch(int)} 计数器的值就是AQS的state变量。
 *   2.当线程调用{@link #await()}后当前线程会被放入AQS的阻塞队列等待计数器为0再返回。
 *   3.当多个线程调用{@link #countDown()}实际上是原子性递减AQS的状态值。当计数器变为0时,
 *   当前线程还要调用{@link AbstractQueuedSynchronizer#doReleaseShared()}来激活那些{@link #await()}方法
 *   而被阻塞的线程。
 * </pre>
 *
 *
 * A synchronization aid that allows one or more threads to wait until
 * a set of operations being performed in other threads completes.
 *
 * <p>A {@code CountDownLatch} is initialized with a given <em>count</em>.
 * The {@link #await await} methods block until the current count reaches
 * zero due to invocations of the {@link #countDown} method, after which
 * all waiting threads are released and any subsequent invocations of
 * {@link #await await} return immediately.  This is a one-shot phenomenon
 * -- the count cannot be reset.  If you need a version that resets the
 * count, consider using a {@link CyclicBarrier}.
 *
 * <p>A {@code CountDownLatch} is a versatile synchronization tool
 * and can be used for a number of purposes.  A
 * {@code CountDownLatch} initialized with a count of one serves as a
 * simple on/off latch, or gate: all threads invoking {@link #await await}
 * wait at the gate until it is opened by a thread invoking {@link
 * #countDown}.  A {@code CountDownLatch} initialized to <em>N</em>
 * can be used to make one thread wait until <em>N</em> threads have
 * completed some action, or some action has been completed N times.
 *
 * <p>A useful property of a {@code CountDownLatch} is that it
 * doesn't require that threads calling {@code countDown} wait for
 * the count to reach zero before proceeding, it simply prevents any
 * thread from proceeding past an {@link #await await} until all
 * threads could pass.
 *
 * <p><b>Sample usage:</b> Here is a pair of classes in which a group
 * of worker threads use two countdown latches:
 * <ul>
 * <li>The first is a start signal that prevents any worker from proceeding
 * until the driver is ready for them to proceed;
 * <li>The second is a completion signal that allows the driver to wait
 * until all workers have completed.
 * </ul>
 *
 *  <pre> {@code
 * class Driver { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch startSignal = new CountDownLatch(1);
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       new Thread(new Worker(startSignal, doneSignal)).start();
 *
 *     doSomethingElse();            // don't let run yet
 *     startSignal.countDown();      // let all threads proceed
 *     doSomethingElse();
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class Worker implements Runnable {
 *   private final CountDownLatch startSignal;
 *   private final CountDownLatch doneSignal;
 *   Worker(CountDownLatch startSignal, CountDownLatch doneSignal) {
 *     this.startSignal = startSignal;
 *     this.doneSignal = doneSignal;
 *   }
 *   public void run() {
 *     try {
 *       startSignal.await();
 *       doWork();
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>Another typical usage would be to divide a problem into N parts,
 * describe each part with a Runnable that executes that portion and
 * counts down on the latch, and queue all the Runnables to an
 * Executor.  When all sub-parts are complete, the coordinating thread
 * will be able to pass through await. (When threads must repeatedly
 * count down in this way, instead use a {@link CyclicBarrier}.)
 *
 *  <pre> {@code
 * class Driver2 { // ...
 *   void main() throws InterruptedException {
 *     CountDownLatch doneSignal = new CountDownLatch(N);
 *     Executor e = ...
 *
 *     for (int i = 0; i < N; ++i) // create and start threads
 *       e.execute(new WorkerRunnable(doneSignal, i));
 *
 *     doneSignal.await();           // wait for all to finish
 *   }
 * }
 *
 * class WorkerRunnable implements Runnable {
 *   private final CountDownLatch doneSignal;
 *   private final int i;
 *   WorkerRunnable(CountDownLatch doneSignal, int i) {
 *     this.doneSignal = doneSignal;
 *     this.i = i;
 *   }
 *   public void run() {
 *     try {
 *       doWork(i);
 *       doneSignal.countDown();
 *     } catch (InterruptedException ex) {} // return;
 *   }
 *
 *   void doWork() { ... }
 * }}</pre>
 *
 * <p>Memory consistency effects: Until the count reaches
 * zero, actions in a thread prior to calling
 * {@code countDown()}
 * <a href="package-summary.html#MemoryVisibility"><i>happen-before</i></a>
 * actions following a successful return from a corresponding
 * {@code await()} in another thread.
 *
 * @since 1.5
 * @author Doug Lea
 *
 * @see #await()
 * @see #await(long, TimeUnit)
 * @see #countDown()
 * @see #getCount()
 */
public class CountDownLatch {
    /**
     * Synchronization control For CountDownLatch.
     * Uses AQS state to represent count.
     */
    private static final class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 4982264981922014374L;

        Sync(int count) {
            setState(count);
        }

        int getCount() {
            return getState();
        }

        // sync类实现的AQS接口
        protected int tryAcquireShared(int acquires) {
            return (getState() == 0) ? 1 : -1;
        }

        // sync类实现的AQS接口
        protected boolean tryReleaseShared(int releases) {
            // Decrement count; signal when transition to zero
            // 循环进行CAS,直到当前线程成功完成CAS使计数器值(状态值state)减1 并更新到state
            for (;;) {
                int c = getState();
                if (c == 0) // (1) 防止多次调用countDown()使得state变为负数
                    return false;
                int nextc = c-1;
                if (compareAndSetState(c, nextc)) // (2) 使用cas让计数器减1
                    // 等于0，AbstractQueuedSynchronizer#doReleaseShared才会唤醒await()方法阻塞的线程
                    return nextc == 0;
            }
        }
    }

    private final Sync sync;

    /**
     * Constructs a {@code CountDownLatch} initialized with the given count.
     *
     * @param count the number of times {@link #countDown} must be invoked
     *        before threads can pass through {@link #await}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public CountDownLatch(int count) {
        if (count < 0) throw new IllegalArgumentException("count < 0");
        this.sync = new Sync(count);
    }

    /**
     * <pre>
     *  当线程调用对象的await方法后，当前线程会被阻塞，直到下面的情况之一发生才会返回：
     *     1.当所有线程都调用了CountDownLatch对象的countDown方法后，也就是计数器的值为0时；
     *     2.其他线程调用了当前线程的interrupt（）方法中断了当前线程，当前线程就会抛出InterruptedException异常，然后返回。
     * </pre>
     *
     * <p>
     * 该方法的特点是线程获取资源时可以被中断，并且获取的资源是共享资源。
     * {@link AbstractQueuedSynchronizer#acquireSharedInterruptibly(int)}首先判断当前线程是否己被中断，
     * 若是则抛出异常，否则调用sync实现的{@link Sync#tryAcquireShared(int)}查看当前状态值（计数器值）是否为0，
     * 是则当前线程的await()方法直接返回，否则调用 {@link AbstractQueuedSynchronizer#doAcquireInterruptibly(int)}
     * 让当前线程阻塞。
     * 另外可以看到，这里{@link AbstractQueuedSynchronizer#tryAcquireShared(int)}传递的arg参数没有被用到，调用该方法仅仅是为了检查当前状态值是不是为0，并没有调用CAS让当前状态值减1。
     * </p>
     *<br>
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the current count is zero then this method returns immediately.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of two things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void await() throws InterruptedException {
        /*
              查看当前当前计数器值是否为0，为0直接返回，否则进入AQS的等待队列
              if (tryAcquireShared(arg) < 0)
                doAcquireSharedInterruptibly(arg); // 进入AQS的等待队列
         */
        sync.acquireSharedInterruptibly(1);
    }

    /**
     * <pre>
     *  当线程调用了CountDownLatch对象的该方法后，当前线程会被阻塞，直到下面的情况之一发生才会返回：
     *     1.当所有线程都调用了CountDownLatch对象的countDown方法后，也就是计数器的值为0时,这时候会返回true;
     *     2.设置的timeout时间到了，因为超时而返回false;
     *     3.其他线程调用了当前线程的interrupt()方法中断了当前线程，当前线程就会抛出InterruptedException异常，然后返回。
     * </pre>
     *
     * Causes the current thread to wait until the latch has counted down to
     * zero, unless the thread is {@linkplain Thread#interrupt interrupted},
     * or the specified waiting time elapses.
     *
     * <p>If the current count is zero then this method returns immediately
     * with the value {@code true}.
     *
     * <p>If the current count is greater than zero then the current
     * thread becomes disabled for thread scheduling purposes and lies
     * dormant until one of three things happen:
     * <ul>
     * <li>The count reaches zero due to invocations of the
     * {@link #countDown} method; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts}
     * the current thread; or
     * <li>The specified waiting time elapses.
     * </ul>
     *
     * <p>If the count reaches zero then the method returns with the
     * value {@code true}.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while waiting,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.  If the time is less than or equal to zero, the method
     * will not wait at all.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return {@code true} if the count reached zero and {@code false}
     *         if the waiting time elapsed before the count reached zero
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean await(long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(1, unit.toNanos(timeout));
    }

    /**
     * <pre>
     *   线程调用该方法后，计数器的值递减，递减后如果计数器值为0则唤醒所有因调用{@link #await()}而被阻塞的线程，否则什么都不做。
     * </pre>
     *
     * Decrements the count of the latch, releasing all waiting threads if
     * the count reaches zero.
     *
     * <p>If the current count is greater than zero then it is decremented.
     * If the new count is zero then all waiting threads are re-enabled for
     * thread scheduling purposes.
     *
     * <p>If the current count equals zero then nothing happens.
     */
    public void countDown() {
        // 委托Sync调用AQS的方法
        sync.releaseShared(1);
    }

    /**
     * <pre>
     *     获取当前计数器的值,也就是 AQS state值,一般在测试时使用该方法
     * </pre>
     *
     * Returns the current count.
     *
     * <p>This method is typically used for debugging and testing purposes.
     *
     * @return the current count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * Returns a string identifying this latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + sync.getCount() + "]";
    }
}
