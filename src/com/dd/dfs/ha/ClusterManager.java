package com.dd.dfs.ha;


import java.io.IOException;

import org.apache.catalina.tribes.io.ReplicationStream;

import com.dd.dfs.data.manager.Manager;


/**
 * 基于集群的Manager接口
 * 
 */
public interface ClusterManager extends Manager {

   /**
    * A message was received from another node, this
    * is the callback method to implement if you are interested in
    * receiving replication messages.
    * @param msg - the message received.
    */
   public void messageDataReceived(ClusterMessage msg);


   /**
    * When the manager expires session not tied to a request.
    * The cluster will periodically ask for a list of sessions
    * that should expire and that should be sent across the wire.
    * @return String[] The invalidated sessions
    */
   public String[] getInvalidatedSessions();
   
   /**
    * Return the name of the manager, at host /context name and at engine hostname+/context.
    * @return String
    * @since 5.5.10
    */
   public String getName();
   
   /**
    * Set the name of the manager, at host /context name and at engine hostname+/context
    * @param name
    * @since 5.5.10
    */
   public void setName(String name);
         
   public DFSCluster getCluster();

   public void setCluster(DFSCluster cluster);

   public ReplicationStream getReplicationStream(byte[] data) throws IOException;

   public ReplicationStream getReplicationStream(byte[] data, int offset, int length) throws IOException;
   
   public boolean isNotifyListenersOnReplication();

}
