package com.dd.dfs.ha;

import java.util.Map;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.Member;

import com.dd.dfs.core.container.pipe.inter.Valve;
import com.dd.dfs.data.manager.Manager;



/**
 * DFS的cluster基础接口
 * @author DD
 *
 */
public interface DFSCluster extends Cluster {
	 /**
     * Descriptive information about this component implementation.
     */
    public String info = "DFSCluster/1.0";
    
    public void addValve(Valve valve);
    
 // --------------------------------------------------------- Cluster Methods
    /**
     * Sends a message to all the members in the cluster
     * @param msg ClusterMessage
     */
    public void send(ClusterMessage msg);
    
    /**
     * Sends a message to a specific member in the cluster.
     *
     * @param msg ClusterMessage
     * @param dest Member
     */
    public void send(ClusterMessage msg, Member dest);

    /**
     * Returns that cluster has members.
     */
    public boolean hasMembers();

    /**
     * Returns all the members currently participating in the cluster.
     *
     * @return Member[]
     */
    public Member[] getMembers();
    
    /**
     * Return the member that represents this node.
     *
     * @return Member
     */
    public Member getLocalMember();
    
    public void addClusterListener(ClusterListener listener);
    
    public void removeClusterListener(ClusterListener listener);
    
    /**
     * @return The map of managers
     */
    public Map<String,ClusterManager> getManagers();

    public Manager getManager(String name);
    public String getManagerName(String name, Manager manager);
    
    public void setChannel(Channel channel);
    public Channel getChannel();
}
