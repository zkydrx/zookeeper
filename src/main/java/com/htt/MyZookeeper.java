package com.htt;

import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-04-11
 * Time: 15:09:07
 * Description:
 */
public class MyZookeeper
{
    private static String connectString="35.189.178.24:2181";

    //会话的超时时间
    private static final int sessionTimeOut = 888888;
    //声明ZooKeeper实例
    ZooKeeper zookeeper;

    //创建Watcher实例
    Watcher watcher = new Watcher()
    {
        @Override
        public void process(WatchedEvent watchedEvent)
        {
            System.out.println("监听到的事件："+watchedEvent.toString());
        }
    };

    //初始化ZooKeeper实例
    public void createZkInstance() throws IOException
    {
        zookeeper = new ZooKeeper(connectString,MyZookeeper.sessionTimeOut,this.watcher);
    }


    //关闭ZooKeeper实例
    public void zkClose() throws InterruptedException
    {
        zookeeper.close();
    }

}
