package com.dd.dfs.ha;

import com.dd.dfs.core.container.pipe.inter.Valve;


/**
 * 对valve接口的基本扩展，以适应cluster使用
 */
public interface ClusterValve extends Valve{
    /**
     * Returns the cluster the cluster deployer is associated with
     * @return CatalinaCluster
     */
    public DFSCluster getCluster();

    /**
     * Associates the cluster deployer with a cluster
     * @param cluster CatalinaCluster
     */
    public void setCluster(DFSCluster cluster);
}
