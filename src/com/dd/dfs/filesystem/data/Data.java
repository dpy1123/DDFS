package com.dd.dfs.filesystem.data;

import com.dd.dfs.filesystem.data.manager.Manager;

/**
 * 文件系统的Data接口
 * @author dd
 *
 */
public interface Data {
	/**
	 * 获取元数据的id，用以从Manager中获取相应元数据对象
	 * 
	 * @return
	 */
	public String getId();
	public void setId(String id);

	public String getName();
	public String getPath();
	public String getParent();
	public void setValid(boolean isValid);
	public void setManager(Manager manager);

	/**
	 * Return the object bound with the specified name in this data, or
	 * <code>null</code> if no object is bound with that name.
	 * 
	 * @param name
	 *            Name of the attribute to be returned
	 * 
	 * @exception IllegalStateException
	 *                if this method is called on an invalidated data
	 */
	public Object getAttribute(String name);
}
