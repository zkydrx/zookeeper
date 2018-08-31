package com.lock;


import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-08-31
 * Time: 10:46:08
 * Description:
 */
class DistributedLockTest
{

    static int count = 500;

    @BeforeEach
    void setUp()
    {
        System.out.println("String...");
    }

    @AfterEach
    void tearDown()
    {
        System.out.println("Ending...");
    }

    public static void secskill()
    {
        System.out.println(--count);
    }

    public static void main(String[] args)
    {
        Runnable runnable = new Runnable()
        {
            DistributedLock distributedLock = null;

            @Override
            public void run()
            {
                try
                {
                    distributedLock = new DistributedLock("130.211.252.172","testz");
                    distributedLock.lock();
                    secskill();
                    System.out.println(Thread.currentThread().getName()+" 正在运行");
                }
                finally
                {
                    if(distributedLock != null)
                    {
                        distributedLock.unlock();
                    }
                }

            }


        };
        for (int i = 0; i < 10; i++)
        {
            Thread thread = new Thread(runnable);
            thread.start();
        }
    }

}