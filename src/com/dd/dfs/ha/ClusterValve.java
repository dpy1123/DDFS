package com.dd.dfs.ha;

import com.dd.dfs.core.container.pipe.inter.Valve;


/**
 * ��valve�ӿڵĻ�����չ������Ӧclusterʹ��
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
