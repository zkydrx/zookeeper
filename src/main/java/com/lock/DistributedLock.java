package com.lock;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-08-31
 * Time: 09:36:54
 * Description:
 */
public class DistributedLock implements Lock,Watcher
{

    private ZooKeeper zk = null;
    // 根节点
    private String ROOT_LOCK = "/locks";
    // 竞争的资源
    private String lockName;
    // 等待的前一个锁
    private String WAIT_LOCK;
    // 当前锁
    private String CURRENT_LOCK;
    // 计数器
    private CountDownLatch countDownLatch;
    private int sessionTimeout = 30000;
    private List<Exception> exceptionList = new ArrayList<Exception>();


    /**
     * 配置分布式锁
     * @param config 连接的url
     * @param lockName 竞争资源
     */
    public DistributedLock(String config, String lockName) {
        this.lockName = lockName;
        try {
            // 连接zookeeper
            zk = new ZooKeeper(config, sessionTimeout, this);
            Stat stat = zk.exists(ROOT_LOCK, false);
            if (stat == null) {
                // 如果根节点不存在，则创建根节点
                zk.create(ROOT_LOCK, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (KeeperException e) {
            e.printStackTrace();
        }
    }

    /**
     * 节点监视器
     * @param event
     */
    @Override
    public void process(WatchedEvent event)
    {
        if(this.countDownLatch != null)
        {
            this.countDownLatch.countDown();
        }
    }

    /**
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     */
    @Override
    public void lock()
    {
        if(exceptionList.size()>0)
        {
            throw new LockException(exceptionList.get(0));
        }
        try
        {
            if(this.tryLock())
            {
                System.out.println(Thread.currentThread().getName()+":"+lockName+"获取了锁");
                return;
            }
            else
            {
                waitForLock(WAIT_LOCK,sessionTimeout);
            }
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (KeeperException e)
        {
            e.printStackTrace();
        }

    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is available and returns immediately.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     *
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     *
     * @throws InterruptedException if the current thread is
     *                              interrupted while acquiring the lock (and interruption
     *                              of lock acquisition is supported)
     */
    @Override
    public void lockInterruptibly() throws InterruptedException
    {
        this.lock();
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>A typical usage idiom for this method would be:
     * <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     * <p>
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     * {@code false} otherwise
     */
    @Override
    public boolean tryLock()
    {
        try
        {
            String splitStr = "_lock_";
            if(lockName.contains(splitStr))
            {
                throw new LockException(" 锁名有误");
            }
            /**
             * 创建临时有序节点
             */
            CURRENT_LOCK = zk.create(ROOT_LOCK+"/"+lockName+splitStr,new byte[0],ZooDefs.Ids.OPEN_ACL_UNSAFE,CreateMode
                    .EPHEMERAL_SEQUENTIAL);
            System.out.println(CURRENT_LOCK+" 已经创建");
            List<String> subNodes = zk.getChildren(ROOT_LOCK, false);
            /**
             * 取出所有lockName锁
             */
            ArrayList<String> lockObjects = new ArrayList<>();
            for (String subNode : subNodes)
            {
                String node = subNode.split(splitStr)[0];
                if(node.equals(lockName))
                {
                    lockObjects.add(subNode);
                }
            }
            Collections.sort(lockObjects);
            System.out.println(Thread.currentThread().getName()+" 的锁是 "+CURRENT_LOCK);
            /**
             * 若当前节点为最小节点，则获取锁成功
             */
            if(CURRENT_LOCK.equals(ROOT_LOCK+"/"+lockObjects.get(0)))
            {
                return true;
            }

            /**
             *  说不是最小节点，则找到自己的前一个节点
             */
            String prevNode = CURRENT_LOCK.substring(CURRENT_LOCK.lastIndexOf("/") + 1);
            WAIT_LOCK = lockObjects.get(Collections.binarySearch(lockObjects,prevNode)-1);
        }
        catch (KeeperException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     * if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted
     *                              while acquiring the lock (and interruption of lock
     *                              acquisition is supported)
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException
    {
        try
        {
            if(this.tryLock())
            {
                return true;
            }
            return waitForLock(WAIT_LOCK,time);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 等待锁
     * @param prev
     * @param waitTime
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    private boolean waitForLock(String prev,long waitTime) throws KeeperException, InterruptedException
    {
        Stat stat = zk.exists(ROOT_LOCK+"/"+prev,true);
        if(stat != null)
        {
            System.out.println(Thread.currentThread().getName()+" 等待锁 "+ROOT_LOCK+"/"+prev);
            this.countDownLatch = new CountDownLatch(1);
            /**
             * 计数等待，若等到前一个节点消失，则precess中进行countDown,停止等待，获取锁
             */
            this.countDownLatch.await(waitTime,TimeUnit.MILLISECONDS);
            this.countDownLatch = null;
            System.out.println(Thread.currentThread().getName()+" 等到了锁");
        }
        return true;
    }

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    @Override
    public void unlock()
    {
        System.out.println(Thread.currentThread().getName()+" 释放锁 "+CURRENT_LOCK);
        try
        {
            zk.delete(CURRENT_LOCK,-1);
            zk.close();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        catch (KeeperException e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Returns a new {@link Condition} instance that is bound to this
     * {@code Lock} instance.
     *
     * <p>Before waiting on the condition the lock must be held by the
     * current thread.
     * A call to {@link Condition#await()} will atomically release the lock
     * before waiting and re-acquire the lock before the wait returns.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The exact operation of the {@link Condition} instance depends on
     * the {@code Lock} implementation and must be documented by that
     * implementation.
     *
     * @return A new {@link Condition} instance for this {@code Lock} instance
     * @throws UnsupportedOperationException if this {@code Lock}
     *                                       implementation does not support conditions
     */
    @Override
    public Condition newCondition()
    {
        return null;
    }


    public class LockException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        public LockException(String e){
            super(e);
        }
        public LockException(Exception e){
            super(e);
        }
    }

}
