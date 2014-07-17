package com.dd.dfs.ha.data;

import java.io.Externalizable;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import com.dd.dfs.data.StandardData;
import com.dd.dfs.data.manager.Manager;


public class DeltaData extends StandardData implements Externalizable{

	/**
	 * The delta request contains all the action info
	 * 
	 */
	private transient DeltaRequest deltaRequest = null;
	
	protected final Lock diffLock = new ReentrantReadWriteLock().writeLock();
    
	public DeltaData(Manager manager) {
		super(manager);
		// TODO Auto-generated constructor stub
	}

	public DeltaData(Manager manager, String id, String name, String path,
			String parent) {
		super(manager, id, name, path, parent);
	}
	
	public DeltaRequest getDeltaRequest() {
		if (deltaRequest == null)
			resetDeltaRequest();
		return deltaRequest;
	}
	
    public void resetDeltaRequest() {
        try {
            lock();
            if (deltaRequest == null) {
                deltaRequest = new DeltaRequest(this.getId(), false);
            } else {
                deltaRequest.reset();
                deltaRequest.setDeltaDataId(this.getId());
            }
        }finally{
            unlock();
        }
    }
    
    
    /**
     * Lock during serialization
     */
    public void lock() {
        diffLock.lock();
    }

    /**
     * Unlock after serialization
     */
    public void unlock() {
        diffLock.unlock();
    }

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		try {
			lock();
			writeObject(out);
		} finally {
			unlock();
		}
	}
	
	@Override
    protected void writeObject(ObjectOutputStream stream) throws IOException {
        writeObject((ObjectOutput)stream);
    }

	private void writeObject(ObjectOutput stream) throws IOException {
		/*
        // Write the scalar instance variables (except Manager)
        stream.writeObject(Long.valueOf(creationTime));
        stream.writeObject(Long.valueOf(lastAccessedTime));
        stream.writeObject(Integer.valueOf(maxInactiveInterval));
        stream.writeObject(Boolean.valueOf(isNew));
        stream.writeObject(Boolean.valueOf(isValid));
        stream.writeObject(Long.valueOf(thisAccessedTime));
        stream.writeObject(Long.valueOf(version));
        stream.writeBoolean(getPrincipal() != null);
        if (getPrincipal() != null) {
            SerializablePrincipal.writePrincipal((GenericPrincipal) principal,stream);
        }

        stream.writeObject(id);
        if (log.isDebugEnabled()) log.debug(sm.getString("deltaSession.writeSession", id));

        // Accumulate the names of serializable and non-serializable attributes
        String keys[] = keys();
        ArrayList<String> saveNames = new ArrayList<String>();
        ArrayList<Object> saveValues = new ArrayList<Object>();
        for (int i = 0; i < keys.length; i++) {
            Object value = null;
            value = attributes.get(keys[i]);
            if (value == null || exclude(keys[i]))
                continue;
            else if (value instanceof Serializable) {
                saveNames.add(keys[i]);
                saveValues.add(value);
            }
        }

        // Serialize the attribute count and the Serializable attributes
        int n = saveNames.size();
        stream.writeObject(Integer.valueOf(n));
        for (int i = 0; i < n; i++) {
            stream.writeObject( saveNames.get(i));
            try {
                stream.writeObject(saveValues.get(i));
            } catch (NotSerializableException e) {
                log.error(sm.getString("standardSession.notSerializable",saveNames.get(i), id), e);
                stream.writeObject(NOT_SERIALIZED);
                log.error("  storing attribute '" + saveNames.get(i)+ "' with value NOT_SERIALIZED");
            }
        }
*/
    }
	
	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}
}
