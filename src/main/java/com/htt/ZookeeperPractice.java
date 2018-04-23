package com.htt;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * Author: zky
 * Date: 2018-04-20
 * Time: 14:33:48
 * Description:
 */
public class ZookeeperPractice
{
    private ZooKeeper zk =null;

    /**
     * create connection.
     * @param connectionString
     * @param sessionTimeOut
     */
    public void createConnection(String connectionString ,int sessionTimeOut)
    {
        this.releaseConnection();
        try
        {
            zk = new ZooKeeper(connectionString, sessionTimeOut, new Watcher()
            {
                /**
                 * 监控所有被触发的事件
                 * @param watchedEvent
                 */
                @Override
                public void process(WatchedEvent watchedEvent)
                {
                    System.out.println("已经出发了"+watchedEvent.getType()+"事件!");
                }
            });
        }
        catch (IOException e)
        {
            System.out.println("创建连接失败，发生IOException.");
            e.printStackTrace();
        }
    }

    /**
     * close the connection.
     */
    public void releaseConnection()
    {
        if(zk != null)
        {
            try
            {
                this.zk.close();
            }
            catch (InterruptedException e)
            {
                System.out.println("close the connection produce exception.");
                e.printStackTrace();
            }
        }
    }


    /**
     * create node.
     * @param path
     * @param data
     * @return
     */
    public boolean createPath(String path,String data)
    {
        try
        {
            //临时永久节点
            String node = zk.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);

            System.out.println("节点创建成功，Path:"+node+",Content:"+data);
        }
        catch (KeeperException e)
        {
            System.out.println("节点创建失败，发生KeeperException.");
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            System.out.println("节点创建失败，发生InterruptedException.");

            e.printStackTrace();
        }

        return true;
    }

    /**
     * read the node's content.
     * @param path
     * @return
     */
    public String readData(String path)
    {
        try
        {
            String data = new String(zk.getData(path, false, null));

            System.out.println("数据获取成功:"+data);
            return data;
        }
        catch (KeeperException e)
        {
            System.out.println("读取数据失败，发生KeeperException,path:"+path);

            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            System.out.println("读取数据失败，发生InterruptedException,path:"+path);

            e.printStackTrace();
        }

        return "";
    }


    /**
     * update the node's data.
     * @param path
     * @param data
     * @return
     */
    public boolean writeData(String path,String data)
    {
        try
        {
            Stat stat = zk.setData(path, data.getBytes(), -1);
            System.out.println("更新数据成功，path:"+path+",stat"+stat);
            return true;
        }
        catch (InterruptedException e)
        {
            System.out.println("更新数据失败,发生InterruptedException异常，path:"+path);
            e.printStackTrace();
        }
        catch (KeeperException e)
        {
            System.out.println("更新数据失败，发生KeeperException异常，path:"+path);
            e.printStackTrace();
        }

        return false;
    }

    /**
     * delete node.
     * @param path
     */
    public void deleteNode(String path)
    {
        try
        {

            zk.delete(path,-1);

            System.out.println("删除节点成功，path:"+path);
        }
        catch (InterruptedException e)
        {
            System.out.println("删除节点失败，发生InterruptedException异常，path:"+path);
            e.printStackTrace();
        }
        catch (KeeperException e)
        {
            System.out.println("删除节点失败，发生KeeperException异常，path:"+path);

            e.printStackTrace();

        }
    }

    public List<String> getChildrens(String path)
    {
        try
        {
            return zk.getChildren(path,true);
        }
        catch (KeeperException e)
        {
            e.printStackTrace();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        return null;

    }
}
