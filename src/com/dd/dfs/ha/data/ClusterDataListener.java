package com.dd.dfs.ha.data;

import java.util.Map;

import com.dd.dfs.ha.ClusterListener;
import com.dd.dfs.ha.ClusterManager;
import com.dd.dfs.ha.ClusterMessage;


/**
 * Receive replicated DataMessage form other cluster node.
 */
public class ClusterDataListener extends ClusterListener {
    
    /**
     * The descriptive information about this implementation.
     */
    protected static final String info = "ClusterDataListener/1.1";

    //--Constructor---------------------------------------------

    public ClusterDataListener() {
        // NO-OP
    }

    //--Logic---------------------------------------------------

    /**
     * Return descriptive information about this implementation.
     */
    public String getInfo() {
        return (info);
    }

    /**
     * Callback from the cluster, when a message is received, The cluster will
     * broadcast it invoking the messageReceived on the receiver.
     * 
     * @param myobj
     *            ClusterMessage - the message received from the cluster
     */
    @Override
    public void messageReceived(ClusterMessage myobj) {
        if (myobj != null && myobj instanceof ClusterDataMessage) {
            ClusterDataMessage msg = (ClusterDataMessage) myobj;
            String name = msg.getContextName();
            //check if the message is a EVT_GET_ALL_SESSIONS,
            //if so, wait until we are fully started up
            Map<String,ClusterManager> managers = cluster.getManagers() ;
            if (name == null) {
                for (Map.Entry<String, ClusterManager> entry : managers.entrySet()) {
					if (entry.getValue() != null)
						entry.getValue().messageDataReceived(msg);
					else {
						// this happens a lot before the system has started up
						System.out.println("Cluster manager doesn't exist:" + entry.getKey());
					}
                }
            } else {
                ClusterManager mgr = managers.get(name);
                if (mgr != null) {
                    mgr.messageDataReceived(msg);
                } else {
                	System.out.println("Cluster manager doesn't exist:" + name);

                    // A no context manager message is replied in order to avoid
                    // timeout of GET_ALL_SESSIONS sync phase.
                    if (msg.getEventType() == ClusterDataMessage.EVT_GET_ALL_SESSIONS) {
                        ClusterDataMessage replymsg = new ClusterDataMessageImpl(name,
                                ClusterDataMessage.EVT_ALL_SESSION_NOCONTEXTMANAGER,
                                null, "NO-CONTEXT-MANAGER","NO-CONTEXT-MANAGER-" + name);
                        cluster.send(replymsg, msg.getAddress());
                    }
                }
            }
        }
        return;
    }

    /**
     * Accept only SessionMessage
     * 
     * @param msg
     *            ClusterMessage
     * @return boolean - returns true to indicate that messageReceived should be
     *         invoked. If false is returned, the messageReceived method will
     *         not be invoked.
     */
    @Override
    public boolean accept(ClusterMessage msg) {
        return (msg instanceof ClusterDataMessage);
    }
}

