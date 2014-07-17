package com.dd.dfs.core.container;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.ha.Cluster;



/**
 * DFS处理容器接口
 * @author DD
 *
 */
public interface Container {
	/**
     * Return the Container for which this Container is a child, if there is
     * one.  If there is no defined parent, return <code>null</code>.
     */
    public Container getParent();
    
    public void setParent(Container container);
    
	/**
	 * 添加子容器
	 * @param child
	 */
	public void addChild(Container child);
	
	/**
	 * 移除子容器
	 * @param child
	 */
	public void removeChild(Container child);
	
	/**
	 * 查找某个子容器
	 * @param name
	 * @return
	 */
	public Container findChild(String name);
	
	/**
	 * 查找所有子容器的集合
	 * @return
	 */
	public Container[] findChildren();
	
	/**
	 * 获得本容器的相关信息
	 * @return
	 */
	public String getInfo();
	
	public void setInfo(String info);

	/**
	 * Servlet容器的invoke方法
	 * @param request
	 * @param response
	 */
	public void invoke(HttpRequest request, HttpResponse response) throws IOException,Exception;

	/**
	 * 容器中需要后台执行的任务，由另一个单独的线程执行
	 */
	public void backgroundProcess();

	/**
     * Get the delay between the invocation of the backgroundProcess method on
     * this container and its children. Child containers will not be invoked
     * if their delay value is not negative (which would mean they are using 
     * their own thread). Setting this to a positive value will cause 
     * a thread to be spawn. After waiting the specified amount of time, 
     * the thread will invoke the executePeriodic method on this container 
     * and all its children.
     */
	public int getBackgroundProcessorDelay();
	
	/**
	 * 设置本容器后台任务的执行间隔
	 * @param delay 
	 */
	public void setBackgroundProcessorDelay(int delay);

	/**
     * Return the Cluster with which this Container is associated.  If there is
     * no associated Cluster, return the Cluster associated with our parent
     * Container (if any); otherwise return <code>null</code>.
     */
    public Cluster getCluster();


    /**
     * Set the Cluster with which this Container is associated.
     *
     * @param cluster the Cluster with which this Container is associated.
     */
    public void setCluster(Cluster cluster);
}
