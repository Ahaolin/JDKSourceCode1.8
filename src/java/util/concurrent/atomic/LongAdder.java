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

package java.util.concurrent.atomic;
import java.io.Serializable;
import java.util.concurrent.atomic.Striped64;

/**
 * <p>
 *     前面讲过，{@link AtomicLong}通过CAS提供了非阻塞的原子性操作，相比使用阻塞算法的同步器来说它的性能己经很好了，但是JDK开发组并不满足于此。
 * 使用AtomicLong时，在高并发下大量线程会同时去竞争更新同一个原子变量，但是由于同时只有一个线程的CAS操作会成功，这就造成了大量线程竞争失败后，
 * 会通过无限循环不断进行自旋尝试CAS的操作，而这会自白浪费CPU资源。 <br>
 *     因此JDK8新增了一个原子性递增或者递减类LongAdder用来克服在高并发下使用AtomicLong的缺点。
 * 既然AtomicLong的性能瓶颈是由于过多线程同时去竞争一个变量的更新而产生的，那么如果把一个变量分解为多个变量，让同样多的线程去竞争多个资源，是不是就解决了性能问题？
 * </p><br>
 *
 *  <p>
 *      使用LongAdder时，则是在内部维护多个{@link #cells}变量，每个Cell里面有一个初始值为0的long型变量，这样，在同等并发量的情况下，
 *  争夺单个变量更新操作的线程量会减少，这变相地减少了争夺共享资的并发量。另外，多个线程在争夺同一个Cell原子变量时如果失败了，
 *  它并不是在当前Cell变量上一直自旋CAS重试，而是尝试在其他Cell的变量上进行CAS尝试，这个改变增加了当前线程重试CAS成功的可能性。
 *  最后，在获取LongAdder当前值时，是把所有Cell变量的{@link Cell#value}值累加后再加上{@link Cell#base}返回的。
 *  </p><br>
 *
 *  <p>
 *      LongAdder维护了一个延迟初始化的原子性更新数组（默认情况下Cell数组是null）和一个基值变量base。
 *  由于{@link #cells}占用的内存是相对比较大的，所以一开始并不创建它，而是在需要时创建，也就是惰性加载。
 *  </p><br>
 *
 *  <p>
 *      当一开始判断CeII数组是null并且并发线程较少时，所有的累加操作都是对base变量进行的。
 *  保持Cell数组的大小为2的N次方，在初始化时Cell数组中的Cell元素个数为2，数组里面的变量实体是Cell类型。
 *  Cell类型是AtomicLong的一个改进，用来减少缓存的争用，也就是解决伪共享问题。
 *  </p><br>
 *
 *  <p>
 *      对于大多数孤立的多个原子操作进行字节填充是浪费的，因为原子性操作都是无规律地分散在内存中的（也就是说多个原子性变量的内存地址是不连续的），
 * 多个原子变量被放入同一个缓存行的可能性很小。但是原子性数组元素的内存地址是连续的，所以数组内的多个元素能经常共享缓存行，
 * 因此这里使用{@link sun.misc.Contended}注解对{@link Cell}进行字节填充，这防止了数组中多个元素共享一个缓存行，在性能上是一个提升。
 *  </p><br>
 *
 *  <br>
 * One or more variables that together maintain an initially zero
 * {@code long} sum.  When updates (method {@link #add}) are contended
 * across threads, the set of variables may grow dynamically to reduce
 * contention. Method {@link #sum} (or, equivalently, {@link
 * #longValue}) returns the current total combined across the
 * variables maintaining the sum.
 *
 * <p>This class is usually preferable to {@link AtomicLong} when
 * multiple threads update a common sum that is used for purposes such
 * as collecting statistics, not for fine-grained synchronization
 * control.  Under low update contention, the two classes have similar
 * characteristics. But under high contention, expected throughput of
 * this class is significantly higher, at the expense of higher space
 * consumption.
 *
 * <p>LongAdders can be used with a {@link
 * java.util.concurrent.ConcurrentHashMap} to maintain a scalable
 * frequency map (a form of histogram or multiset). For example, to
 * add a count to a {@code ConcurrentHashMap<String,LongAdder> freqs},
 * initializing if not already present, you can use {@code
 * freqs.computeIfAbsent(k -> new LongAdder()).increment();}
 *
 * <p>This class extends {@link Number}, but does <em>not</em> define
 * methods such as {@code equals}, {@code hashCode} and {@code
 * compareTo} because instances are expected to be mutated, and so are
 * not useful as collection keys.
 *
 * @since 1.8
 * @author Doug Lea
 *
 * <pre>
 *     (1) LongAdder的结构是怎样的?                            {@link #sum()} <br>
 *     (2) 当前线程应该访问{@link #cells}数组中的哪一个元素?       {@link #add(long)} <br>
 *     (3) 如何初始化Cell数组 {@link #cells}?                  {@link Striped64#longAccumulate} P14<br>
 *     (4) {@link #cells}数组如何扩容?                         {@link Striped64#longAccumulate} <br>
 *     (5) 线程访问分配的{@link}元素有冲突后如何处理?              {@link Striped64#longAccumulate} <br>
 *     (6) 如何保证线程操作被分配的的Cell元素的原子性?              {@link Striped64.Cell}、{@link #add(long)} <br>
 * </pre>
 *
 * @see #sum()
 * @see #reset()
 * @see #sumThenReset()
 * @see #add(long)
 *
 */
public class LongAdder extends Striped64 implements Serializable {
    private static final long serialVersionUID = 7249069246863182397L;

    /**
     * Creates a new adder with initial sum of zero.
     */
    public LongAdder() {
    }

    /**
     * Adds the given value.
     *
     * @param x the value to add
     */
    public void add(long x) {
        Cell[] as; long b, v; int m; Cell a;
        if ((as = cells) != null || !casBase(b = base, b + x)) { // (1) 首先看cells是否为null，如果为null则当前在基础变量base上进行累加，这时候就类似AtomicLong的操作。
            // 如果cells不为null or 线程执行代码(1)的CAS操作失败了 才会往下执行

            /**
             * 代码(2)(3)决定当前线程应该访问cells数组里面的哪一个Cell元素，如果当前线程映射的元素存在则执行代码(4)，使用CAS操作去更新分配的Cell元素的value值，
             * 如果当前线程映射的元素不存在或者存在但是CAS操作失败则执行代码(5)。其实将代码(2)(3)(4)合起来看就是获取当前线程应该访问的cells数组的Cell元素，
             * 然后进行CAS更新操作，只是在获取期间如果有些条件不满足则会跳转到代码(5)执行。
             * 另外当前线程应该访问cells数组的哪一个Cell元素是通过getProbe()&m进行计算的，其中m是当前cells数组元素个数-1，
             * getProbe()则用于获取当前线程中变量threadLocalRandomProbe的值，这个值一开始为0，在代码(5)里面会对其进行初始化。
             * 并且当前线程通过分配的Cell元素的cas函数来保证对Cell元素value值更新的原子性，到这里我们回答了问题2和问题6。
             */
            boolean uncontended = true;
            if (as == null || (m = as.length - 1) < 0 ||  // (2)
                    (a = as[getProbe() & m]) == null ||   // (3)
                    !(uncontended = a.cas(v = a.value, v + x))) // (4)
                longAccumulate(x, null, uncontended); // (5)
        }
    }

    /**
     * Equivalent to {@code add(1)}.
     */
    public void increment() {
        add(1L);
    }

    /**
     * Equivalent to {@code add(-1)}.
     */
    public void decrement() {
        add(-1L);
    }

    /**
     * <p>
     *  LongAdder类继承自{@link Striped64}类，在Striped64内部维护着三个变量。 <br>
     *  LongAdder的真实值 = {@link Striped64#base} + ( {@link #cells}数组里面Cell元素中的{@link Cell#value} )
     *  base是个基础值，默认为0。
     *  cellsBusy用来实现自旋锁，状态值只有0和1，当创建Cell元素，扩容cell数组或者初始化cell数组时，使用CAS操作该变量来保证同时只有一个线程可以进行其中之一的操作。
     * </p><br>
     *
     * <p>
     *  由于计算总和时没有对CeII数组进行加锁，所以在累加过程中可能有其他线程对Cell中的值进行了修改，也有可能对数组进行了扩容，
     *  所以sum返回的值并不是罪精确的，其返回值并不是一个调用sum方法时的原子快照值。
     * </p><br>
     *
     *
     * Returns the current sum.  The returned value is <em>NOT</em> an
     * atomic snapshot; invocation in the absence of concurrent
     * updates returns an accurate result, but concurrent updates that
     * occur while the sum is being calculated might not be
     * incorporated.
     *
     * @return the sum
     */
    public long sum() {
        Cell[] as = cells; Cell a;
        long sum = base;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    sum += a.value;
            }
        }
        return sum;
    }

    /**
     * 重置操作，如下代码把base置为0，如果Cell数组有元素，则元素值被重置为0。<br>
     *
     * Resets variables maintaining the sum to zero.  This method may
     * be a useful alternative to creating a new adder, but is only
     * effective if there are no concurrent updates.  Because this
     * method is intrinsically racy, it should only be used when it is
     * known that no threads are concurrently updating.
     */
    public void reset() {
        Cell[] as = cells; Cell a;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null)
                    a.value = 0L;
            }
        }
    }

    /**
     * <p>
     *    {@link #sum()}的改造版本，如下代码在使用sum累加对应的Cell值后，把当前Cell的值重置为0，base重置为0。
     *    这样，当多线程调用该方法时会有问题，比如考虑第一个调用线程清空Cell的值，则后一个线程调用时累加的都是0值。
     * </p><br>
     *
     * Equivalent in effect to {@link #sum} followed by {@link
     * #reset}. This method may apply for example during quiescent
     * points between multithreaded computations.  If there are
     * updates concurrent with this method, the returned value is
     * <em>not</em> guaranteed to be the final value occurring before
     * the reset.
     *
     * @return the sum
     */
    public long sumThenReset() {
        Cell[] as = cells; Cell a;
        long sum = base;
        base = 0L;
        if (as != null) {
            for (int i = 0; i < as.length; ++i) {
                if ((a = as[i]) != null) {
                    sum += a.value;
                    a.value = 0L;
                }
            }
        }
        return sum;
    }

    /**
     * Returns the String representation of the {@link #sum}.
     * @return the String representation of the {@link #sum}
     */
    public String toString() {
        return Long.toString(sum());
    }

    /**
     * Equivalent to {@link #sum}.
     *
     * @return the sum
     */
    public long longValue() {
        return sum();
    }

    /**
     * Returns the {@link #sum} as an {@code int} after a narrowing
     * primitive conversion.
     */
    public int intValue() {
        return (int)sum();
    }

    /**
     * Returns the {@link #sum} as a {@code float}
     * after a widening primitive conversion.
     */
    public float floatValue() {
        return (float)sum();
    }

    /**
     * Returns the {@link #sum} as a {@code double} after a widening
     * primitive conversion.
     */
    public double doubleValue() {
        return (double)sum();
    }

    /**
     * Serialization proxy, used to avoid reference to the non-public
     * Striped64 superclass in serialized forms.
     * @serial include
     */
    private static class SerializationProxy implements Serializable {
        private static final long serialVersionUID = 7249069246863182397L;

        /**
         * The current value returned by sum().
         * @serial
         */
        private final long value;

        SerializationProxy(LongAdder a) {
            value = a.sum();
        }

        /**
         * Return a {@code LongAdder} object with initial state
         * held by this proxy.
         *
         * @return a {@code LongAdder} object with initial state
         * held by this proxy.
         */
        private Object readResolve() {
            LongAdder a = new LongAdder();
            a.base = value;
            return a;
        }
    }

    /**
     * Returns a
     * <a href="../../../../serialized-form.html#java.util.concurrent.atomic.LongAdder.SerializationProxy">
     * SerializationProxy</a>
     * representing the state of this instance.
     *
     * @return a {@link SerializationProxy}
     * representing the state of this instance
     */
    private Object writeReplace() {
        return new SerializationProxy(this);
    }

    /**
     * @param s the stream
     * @throws java.io.InvalidObjectException always
     */
    private void readObject(java.io.ObjectInputStream s)
            throws java.io.InvalidObjectException {
        throw new java.io.InvalidObjectException("Proxy required");
    }

}
