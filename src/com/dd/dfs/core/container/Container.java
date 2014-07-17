package com.dd.dfs.core.container;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.ha.Cluster;



/**
 * DFS���������ӿ�
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
	 * ���������
	 * @param child
	 */
	public void addChild(Container child);
	
	/**
	 * �Ƴ�������
	 * @param child
	 */
	public void removeChild(Container child);
	
	/**
	 * ����ĳ��������
	 * @param name
	 * @return
	 */
	public Container findChild(String name);
	
	/**
	 * ���������������ļ���
	 * @return
	 */
	public Container[] findChildren();
	
	/**
	 * ��ñ������������Ϣ
	 * @return
	 */
	public String getInfo();
	
	public void setInfo(String info);

	/**
	 * Servlet������invoke����
	 * @param request
	 * @param response
	 */
	public void invoke(HttpRequest request, HttpResponse response) throws IOException,Exception;

	/**
	 * ��������Ҫ��ִ̨�е���������һ���������߳�ִ��
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
	 * ���ñ�������̨�����ִ�м��
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
