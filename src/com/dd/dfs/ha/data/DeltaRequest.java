package com.dd.dfs.ha.data;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.LinkedList;




public class DeltaRequest implements Externalizable{
	
	private String deltaDataId;
	private boolean recordAllActions = false;

    private LinkedList<AttributeInfo> actions = new LinkedList<AttributeInfo>();
    private LinkedList<AttributeInfo> actionPool = new LinkedList<AttributeInfo>();
    
	public DeltaRequest() {

	}

	public DeltaRequest(String deltaDataId, boolean recordAllActions) {
		this.recordAllActions = recordAllActions;
		if (deltaDataId != null)
			setDeltaDataId(deltaDataId);
	}
	
	public int getSize() {
		return actions.size();
	}
	   
	public void reset() {
		while (actions.size() > 0) {
			try {
				AttributeInfo info = actions.removeFirst();
				info.recycle();
				actionPool.addLast(info);
			} catch (Exception x) {
				System.out.println("Unable to remove element" + x);
			}
		}
		actions.clear();
	}
	
    /**
     * serialize DeltaRequest
     * @see DeltaRequest#writeExternal(java.io.ObjectOutput)
     * 
     * @return serialized delta request
     * @throws IOException
     */
    protected byte[] serialize() throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        writeExternal(oos);
        oos.flush();
        oos.close();
        return bos.toByteArray();
    }
    
	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		//sessionId - String
        //recordAll - boolean
        //size - int
        //AttributeInfo - in an array
        out.writeUTF(deltaDataId);
        out.writeBoolean(recordAllActions);
        out.writeInt(getSize());
        for ( int i=0; i<getSize(); i++ ) {
            AttributeInfo info = actions.get(i);
            info.writeExternal(out);
        }
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException,
			ClassNotFoundException {
		// TODO Auto-generated method stub
		
	}

	public void setDeltaDataId(String deltaDataId) {
		this.deltaDataId = deltaDataId;
	}

	
	private static class AttributeInfo implements java.io.Externalizable {
        private String name = null;
        private Object value = null;
        private int action;
        private int type;

        public AttributeInfo() {
            this(-1, -1, null, null);
        }

        public AttributeInfo(int type,
                             int action,
                             String name,
                             Object value) {
            super();
            init(type,action,name,value);
        }

        public void init(int type,
                         int action,
                         String name,
                         Object value) {
            this.name = name;
            this.value = value;
            this.action = action;
            this.type = type;
        }

        public int getType() {
            return type;
        }

        public int getAction() {
            return action;
        }

        public Object getValue() {
            return value;
        }
        @Override
        public int hashCode() {
            return name.hashCode();
        }

        public String getName() {
            return name;
        }
        
        public void recycle() {
            name = null;
            value = null;
            type=-1;
            action=-1;
        }

        @Override
        public boolean equals(Object o) {
            if ( ! (o instanceof AttributeInfo ) ) return false;
            AttributeInfo other =  (AttributeInfo)o;
            return other.getName().equals(this.getName());
        }
        
        @Override
        public void readExternal(java.io.ObjectInput in ) throws IOException,ClassNotFoundException {
            //type - int
            //action - int
            //name - String
            //hasvalue - boolean
            //value - object
            type = in.readInt();
            action = in.readInt();
            name = in.readUTF();
            boolean hasValue = in.readBoolean();
            if ( hasValue ) value = in.readObject();
        }

        @Override
        public void writeExternal(java.io.ObjectOutput out) throws IOException {
            //type - int
            //action - int
            //name - String
            //hasvalue - boolean
            //value - object
            out.writeInt(getType());
            out.writeInt(getAction());
            out.writeUTF(getName());
            out.writeBoolean(getValue()!=null);
            if (getValue()!=null) out.writeObject(getValue());
        }
        
        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder("AttributeInfo[type=");
            buf.append(getType()).append(", action=").append(getAction());
            buf.append(", name=").append(getName()).append(", value=").append(getValue());
            buf.append(", addr=").append(super.toString()).append("]");
            return buf.toString();
        }

    }
}
