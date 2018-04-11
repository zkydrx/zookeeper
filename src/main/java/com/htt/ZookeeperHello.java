package com.htt;


import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-04-11
 * Time: 15:34:49
 * Description:
 */
public class ZookeeperHello
{
    public static void main(String[] args) throws IOException, KeeperException, InterruptedException
    {
        //连接zookeeper服务
        ZooKeeper zooKeeper = new ZooKeeper("35.189.178.24:2181", 3000000, new
                                DemoWatcher());
        //连接zookeeeper 集群服务使用开源框架zkclient简化代码
//        ZooKeeper zooKeeper = new ZooKeeper("35.189.178.24:2181,35.189.178.24:2182,35.189.178.24:2183", 3000000, new
//                DemoWatcher());
//        if (!zooKeeper.getState().equals(ZooKeeper.States.CONNECTED))
//        {
//            while (true)
//            {
//                if (zooKeeper.getState().equals(ZooKeeper.States.CONNECTED))
//                {
//                    break;
//                }
//                try
//                {
//                    TimeUnit.SECONDS.sleep(5);
//                }
//                catch (InterruptedException e)
//                {
//                    e.printStackTrace();
//                }
//            }
//        }

        String node = "/app1";

        /**
         * 可以用以下代码代替上面的繁琐代码。
         */
//        ZkClient zkClient = new ZkClient("35.189.178.24:2181,35.189.178.24:2182,35.189.178.24:2183");
//
//        if(!zkClient.exists(node))
//        {
//            zkClient.createPersistent(node,"hello,zookeeper");
//        }
//        System.out.println(zkClient.readData(node));
        Stat stat = zooKeeper.exists(java.lang.String.valueOf(node), false);
        if (stat == null)
        {
            String createResult = zooKeeper.create(node, "test".getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode
                    .PERSISTENT);
            System.out.println(createResult);
        }

        byte[] data = zooKeeper.getData(node, false, stat);
        System.out.println(new String(data));
        zooKeeper.close();

    }

    static class DemoWatcher implements Watcher
    {

        @Override
        public void process(WatchedEvent event)
        {
            System.out.println("----------------------->");
            System.out.println("path: " + event.getPath());
            System.out.println("type: " + event.getType());
            System.out.println("stat: " + event.getState());
            System.out.println("<-----------------------");
        }
    }
}
