package com.dd.dfs.data;

import java.io.Serializable;

public class DataStore implements Serializable {
	
	private static final long serialVersionUID = 1L;
	/**
	 * ��ʵ�ļ���
	 */
	String physicalName = null;
	/**
	 * ���·��
	 */
	String physicalPath = null;
	/**
	 * �Ƿ���Ч
	 */
	boolean available = false;
	/**
	 * �������̨��������
	 */
	String server = null;
	
	public String getPhysicalName() {
		return physicalName;
	}
	public void setPhysicalName(String realName) {
		this.physicalName = realName;
	}

	public String getPhysicalPath() {
		return physicalPath;
	}
	public void setPhysicalPath(String physicalPath) {
		this.physicalPath = physicalPath;
	}
	
	public boolean isAvailable() {
		return available;
	}
	public void setAvailable(boolean available) {
		this.available = available;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DataStore other = (DataStore) obj;
		if (available != other.available)
			return false;
		if (physicalName == null) {
			if (other.physicalName != null)
				return false;
		} else if (!physicalName.equals(other.physicalName))
			return false;
		if (physicalPath == null) {
			if (other.physicalPath != null)
				return false;
		} else if (!physicalPath.equals(other.physicalPath))
			return false;
		return true;
	}
	
}
