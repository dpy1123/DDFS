package com.dd.dfs.filesystem.entity;

import java.util.HashMap;

/**
 * DFS中的文件夹类
 * @author dd
 *
 */
public class DirectoryData implements IData {
	
	/**
	 * 保存文件夹中的内容
	 */
	protected HashMap<String, IData> contents = new HashMap<String, IData>();
	
	/**
	 * 本文件夹的名字
	 */
	protected String name = null;
	/**
	 * 上级目录
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
