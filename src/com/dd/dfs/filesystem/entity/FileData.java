package com.dd.dfs.filesystem.entity;

import java.io.File;

public class FileData implements IData {
	/**
	 * 文件名
	 */
	String name = null;
	
	/**
	 * 上级目录
	 */
	DirectoryData parent = null;
	
	/**
	 * 
	 */
	DataStore[] dataStores = null;
	
	public FileData(String name, DirectoryData parent) {
		this.name = name;
		this.parent = parent;
		this.dataStores = new DataStore[0];
	}
	
	public String getPath(){
		String path = name;
		DirectoryData dir = parent;
		while(dir!=null) {
			path = dir.getName()+File.separator+path;
			dir = dir.getParent();
		}
		return path;
	}

	public synchronized void addDataStore(DataStore newStore) {
		if(newStore==null) return;
		DataStore[] newDataStores = new DataStore[dataStores.length+1];
		for (int i = 0; i < dataStores.length; i++) {
			newDataStores[i] = dataStores[i];
		}
		newDataStores[dataStores.length] = newStore;
		dataStores = newDataStores;
	}
	public synchronized void removeDataStore(DataStore oldStore) {
		if(oldStore==null) return;
		int deleteIndex = -1;//记录待删除的valve的下标
		for (int i = 0; i < dataStores.length; i++) {
			if(oldStore.equals(dataStores[i])){
				deleteIndex = i;
			}
		}
		if(deleteIndex > 0){
			DataStore[] newDataStores = new DataStore[dataStores.length-1];
			for (int i = 0; i < deleteIndex; i++) {
				newDataStores[i] = dataStores[i];
			}
			for (int i = deleteIndex + 1; i < dataStores.length; i++) {
				newDataStores[i] = dataStores[i];
			}
			dataStores = newDataStores;
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public DataStore[] getDataStores() {
		return dataStores;
	}
	
	public DirectoryData getParent() {
		return parent;
	}
	public void setParent(DirectoryData parent) {
		this.parent = parent;
	}
	
}
