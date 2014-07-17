package com.dd.dfs.data;

import java.io.Serializable;

public class DataStore implements Serializable {
	
	private static final long serialVersionUID = 1L;
	/**
	 * 真实文件名
	 */
	String physicalName = null;
	/**
	 * 存放路径
	 */
	String physicalPath = null;
	/**
	 * 是否有效
	 */
	boolean available = false;
	/**
	 * 存放在哪台服务器上
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
