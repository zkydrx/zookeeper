package com.htt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-04-20
 * Time: 20:52:13
 * Description:
 */
class ZookeeperPracticeTest
{

    private ZookeeperPractice zookeeperPractice = new ZookeeperPractice();
    private final String connectionString="35.189.178.24:2181";
    private final int sessionTimeOut=3000;
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

    @Test
    void createConnection()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        zookeeperPractice.releaseConnection();
    }

    @Test
    void releaseConnection()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        zookeeperPractice.releaseConnection();
    }

    @Test
    void createPath()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
//        boolean linux = zookeeperPractice.createPath("/abcd", "linux");
        for (int i = 0; i < 100; i++)
        {
            zookeeperPractice.createPath("/a"+i,"i"+i);
        }
    }

    @Test
    void readData()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        String s = zookeeperPractice.readData("/a0");
        System.out.println(s);
    }

    @Test
    void writeData()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        boolean b = zookeeperPractice.writeData("/a0", "a0----->linux is the wonderful os in the world.");
    }

    @Test
    void deleteNode()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        for (int i = 0; i < 10; i++)
        {
            zookeeperPractice.deleteNode("/a"+i);

        }
    }

    @Test
    void getChildrens()
    {
        zookeeperPractice.createConnection(connectionString,sessionTimeOut);
        List<String> childrens = zookeeperPractice.getChildrens("/zookeeper");

        for (String children : childrens)
        {
            System.out.println(children);
        }
    }
}