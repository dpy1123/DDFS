package com.dd.dfs.filesystem.entity;

import java.util.HashMap;

/**
 * DFS�е��ļ�����
 * @author dd
 *
 */
public class DirectoryData implements IData {
	
	/**
	 * �����ļ����е�����
	 */
	protected HashMap<String, IData> contents = new HashMap<String, IData>();
	
	/**
	 * ���ļ��е�����
	 */
	protected String name = null;
	/**
	 * �ϼ�Ŀ¼
	 */
	protected DirectoryData parent = null;
	
	public DirectoryData(String name, DirectoryData parent) {
		this.name = name;
		this.parent = parent;
	}
	
	public boolean contains(String name){
		return contents.containsKey(name);
	}
	
	public IData get(String name){
		return contents.get(name);
	}
	
	public IData put(String name, IData value){
		return contents.put(name, value);
	}

	public HashMap<String, IData> getContents() {
		return contents;
	}

	public void setContents(HashMap<String, IData> contents) {
		this.contents = contents;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DirectoryData getParent() {
		return parent;
	}

	public void setParent(DirectoryData parent) {
		this.parent = parent;
	}
	
}
