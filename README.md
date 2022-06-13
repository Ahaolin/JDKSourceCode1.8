# JDK 1.8 源码解析

## 1. 基础

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/String.java">String 源码</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/sun/misc/Unsafe.java">💛Unsafe 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/Random.java">💛Random 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/ThreadLocalRandom.java">💛💛ThreadLocalRandom 源码解析</a>

## 2. 集合

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/ArrayList.java">ArrayList 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/LinkedList.java">LinkedList 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/HashMap.java">HashMap 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/Hashtable.java">Hashtable 源码解析</a>


## 3. 原子操作类
> 原子操作类的原理大致都相同，只针对AtomicLong、LongAdder、LongAccumulator

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/atomic/AtomicLong.java">💛💛AtomicLong 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/atomic/LongAdder.java">💛💛LongAdder 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/atomic/LongAccumulator.java">💛💛LongAccumulator 源码解析</a>


## 4. 并发集合类
> - 并发包中的并发List只有`CopyOnWriteArrayList`是一个线程安全的ArrayList，对其进行的修改操作都是在底层的一个复制的数组（快照）上进行的， 也就是使用了写时复制策略。
>   - `CopyOnWriteArraySet`底层就是 使用`CopyOnWriteArrayList`

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/ConcurrentHashMap.java">ConcurrentHashMap 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/CopyOnWriteArrayList.java">💛💛CopyOnWriteArrayList 源码解析</a>

## 5. 锁
<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/locks/LockSupport.java">💛💛LockSupport 源码解析</a>


## 线程

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/Thread.java">Thread 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/Runnable.java">Runnable 源码解析</a>

<a href="https://gitee.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/ThreadPoolExecutor.java">💛💛💛ThreadPoolExecutor 源码解析</a>

<a href="https://gitee.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/ScheduledThreadPoolExecutor.java">💛💛💛ScheduledThreadPoolExecutor 源码解析</a>

## 并发工具类

<a href="https://gitee.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/CountDownLatch.java">💛💛💛CountDownLatch 源码解析</a>

<a href="https://gitee.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/CyclicBarrier.java">💛💛💛CyclicBarrier 源码解析</a>

<a href="https://gitee.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/util/concurrent/Semaphore.java">💛💛💛Semaphore 源码解析</a>


## 其他

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/Object.java">Object 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/Runtime.java">Runtime 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/ThreadLocal.java">ThreadLocal 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/InheritableThreadLocal.java">InheritableThreadLocal 源码解析</a>

<a href="https://github.com/Ahaolin/JDKSourceCode1.8/blob/master/src/java/lang/ref/WeakReference.java">WeakReference 源码解析</a>
