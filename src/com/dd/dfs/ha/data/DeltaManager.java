package com.dd.dfs.ha.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.io.ReplicationStream;

import com.dd.dfs.core.container.Container;
import com.dd.dfs.data.Data;
import com.dd.dfs.data.DirectoryData;
import com.dd.dfs.data.FileData;
import com.dd.dfs.data.StandardData;
import com.dd.dfs.data.manager.StandardManager;
import com.dd.dfs.ha.Cluster;
import com.dd.dfs.ha.ClusterManager;
import com.dd.dfs.ha.ClusterMessage;
import com.dd.dfs.ha.DFSCluster;

/**
 * 对标准Manager的扩展，使其支持cluster使用
 * @author dd
 *
 */
public class DeltaManager extends StandardManager
        implements ClusterManager {
	/**
	 * The descriptive information about this class.
	 */
	protected static final String info = "DeltaManager/1.0";
	
	/**
	 * 指定发出该消息的上下文的名称  (for ClusterDataMessageImpl)
	 */
	protected String name = null;
	 
    /**
     * A reference to the cluster
     */
    protected DFSCluster cluster = null;

    /**
     * Should listeners be notified?
     */
    private boolean notifyListenersOnReplication = true;
    
    /**
     * 默认60s传输超时时间
     */
    private int stateTransferTimeout = 60;
    /**
     * 是否发送全部Session
     */
    private boolean sendAllSessions = true;
    private int sendAllSessionsSize = 1000 ;
    /**
     * wait time between send session block (default 2 sec) 
     */
    private int sendAllSessionsWaitTime = 2 * 1000 ; 
    
    private volatile boolean stateTransfered = false;
    private ArrayList<ClusterDataMessage> receivedMessageQueue = new ArrayList<ClusterDataMessage>();
    private boolean receiverQueue = false;

    @Override
    public String getInfo() {
    	return info;
    }

    @Override
    public DFSCluster getCluster() {
        return cluster;
    }

    @Override
    public void setCluster(DFSCluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public boolean isNotifyListenersOnReplication() {
        return notifyListenersOnReplication;
    }

    public void setNotifyListenersOnReplication(boolean notifyListenersOnReplication) {
        this.notifyListenersOnReplication = notifyListenersOnReplication;
    }

    /**
     * Open Stream and use correct ClassLoader (Container) Switch
     * ThreadClassLoader
     * 
     * @param data
     * @return The object input stream
     * @throws IOException
     */
    @Override
    public ReplicationStream getReplicationStream(byte[] data) throws IOException {
        return getReplicationStream(data,0,data.length);
    }

    public ReplicationStream getReplicationStream(byte[] data, int offset, int length) throws IOException {
        ByteArrayInputStream fis = new ByteArrayInputStream(data, offset, length);
        return new ReplicationStream(fis, new ClassLoader[] { Thread.currentThread().getContextClassLoader()});
    }

    
    /**
     * 向集群发送新建的Data
     * @param newData
     * @param sender 向哪个成员发送，如果是null则向全体成员发送
     * @throws IOException 
     */
    public void sendCreateData(Data newData, Member sender) throws IOException {
        	long sendTimestamp = System.currentTimeMillis();
        	byte[] data = serializeSessions(new Data[] { newData });
        	ClusterDataMessage msg = new ClusterDataMessageImpl(getName(),
					ClusterDataMessage.EVT_SESSION_CREATED,
					data, 
					newData.getId(),
					newData.getId() + "-" + System.currentTimeMillis());
            msg.setTimestamp(sendTimestamp);
//            counterSend_EVT_SESSION_CREATED++;
    		cluster.send(msg, sender);    
    }
    
    /**
     * 向集群发送发生了变化的Data
     * @param deltData
     * @param sender 向哪个成员发送，如果是null则向全体成员发送
     * @throws IOException 
     */
    //主要参考Tomcat的ReplicationValve#invoke()和DeltaManager#requestCompleted()
    public void sendDeltaData(Data deltData, Member sender) throws IOException{
    	//由于本系统的data设计和Session略有不同，Session中是会放一些object的，但是data里面除了本身的属性，就只有一个hash表
    	//因此不考虑使用Tomcat的DeltaRequest和DeltaSession的复杂设计
    	//在这里我们像启动时那样，直接传变化了的data
		long sendTimestamp = System.currentTimeMillis();
		byte[] data = serializeSessions(new Data[] { deltData });
		ClusterDataMessage msg = new ClusterDataMessageImpl(getName(),
												ClusterDataMessage.EVT_SESSION_DELTA,
												data, 
												deltData.getId(),
												deltData.getId() + "-" + System.currentTimeMillis());
		msg.setTimestamp(sendTimestamp);
		cluster.send(msg, sender);    
    }
    
    /**
     * 向集群发送要移除的Data
     * @param id
     * @param sender 向哪个成员发送，如果是null则向全体成员发送
     * @throws IOException 
     */
    public void sendRemoveData(String id, Member sender) throws IOException {
//            counterSend_EVT_SESSION_EXPIRED++;
        	long sendTimestamp = System.currentTimeMillis();
        	ClusterDataMessage msg = new ClusterDataMessageImpl(getName(),
					ClusterDataMessage.EVT_SESSION_EXPIRED,
					null, 
					id,
					id+ "-EXPIRED-MSG");
            msg.setTimestamp(sendTimestamp);
    		cluster.send(msg, sender);    
    }
    
    /**
     * 启动manager，并通知集群
     */
    @Override
    public void start() {
    	super.start();
    	
     	//为本manager设置cluster
        Cluster cluster = getCluster() ;
		if (cluster == null) {
			Container context = getContainer();
			if (context != null) {
				cluster = context.getCluster();
				if (cluster instanceof DFSCluster) {
					setCluster((DFSCluster) cluster);
				} else {
					cluster = null;
				}
			}
		} 
		if (cluster == null) {
            System.out.println("deltaManager.noCluster");
            return;
        }
		//为Manager设置name
		setName(getContainer().getInfo()+"#"+getInfo());

		//将本manager注册到cluster
		cluster.registerManager(this);
		
		//从其他集群结点获取本地没有的数据
		getAllClusterDatas();
    }
    
    /**
     * get from first session master the backup from all clustered sessions
     * @see #findMasterMember()
     */
    /*
     * 关于本方法的流程分析：
     * Node1							Node2
     * 1.node1正常启动，loadSessions		
     * 									2.node2启动，调用getAllClusterDatas，transfered=false，receiverQueue=true，send msg and wait.....
     * 3.node1收到msg，sendSessions
     * 									4.node2收到msg，loadSessions
     * 5.node1发送完成msg
     * 									6.node2收到，并继续getAllClusterDatas后面的方法
     * 因为receiverQueue=true之后，启动了消息缓存队列，第2步wait时候收到的消息都没有处理，只是放入的队列
     * 当第6步唤醒的时候，处理所有队列中未处理的消息，清空队列
     * */
    public synchronized void getAllClusterDatas() {
        if (cluster != null && cluster.getMembers().length > 0) {
            long beforeSendTime = System.currentTimeMillis();
            Member mbr = findMasterMember();
            if(mbr == null) { // No domain member found
                 return;
            }
            ClusterDataMessage msg = new ClusterDataMessageImpl(this.getName(),ClusterDataMessage.EVT_GET_ALL_SESSIONS, null, "GET-ALL","GET-ALL-" + getName());
            msg.setTimestamp(beforeSendTime);
            // set reference time
//            stateTransferCreateSendTime = beforeSendTime ;
            stateTransfered = false ;
            // FIXME This send call block the deploy thread, when sender waitForAck is enabled
            try {
                synchronized(receivedMessageQueue) {
                	receiverQueue = true ;
                }
                cluster.send(msg, mbr);
                waitForSendAllSessions(beforeSendTime);
            } finally {
                synchronized(receivedMessageQueue) {
                    for (Iterator<ClusterDataMessage> iter = receivedMessageQueue.iterator(); iter.hasNext();) {
                    	ClusterDataMessage smsg = iter.next();
                        messageReceived(smsg, smsg.getAddress() != null ? (Member) smsg.getAddress() : null);
                    }        
                    receivedMessageQueue.clear();
                    receiverQueue = false ;
                }
           }
        } 
    }
    
    /**
     * Find the master of the session state
     * @return master member of sessions 
     */
    protected Member findMasterMember() {
        Member mbr = null;
        Member mbrs[] = cluster.getMembers();
        if(mbrs.length != 0 ) mbr = mbrs[0];//因为集群成员中的第一个必然是从文件系统中加载了上次关闭前的Sessions
        									//因此启动的时候，只要从第一个成员拿所有的Session就可以了。
        return mbr;
    }
    
    /**
     * Wait that cluster session state is transfer or timeout after 60 Sec
     * With stateTransferTimeout == -1 wait that backup is transfered (forever mode)
     */
    protected void waitForSendAllSessions(long beforeSendTime) {
        long reqStart = System.currentTimeMillis();
        long reqNow = reqStart ;
        boolean isTimeout = false;
        if(getStateTransferTimeout() > 0) {
            // wait that state is transfered with timeout check
            do {
                try {
                    Thread.sleep(100);
                } catch (Exception sleep) {
                    //
                }
                reqNow = System.currentTimeMillis();
                isTimeout = ((reqNow - reqStart) > (1000 * getStateTransferTimeout()));
            } while ((!getStateTransfered()) && (!isTimeout));
        } else {
            if(getStateTransferTimeout() == -1) {
                // wait that state is transfered
                do {
                    try {
                        Thread.sleep(100);
                    } catch (Exception sleep) {
                    }
                } while (!getStateTransfered());
                reqNow = System.currentTimeMillis();
            }
        }
        if (isTimeout) {
            System.out.println("deltaManager.noSessionState on "+getName()+" startat: "+new Date(beforeSendTime)+" wait: "+Long.valueOf(reqNow - beforeSendTime));
        } else {
            System.out.println("deltaManager.sessionReceived on "+getName()+" startat: "+ new Date(beforeSendTime)+" wait: "+ Long.valueOf(reqNow - beforeSendTime));
        }
    }
    
    @Override
    public void stop() {
    	super.stop();
    	getCluster().removeManager(this);
    }
    
    /**
     * This method is called by the received thread when a SessionMessage has
     * been received from one of the other nodes in the cluster.
     * 
     * @param msg -
     *            the message received
     * @param sender -
     *            the sender of the message, this is used if we receive a
     *            EVT_GET_ALL_SESSION message, so that we only reply to the
     *            requesting node
     */
    protected void messageReceived(ClusterDataMessage msg, Member sender) {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        try {
            switch (msg.getEventType()) {
                case ClusterDataMessage.EVT_GET_ALL_SESSIONS: {
                    handleGET_ALL_SESSIONS(msg,sender);
                    break;
                }
                case ClusterDataMessage.EVT_ALL_SESSION_DATA: {
                    handleALL_SESSION_DATA(msg,sender);
                    break;
                }
                case ClusterDataMessage.EVT_ALL_SESSION_TRANSFERCOMPLETE: {
                    handleALL_SESSION_TRANSFERCOMPLETE(msg,sender);
                    break;
                }
                case ClusterDataMessage.EVT_SESSION_CREATED: {
                    handleSESSION_CREATED(msg,sender);
                    break;
                }
                case ClusterDataMessage.EVT_SESSION_EXPIRED: {
                    handleSESSION_EXPIRED(msg,sender);
                    break;
                }
//                case ClusterDataMessage.EVT_SESSION_ACCESSED: {
//                    handleSESSION_ACCESSED(msg,sender);
//                    break;
//                }
                case ClusterDataMessage.EVT_SESSION_DELTA: {
                   handleSESSION_DELTA(msg,sender);
                   break;
                }
//                case ClusterDataMessage.EVT_CHANGE_SESSION_ID: {
//                    handleCHANGE_SESSION_ID(msg,sender);
//                    break;
//                 }
//                case ClusterDataMessage.EVT_ALL_SESSION_NOCONTEXTMANAGER: {
//                    handleALL_SESSION_NOCONTEXTMANAGER(msg,sender);
//                    break;
//                 }
                default: {
                    //we didn't recognize the message type, do nothing
                    break;
                }
            } //switch
        } catch (Exception x) {
            System.out.println("deltaManager.receiveMessage.error"+ x);
        } finally {
            Thread.currentThread().setContextClassLoader(contextLoader);
        }
    }

    // -------------------------------------------------------- message receiver handler

    /**
     * handle receive session is expire at other node ( expire session also here)
     * @param msg
     * @param sender
     * @throws IOException
     */
    protected void handleSESSION_EXPIRED(ClusterDataMessage msg,Member sender) throws IOException {
//        counterReceive_EVT_SESSION_EXPIRED++;
		Data curData = findData(msg.getDataID());
		if (curData != null) {
			remove(curData);
		}
    }
    
    /**
     * handle receive new session is created at other node (create backup - primary false)
     * @param msg
     * @param sender
     * @throws IOException 
     * @throws ClassNotFoundException 
     */
    protected void handleSESSION_CREATED(ClusterDataMessage msg,Member sender) throws ClassNotFoundException, IOException {
		// counterReceive_EVT_SESSION_CREATED++;
		System.out.println("deltaManager.handleSESSION_CREATED: " + msg.getDataID());
		byte[] data = msg.getData();
		deserializeSessions(data);
    }
    
    /**
     * handle receive session delta
     * @param msg
     * @param sender
     * @throws IOException
     * @throws ClassNotFoundException
     */
    protected void handleSESSION_DELTA(ClusterDataMessage msg, Member sender) throws IOException, ClassNotFoundException {
    	//FIXME 存在数据一致性问题
    	//counterReceive_EVT_SESSION_DELTA++;
        byte[] data = msg.getData();
        Data curData = findData(msg.getDataID());
        if (curData != null) {
        	try{
        		curData.setValid(false);
	        	remove(curData);
	            System.out.println("deltaManager.handleSESSION_DELTA: " + msg.getDataID());
	            deserializeSessions(data);
        	}catch(Exception e){
        		System.out.println("deltaManager.handleSESSION_DELTA.deserializeSessions.err " + e);
        	}finally{
        		curData.setValid(true);
        		add(curData);
        	}
        }
    }
    
    /**
     * handle receive session state is complete transfered
     * @param msg
     * @param sender
     */
    protected void handleALL_SESSION_TRANSFERCOMPLETE(ClusterDataMessage msg, Member sender) {
//        counterReceive_EVT_ALL_SESSION_TRANSFERCOMPLETE++ ;
    	System.out.println("deltaManager.receiveMessage.transfercomplete "+getName()+" at: "+ sender.getHost()+":"+ Integer.valueOf(sender.getPort()));
//        stateTransferCreateSendTime = msg.getTimestamp() ;
        stateTransfered = true ;
    }
    
    /**
     * handle receive sessions from other not ( restart )
     * @param msg
     * @param sender
     * @throws ClassNotFoundException
     * @throws IOException
     */
    protected void handleALL_SESSION_DATA(ClusterDataMessage msg,Member sender) throws ClassNotFoundException, IOException {
//        counterReceive_EVT_ALL_SESSION_DATA++;
    	System.out.println("deltaManager.handleALL_SESSION_DATA.allSessionDataBegin " + getName());
        byte[] data = msg.getData();
        deserializeSessions(data);
        System.out.println("deltaManager.handleALL_SESSION_DATA.allSessionDataEnd " + getName());
    }
    
    /**
     * handle receive that other node want all sessions ( restart )
     * a) send all sessions with one message
     * b) send session at blocks
     * After sending send state is complete transfered
     * @param msg
     * @param sender
     * @throws IOException
     */
    protected void handleGET_ALL_SESSIONS(ClusterDataMessage msg, Member sender) throws IOException {
//        counterReceive_EVT_GET_ALL_SESSIONS++;
        //get a list of all the session from this manager
       System.out.println("deltaManager.handleGET_ALL_SESSIONS.unloadingBegin " + getName());
        // Write the number of active sessions, followed by the details
        // get all sessions and serialize without sync
        Data[] currentSessions = findDatas();
        long findSessionTimestamp = System.currentTimeMillis() ;
        if (isSendAllSessions()) {
            sendSessions(sender, currentSessions, findSessionTimestamp);
        } else {
            // send session at blocks
            int remain = currentSessions.length;
            for (int i = 0; i < currentSessions.length; i += getSendAllSessionsSize()) {
                int len = i + getSendAllSessionsSize() > currentSessions.length ? currentSessions.length - i : getSendAllSessionsSize();
                Data[] sendSessions = new Data[len];
                System.arraycopy(currentSessions, i, sendSessions, 0, len);
                sendSessions(sender, sendSessions,findSessionTimestamp);
                remain = remain - len;
                if (getSendAllSessionsWaitTime() > 0 && remain > 0) {
                    try {
                        Thread.sleep(getSendAllSessionsWaitTime());
                    } catch (Exception sleep) {
                    }
                }//end if
            }//for
        }//end if
        
        ClusterDataMessage newmsg = new ClusterDataMessageImpl(name,ClusterDataMessage.EVT_ALL_SESSION_TRANSFERCOMPLETE, null,"SESSION-STATE-TRANSFERED", "SESSION-STATE-TRANSFERED"+ getName());
        newmsg.setTimestamp(findSessionTimestamp);
        System.out.println("deltaManager.handleGET_ALL_SESSIONS.allSessionTransfered " + getName());
//        counterSend_EVT_ALL_SESSION_TRANSFERCOMPLETE++;
        cluster.send(newmsg, sender);
    }
    
    /**
     * send a block of session to sender
     * @param sender
     * @param currentSessions
     * @param sendTimestamp
     * @throws IOException
     */
    protected void sendSessions(Member sender, Data[] currentSessions,long sendTimestamp) throws IOException {
        byte[] data = serializeSessions(currentSessions);
        ClusterDataMessage newmsg = new ClusterDataMessageImpl(name,ClusterDataMessage.EVT_ALL_SESSION_DATA, data,"SESSION-STATE", "SESSION-STATE-" + getName());
        newmsg.setTimestamp(sendTimestamp);
//        counterSend_EVT_ALL_SESSION_DATA++;
        cluster.send(newmsg, sender);
    }
    
    /**
     * Save any currently active sessions in the appropriate persistence
     * mechanism, if any. If persistence is not supported, this method returns
     * without doing anything.
     * 
     * @exception IOException
     *                if an input/output error occurs
     */
    protected byte[] serializeSessions(Data[] currentSessions) throws IOException {

        // Open an output stream to the specified pathname, if any
        ByteArrayOutputStream fos = null;
        ObjectOutputStream oos = null;

        try {
            fos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(new BufferedOutputStream(fos));
            oos.writeObject(Integer.valueOf(currentSessions.length));
            for(int i=0 ; i < currentSessions.length;i++) {
            	oos.writeObject(currentSessions[i].getClass());//针对每一个metaData元素，先写入它的类型，以便读取的时候根据类型创建相应子类
                ((StandardData)currentSessions[i]).writeObjectData(oos);                
            }
            // Flush and close the output stream
            oos.flush();
        } catch (IOException e) {
            System.out.println("deltaManager.unloading.IOException: " + e);
            throw e;
        } finally {
            if (oos != null) {
                try {
                    oos.close();
                } catch (IOException f) {
                    // Ignore
                }
                oos = null;
            }
        }
        // send object data as byte[]
        return fos.toByteArray();
    }
    
    /**
    * Load sessions from other cluster node.
    * @exception ClassNotFoundException
    *                if a serialized class cannot be found during the reload
    * @exception IOException
    *                if an input/output error occurs
    */
   protected void deserializeSessions(byte[] data) throws ClassNotFoundException,IOException {

       // Initialize our internal data structures
       //sessions.clear(); //should not do this
       // Open an input stream to the specified pathname, if any
       ClassLoader originalLoader = Thread.currentThread().getContextClassLoader();
       ObjectInputStream ois = null;
       // Load the previously unloaded active sessions
       try {
           ois = getReplicationStream(data);
           Integer count = (Integer) ois.readObject();
           int n = count.intValue();
           for (int i = 0; i < n; i++) {
				Object dataType = ois.readObject();// 先获取metaData的类型
				// 根据类型创建相应子类的对象
				StandardData session = null;
				if (FileData.class == dataType) {
					session = new FileData(this);
				} else if (DirectoryData.class == dataType) {
					session = new DirectoryData(this);
				}
				session.readObjectData(ois);
				add(session);
           }
       } catch (ClassNotFoundException e) {
    	   System.out.println("deltaManager.loading.ClassNotFoundException " + e);
           throw e;
       } catch (IOException e) {
    	   System.out.println("deltaManager.loading.IOException" + e);
           throw e;
       } finally {
           // Close the input stream
           try {
               if (ois != null) ois.close();
           } catch (IOException f) {
               // ignored
           }
           ois = null;
           if (originalLoader != null) Thread.currentThread().setContextClassLoader(originalLoader);
       }

   }
    
    
    
    @Override
    public void messageDataReceived(ClusterMessage cmsg) {
    	 if (cmsg != null && cmsg instanceof ClusterDataMessage) {
             ClusterDataMessage msg = (ClusterDataMessage) cmsg;
             switch (msg.getEventType()) {
                 case ClusterDataMessage.EVT_GET_ALL_SESSIONS:
                 case ClusterDataMessage.EVT_SESSION_CREATED: 
                 case ClusterDataMessage.EVT_SESSION_EXPIRED: 
//                 case ClusterDataMessage.EVT_SESSION_ACCESSED:
                 case ClusterDataMessage.EVT_SESSION_DELTA:
//                 case ClusterDataMessage.EVT_CHANGE_SESSION_ID:
                 {
                     synchronized(receivedMessageQueue) {
                         if(receiverQueue) {
                             receivedMessageQueue.add(msg);
                             return ;
                         }
                     }
                    break;
                 }
                 default: {
                     //we didn't queue, do nothing
                     break;
                 }
             } //switch
             
             messageReceived(msg, msg.getAddress() != null ? (Member) msg.getAddress() : null);
         }
    }
    
	
	@Override
	public String[] getInvalidatedSessions() {
		// TODO Auto-generated method stub
		return null;
	}

	//---------------------------getters and setters
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}    
	
    /**
     * @return Returns the stateTransferTimeout.
     */
    public int getStateTransferTimeout() {
        return stateTransferTimeout;
    }
    /**
     * @param timeoutAllSession The timeout
     */
    public void setStateTransferTimeout(int timeoutAllSession) {
        this.stateTransferTimeout = timeoutAllSession;
    }

    /**
     * is session state transfered complete?
     * 
     */
    public boolean getStateTransfered() {
        return stateTransfered;
    }

    /**
     * set that state ist complete transfered  
     * @param stateTransfered
     */
    public void setStateTransfered(boolean stateTransfered) {
        this.stateTransfered = stateTransfered;
    }

	public boolean isSendAllSessions() {
		return sendAllSessions;
	}

	public void setSendAllSessions(boolean sendAllSessions) {
		this.sendAllSessions = sendAllSessions;
	}

	public int getSendAllSessionsSize() {
		return sendAllSessionsSize;
	}

	public void setSendAllSessionsSize(int sendAllSessionsSize) {
		this.sendAllSessionsSize = sendAllSessionsSize;
	}

	public int getSendAllSessionsWaitTime() {
		return sendAllSessionsWaitTime;
	}

	public void setSendAllSessionsWaitTime(int sendAllSessionsWaitTime) {
		this.sendAllSessionsWaitTime = sendAllSessionsWaitTime;
	}
}
