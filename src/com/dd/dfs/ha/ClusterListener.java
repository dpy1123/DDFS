package com.dd.dfs.ha;

import java.io.Serializable;

import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;


/**
 * Receive SessionID cluster change from other backup node after primary session
 * node is failed.
 * 
 */
public abstract class ClusterListener implements ChannelListener {


    protected DFSCluster cluster = null;

    //--Constructor---------------------------------------------

    public ClusterListener() {
        // NO-OP
    }
    
    //--Instance Getters/Setters--------------------------------
    
    public DFSCluster getCluster() {
        return cluster;
    }

    public void setCluster(DFSCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public boolean equals(Object listener) {
        return super.equals(listener);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    //--Logic---------------------------------------------------

    @Override
    public final void messageReceived(Serializable msg, Member member) {
        if ( msg instanceof ClusterMessage ) messageReceived((ClusterMessage)msg);
    }
    @Override
    public final boolean accept(Serializable msg, Member member) {
        if ( msg instanceof ClusterMessage ) return true;
        return false;
    }



    /**
     * Callback from the cluster, when a message is received, The cluster will
     * broadcast it invoking the messageReceived on the receiver.
     * 
     * @param msg
     *            ClusterMessage - the message received from the cluster
     */
    public abstract void messageReceived(ClusterMessage msg) ;
    

    /**
     * Accept only SessionIDMessages
     * 
     * @param msg
     *            ClusterMessage
     * @return boolean - returns true to indicate that messageReceived should be
     *         invoked. If false is returned, the messageReceived method will
     *         not be invoked.
     */
    public abstract boolean accept(ClusterMessage msg) ;

}
