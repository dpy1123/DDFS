package com.dd.dfs.filesystem.data;

import com.dd.dfs.filesystem.data.manager.Manager;

/**
 * �ļ�ϵͳ��Data�ӿ�
 * @author dd
 *
 */
public interface Data {
	/**
	 * ��ȡԪ���ݵ�id�����Դ�Manager�л�ȡ��ӦԪ���ݶ���
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
