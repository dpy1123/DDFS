package com.dd.dfs.ha;

import com.dd.dfs.core.container.Container;
import com.dd.dfs.data.manager.Manager;



/**
 * A <b>Cluster</b> works as a Cluster client/server for the local host
 * Different Cluster implementations can be used to support different
 * ways to communicate within the Cluster. A Cluster implementation is
 * responsible for setting up a way to communicate within the Cluster
 * and also supply "ClientApplications" with <code>ClusterSender</code>
 * used when sending information in the Cluster and
 * <code>ClusterInfo</code> used for receiving information in the Cluster.
 * 
 */
public interface Cluster {
	
    public String getInfo();

    public String getClusterName();

    public void setClusterName(String clusterName);

    public void setContainer(Container container);

    public Container getContainer();
    
    public void registerManager(Manager manager);

    public void removeManager(Manager manager);

    public Manager getManager(String name);

 // --------------------------------------------------------- Cluster Wide Deployments

    public void backgroundProcess();
}
