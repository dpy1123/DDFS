package com.dd.dfs.ha.data;

import com.dd.dfs.ha.ClusterMessageBase;



/**
 * Session cluster message
 * 
 */
public class DataMessageImpl extends ClusterMessageBase implements DataMessage {
    
    private static final long serialVersionUID = 1L;

    public DataMessageImpl() {
    }
    
    /*
     * Private serializable variables to keep the messages state
     */
    private int mEvtType = -1;
    private byte[] mData;
    private String mDataID;

    private String mContainerName;
    private long serializationTimestamp;
    private boolean timestampSet = false ;
    private String uniqueId;


    /**
     * Creates a session message. Depending on what event type you want this
     * message to represent, you populate the different parameters in the constructor<BR>
      * The following rules apply dependent on what event type argument you use:<BR>
     * <B>EVT_SESSION_CREATED</B><BR>
     *    The parameters: session, sessionID must be set.<BR>
     * <B>EVT_SESSION_EXPIRED</B><BR>
     *    The parameters: sessionID must be set.<BR>
     * <B>EVT_SESSION_ACCESSED</B><BR>
     *    The parameters: sessionID must be set.<BR>
     * <B>EVT_GET_ALL_SESSIONS</B><BR>
     *    get all sessions from from one of the nodes.<BR>
     * <B>EVT_SESSION_DELTA</B><BR>
     *    Send attribute delta (add,update,remove attribute or principal, ...).<BR>
     * <B>EVT_ALL_SESSION_DATA</B><BR>
     *    Send complete serializes session list<BR>
     * <B>EVT_ALL_SESSION_TRANSFERCOMPLETE</B><BR>
     *    send that all session state information are transfered
     *    after GET_ALL_SESSION received from this sender.<BR>
     * <B>EVT_CHANGE_SESSION_ID</B><BR>
     *    send original sessionID and new sessionID.<BR>
     * <B>EVT_ALL_SESSION_NOCONTEXTMANAGER</B><BR>
     *    send that context manager does not exist
     *    after GET_ALL_SESSION received from this sender.<BR>
     * @param containerName - the name of the context (application
     * @param eventtype - one of the 8 event type defined in this class
     * @param data - the serialized byte array of the session itself
     * @param dataID - the id that identifies this session
     * @param uniqueID - the id that identifies this message
     */
	public DataMessageImpl(String containerName, int eventtype, byte[] data, String dataID, String uniqueID) {
		mContainerName = containerName;
		mEvtType = eventtype;
		mData = data;
		mDataID = dataID;
		uniqueId = uniqueID;
	}

    /**
     * returns the event type
     * @return one of the event types EVT_XXXX
     */
    @Override
    public int getEventType() { return mEvtType; }

    /**
     * @return the serialized data for the session
     */
    @Override
    public byte[] getData() { return mData;}

    /**
     * @return the session ID for the session
     */
    @Override
    public String getDataID(){ return mDataID; }
    
    /**
     * set message send time but only the first setting works (one shot)
     */
    @Override
    public void setTimestamp(long time) {
        synchronized(this) {
            if(!timestampSet) {
                serializationTimestamp=time;
                timestampSet = true ;
            }
        }
    }
    
    @Override
    public long getTimestamp() { return serializationTimestamp;}
    
    /**
     * clear text event type name (for logging purpose only) 
     * @return the event type in a string representation, useful for debugging
     */
    @Override
    public String getEventTypeString()
    {
        switch (mEvtType)
        {
            case EVT_SESSION_CREATED : return "SESSION-MODIFIED";
            case EVT_SESSION_EXPIRED : return "SESSION-EXPIRED";
            case EVT_SESSION_ACCESSED : return "SESSION-ACCESSED";
            case EVT_GET_ALL_SESSIONS : return "SESSION-GET-ALL";
            case EVT_SESSION_DELTA : return "SESSION-DELTA";
            case EVT_ALL_SESSION_DATA : return "ALL-SESSION-DATA";
            case EVT_ALL_SESSION_TRANSFERCOMPLETE : return "SESSION-STATE-TRANSFERED";
            case EVT_CHANGE_SESSION_ID : return "SESSION-ID-CHANGED";
            case EVT_ALL_SESSION_NOCONTEXTMANAGER : return "NO-CONTEXT-MANAGER";
            default : return "UNKNOWN-EVENT-TYPE";
        }
    }

    @Override
    public String getContainerName() {
       return mContainerName;
    }
    @Override
    public String getUniqueId() {
        return uniqueId;
    }
    @Override
    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }
    
    @Override
    public String toString() {
        return getEventTypeString() + "#" + getContainerName() + "#" + getDataID() ;
    }
}
