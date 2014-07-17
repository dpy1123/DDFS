package com.dd.dfs.filesystem.data;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dd.dfs.filesystem.data.manager.Manager;


/**
 * 实现Data接口的基础类
 * 
 * @author DD
 * 
 */
public class StandardData implements Data, Serializable{

	private static final long serialVersionUID = 1L;
	/**
	 * The Manager with which this Data is associated.
	 */
	protected transient Manager manager = null;
	protected String id = null;
	protected String name = null;
	protected String path = null;
	protected String parent = null;
	protected boolean isValid = true;

	/**
	 * The dummy attribute value serialized when a NotSerializableException is
	 * encountered in <code>writeObject()</code>.
	 */
	protected static final String NOT_SERIALIZED = "___NOT_SERIALIZABLE_EXCEPTION___";
	/**
	 * Type array.
	 */
	protected static final String EMPTY_ARRAY[] = new String[0];
	/**
	 * The collection of user data attributes associated with this Session.
	 */
	protected Map<String, Object> attributes = new ConcurrentHashMap<String, Object>();


	/**
	 * Set of attribute names which are not allowed to be persisted.
	 */
	protected static final String[] excludedAttributes = {  };
	
	public StandardData(Manager manager) {
		this.manager = manager;
	}
	
	public StandardData(Manager manager, String id, String name, String path,
			String parent) {
		this.manager = manager;
		this.id = id;
		this.name = name;
		this.path = path;
		this.parent = parent;
	}
	
	@Override
	public String getId() {
		return this.id;
	}

	@Override
	public void setId(String id) {
		this.id = id;
	}
	
	@Override
	public void setValid(boolean isValid) {
		this.isValid = isValid;
	}

    /**
     * Read a serialized version of the contents of this data object from
     * the specified object input stream, without requiring that the
     * StandardData itself have been serialized.
     *
     * @param stream The object input stream to read from
     *
     * @exception ClassNotFoundException if an unknown class is specified
     * @exception IOException if an input/output error occurs
     */
    public void readObjectData(ObjectInputStream stream)
        throws ClassNotFoundException, IOException {

        readObject(stream);

    }

    /**
     * Read a serialized version of this data object from the specified
     * object input stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The reference to the owning Manager
     * is not restored by this method, and must be set explicitly.
     *
     * @param stream The input stream to read from
     *
     * @exception ClassNotFoundException if an unknown class is specified
     * @exception IOException if an input/output error occurs
     */
    protected void readObject(ObjectInputStream stream)
        throws ClassNotFoundException, IOException {

        // Deserialize the scalar instance variables (except Manager)
        id = (String) stream.readObject();
        name = (String) stream.readObject();
        path = (String) stream.readObject();
        parent = (String) stream.readObject();
        isValid = ((Boolean) stream.readObject()).booleanValue();
        System.out.println("readObject() loading data " + id);

        // Deserialize the attribute count and attribute values
        if (attributes == null)
            attributes = new ConcurrentHashMap<String, Object>();
        int n = ((Integer) stream.readObject()).intValue();
        boolean isValidSave = isValid;
        isValid = true;
        for (int i = 0; i < n; i++) {
            String name = (String) stream.readObject();
            Object value = stream.readObject();
            if ((value instanceof String) && (value.equals(NOT_SERIALIZED)))
                continue;
            System.out.println("  loading attribute '" + name +
                    "' with value '" + value + "'");
            attributes.put(name, value);
        }
        isValid = isValidSave;

    }

    /**
     * Write a serialized version of the contents of this data object to
     * the specified object output stream, without requiring that the
     * StandardData itself have been serialized.
     *
     * @param stream The object output stream to write to
     *
     * @exception IOException if an input/output error occurs
     */
    public void writeObjectData(ObjectOutputStream stream)
        throws IOException {

        writeObject(stream);

    }
    
    /**
     * Write a serialized version of this data object to the specified
     * object output stream.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  The owning Manager will not be stored
     * in the serialized representation of this StandardData.  After calling
     * <code>readObject()</code>, you must set the associated Manager
     * explicitly.
     * <p>
     * <b>IMPLEMENTATION NOTE</b>:  Any attribute that is not Serializable
     * will be unbound from the StandardData, with appropriate actions if it
     * implements HttpSessionBindingListener. 
     *
     * @param stream The output stream to write to
     *
     * @exception IOException if an input/output error occurs
     */
    protected void writeObject(ObjectOutputStream stream) throws IOException {

        // Write the scalar instance variables (except Manager)
    	stream.writeObject(id);
        stream.writeObject(name);
        stream.writeObject(path);
        stream.writeObject(parent);
        stream.writeObject(Boolean.valueOf(isValid));
        System.out.println("writeObject() storing data " + id);

        // Accumulate the names of serializable and non-serializable attributes
        String keys[] = keys();
        ArrayList<String> saveNames = new ArrayList<String>();
        ArrayList<Object> saveValues = new ArrayList<Object>();
        for (int i = 0; i < keys.length; i++) {
            Object value = attributes.get(keys[i]);
            if (value == null)
                continue;
            else if ( (value instanceof Serializable) && (!exclude(keys[i]) )) {
                saveNames.add(keys[i]);
                saveValues.add(value);
            } else {
                // Remove this attribute from our collection
                attributes.remove(keys[i]);
            }
        }

        // Serialize the attribute count and the Serializable attributes
        int n = saveNames.size();
        stream.writeObject(Integer.valueOf(n));
        for (int i = 0; i < n; i++) {
            stream.writeObject(saveNames.get(i));
            try {
                stream.writeObject(saveValues.get(i));
                System.out.println("  storing attribute '" + saveNames.get(i) +
                        "' with value '" + saveValues.get(i) + "'");
            } catch (NotSerializableException e) {
            	System.out.println("standardSession.notSerializable: attribute '"+
                     saveNames.get(i)+"' " + e.getMessage());
                stream.writeObject(NOT_SERIALIZED);
                System.out.println("  storing attribute '" + saveNames.get(i) +
                        "' with value NOT_SERIALIZED");
            }
        }

    }

    /**
     * Return the names of all currently defined data attributes
     * as an array of Strings.  If there are no defined attributes, a
     * zero-length array is returned.
     */
    public String[] keys() {
		return attributes.keySet().toArray(EMPTY_ARRAY);
	}

	/**
	 * Exclude standard attributes that cannot be serialized.
	 * 
	 * @param name the attribute's name
	 */
	protected boolean exclude(String name) {
		for (int i = 0; i < excludedAttributes.length; i++) {
			if (name.equalsIgnoreCase(excludedAttributes[i]))
				return true;
		}
		return false;
	}
    
	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public boolean isValid() {
		return isValid;
	}

	@Override
	public Object getAttribute(String name) {
		if (!isValid)
			throw new IllegalStateException("standardData.getAttribute.ise: data invalidated");

		if (name == null)
			return null;

		return (attributes.get(name));
	}

}
