package com.dd.dfs.ha.tcp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.catalina.tribes.Channel;
import org.apache.catalina.tribes.ChannelListener;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.MembershipListener;
import org.apache.catalina.tribes.group.GroupChannel;
import org.apache.catalina.tribes.group.interceptors.MessageDispatch15Interceptor;
import org.apache.catalina.tribes.group.interceptors.TcpFailureDetector;
import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.container.ContainerBase;
import com.dd.dfs.core.container.pipe.inter.Valve;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;
import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.ha.ClusterListener;
import com.dd.dfs.ha.ClusterManager;
import com.dd.dfs.ha.ClusterMessage;
import com.dd.dfs.ha.ClusterValve;
import com.dd.dfs.ha.DFSCluster;
import com.dd.dfs.ha.data.ClusterDataListener;
import com.dd.dfs.ha.data.ClusterDataMessage;

/**
 * A <b>Cluster </b> implementation using simple multicast. Responsible for
 * setting up a cluster and provides callers with a valid multicast
 * receiver/sender.
 * 
 */
public class SimpleTcpCluster implements DFSCluster, Lifecycle, MembershipListener, ChannelListener {
	
	//---------------业务相关
	protected String info = "SimpleTcpCluster/1.0";
	private boolean started = false;//记录本容器是否启动
	protected String clusterName;
    /**
     * The Container associated with this Cluster.
     */
    protected Container container = null;

    /**
     * The context name <->manager association for distributed contexts.
     */
    protected Map<String, ClusterManager> managers = new HashMap<String, ClusterManager>();
    
    /**
     * Listeners of messages
     */
    protected List<ClusterListener> clusterListeners = new ArrayList<ClusterListener>();

    private List<Valve> valves = new ArrayList<Valve>();
    
    private LifecycleSupport lifecycle = null;//生命周期管理工具
    
    //---------------集群相关
	/**
	 * Group channel.
	 */
	protected Channel channel = new GroupChannel();
	/**
	 * call Channel.heartbeat() at container background thread
	 * 
	 * @see org.apache.catalina.tribes.group.GroupChannel#heartbeat()
	 */
	protected boolean heartbeatBackgroundEnabled = false;

	private int channelSendOptions = Channel.SEND_OPTIONS_ASYNCHRONOUS;

	private int channelStartOptions = Channel.DEFAULT;
	
	public SimpleTcpCluster() {
		lifecycle = new LifecycleSupport(this);
	}
	
	@Override
	public String getInfo() {
		return this.info;
	}

	@Override
	public String getClusterName() {
		return this.clusterName;
	}

	@Override
	public void setClusterName(String clusterName) {
		this.clusterName = clusterName;
	}

	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public Container getContainer() {
		return this.container;
	}

	/**
	 * Return heartbeat enable flag (default false)
	 * 
	 * @return the heartbeatBackgroundEnabled
	 */
	public boolean isHeartbeatBackgroundEnabled() {
		return heartbeatBackgroundEnabled;
	}

	/**
	 * enabled that container backgroundThread call heartbeat at channel
	 * 
	 * @param heartbeatBackgroundEnabled the heartbeatBackgroundEnabled to set
	 */
	public void setHeartbeatBackgroundEnabled(boolean heartbeatBackgroundEnabled) {
		this.heartbeatBackgroundEnabled = heartbeatBackgroundEnabled;
	}

	@Override
	public void registerManager(Manager manager) {
		if (!(manager instanceof ClusterManager)) {
			System.out.println("Manager [ "+ manager + "] does not implement ClusterManager, addition to cluster has been aborted.");
			return;
		}
		ClusterManager cmanager = (ClusterManager) manager;
		String clusterName = getManagerName(cmanager.getName(), manager);
		cmanager.setName(clusterName);
		cmanager.setCluster(this);

		managers.put(clusterName, cmanager);
	}

	@Override
	public void removeManager(Manager manager) {
		if (manager != null && manager instanceof ClusterManager ) {
            ClusterManager cmgr = (ClusterManager) manager;
            managers.remove(getManagerName(cmgr.getName(),manager));
            cmgr.setCluster(null);
        }
	}

	@Override
	public Manager getManager(String name) {
		return managers.get(name);
	}

	@Override
	public Map<String, ClusterManager> getManagers() {
		return managers;
	}

	@Override
	public String getManagerName(String name, Manager manager) {
		String clusterName = name;
		if (clusterName == null && manager instanceof ClusterManager)
			clusterName = ((ClusterManager) manager).getName();
		return clusterName;
	}
	
	/**
     * 后台处理任务
     * @see org.apache.catalina.tribes.group.GroupChannel#heartbeat()
     * @see org.apache.catalina.tribes.group.GroupChannel.HeartbeatThread#run()
     * 
     */
	@Override
	public void backgroundProcess() {
		//send a heartbeat through the channel        
        if ( isHeartbeatBackgroundEnabled() && channel !=null ) channel.heartbeat();
	}


	@Override
	public void addValve(Valve valve) {
		if (valve instanceof ClusterValve && (!valves.contains(valve)))
			valves.add(valve);
	}

	@Override
	public void send(ClusterMessage msg) {
		send(msg, null);
	}

	@Override
	public void send(ClusterMessage msg, Member dest) {
		try {
            msg.setAddress(getLocalMember());
            int sendOptions = channelSendOptions;
            if (msg instanceof ClusterDataMessage && ((ClusterDataMessage)msg).getEventType() == ClusterDataMessage.EVT_ALL_SESSION_DATA) {
                sendOptions = Channel.SEND_OPTIONS_SYNCHRONIZED_ACK|Channel.SEND_OPTIONS_USE_ACK;
            }
            if (dest != null) {
                if (!getLocalMember().equals(dest)) {
                    channel.send(new Member[] {dest}, msg, sendOptions);
                } else
                	System.out.println("Unable to send message to local member " + msg);
            } else {
                Member[] destmembers = channel.getMembers();
                if (destmembers.length>0)
                    channel.send(destmembers,msg, sendOptions);
            }
        } catch (Exception x) {
            System.out.println("Unable to send message through cluster sender." + x);
        }
	}
	
	/**
	 * has members
	 */
	protected boolean hasMembers = false;
	@Override
	public boolean hasMembers() {
		return hasMembers;
	}

	/**
	 * Get all current cluster members
	 * @return all members or empty array
	 */
	@Override
	public Member[] getMembers() {
		return channel.getMembers();
	}

	@Override
	public Member getLocalMember() {
		return channel.getLocalMember(true);
	}

	/**
     * Add cluster message listener and register cluster to this listener.
     * 
     */
	@Override
	public void addClusterListener(ClusterListener listener) {
		if (listener != null && !clusterListeners.contains(listener)) {
            clusterListeners.add(listener);
            listener.setCluster(this);
        }
	}

	/**
     * Remove message listener and deregister Cluster from listener.
     * 
     */
	@Override
	public void removeClusterListener(ClusterListener listener) {
		if (listener != null) {
			clusterListeners.remove(listener);
			listener.setCluster(null);
		}
	}

	@Override
	public void setChannel(Channel channel) {
		this.channel = channel;
	}

	@Override
	public Channel getChannel() {
		return this.channel;
	}

	
	//=============以下是Lifecycle接口相关函数=============
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}

	@Override
	public synchronized void start() {
		System.out.println("["+this.getInfo()+"] Start!");
		if(started){//已被启动
			return;
		}
		
		try {
			checkDefaults();
			registerClusterValve();
			channel.addMembershipListener(this);
			channel.addChannelListener(this);
			channel.start(channelStartOptions);
//			registerMember(channel.getLocalMember(false));
		} catch (Exception x) {
			System.out.println("Unable to start cluster." + x);
		}

		//启动
		started = true;
	}

	protected void checkDefaults() {
		if (clusterListeners.size() == 0) {
			addClusterListener(new ClusterDataListener());
		}
		if (valves.size() == 0) {
//			addValve(new ReplicationValve());
		}
		if (channel == null)
			channel = new GroupChannel();
		if (channel instanceof GroupChannel
				&& !((GroupChannel) channel).getInterceptors().hasNext()) {
			channel.addInterceptor(new MessageDispatch15Interceptor());
			channel.addInterceptor(new TcpFailureDetector());
		}
	}
	
	/**
     * register all cluster valve to container
     * @throws Exception
     */
    protected void registerClusterValve() throws Exception {
        if(container != null && container instanceof ContainerBase) {
            for (Iterator<Valve> iter = valves.iterator(); iter.hasNext();) {
                ClusterValve valve = (ClusterValve) iter.next();
                if (valve != null) {
					((ContainerBase) container).getPipeLine().addValve(valve);
					valve.setCluster(this);
                }
            }
        }
    }
    
    /**
     * unregister all cluster valve to container
     * @throws Exception
     */
    protected void unregisterClusterValve() throws Exception {
        for (Iterator<Valve> iter = valves.iterator(); iter.hasNext();) {
            ClusterValve valve = (ClusterValve) iter.next();
            if (valve != null) {
            	((ContainerBase) container).getPipeLine().removeValve(valve);
                valve.setCluster(this);
            }
        }
    }
    
	@Override
	public void stop() {
		System.out.println("["+this.getInfo()+"] Stop!");
//        unregisterMember(channel.getLocalMember(false));
        this.managers.clear();
        try {
            channel.stop(channelStartOptions);
            channel.removeChannelListener(this);
            channel.removeMembershipListener(this);
            this.unregisterClusterValve();
        } catch (Exception x) {
            System.out.println("Unable to stop cluster." + x);
        }
	}
	
	//-------------------------------MembershipListener的实现
	/**
	 * 监听到cluster中加入新成员
	 */
	@Override
	public void memberAdded(Member member) {
		System.out.println("new Member in: ["+member.getName()+" "+member.getPort()+"]");
		try {
            hasMembers = channel.hasMembers();
//            registerMember(member);

        } catch (Exception x) {
            System.out.println("Unable to connect to replication system." + x);
        }
	}

	@Override
	public void memberDisappeared(Member member) {
		System.out.println("Member out: ["+member.getName()+"]");
		try {
            hasMembers = channel.hasMembers();            

//            unregisterMember(member);

        } catch (Exception x) {
        	System.out.println("Unable remove cluster node from replication system." + x);
        }
	}
	
	//--------------------------------ChannelListener的实现
	@Override
	public void messageReceived(Serializable msg, Member sender) {
		ClusterMessage fwd = (ClusterMessage) msg;
        fwd.setAddress(sender);
        messageReceived(fwd);
	}

	public void messageReceived(ClusterMessage message) {

//		System.out.println("Assuming clocks are synched: Replication for "
//				+ message.getUniqueId() + " took="
//				+ (System.currentTimeMillis() - (message).getTimestamp())
//				+ " ms.");

        //invoke all the listeners
        boolean accepted = false;
        if (message != null) {
            for (Iterator<ClusterListener> iter = clusterListeners.iterator();
                    iter.hasNext();) {
                ClusterListener listener = iter.next();
                if (listener.accept(message)) {
                    accepted = true;
                    listener.messageReceived(message);
                }
            }
            if (!accepted) {//如果消息没有被任何一个listener接收
				System.out.println("Message " + message.toString()
						+ " from type " + message.getClass().getName()
						+ " transfered but no listener registered");
            }
        }
        return;
    }
	
	@Override
	public boolean accept(Serializable msg, Member sender) {
		 return (msg instanceof ClusterMessage);
	}

}
