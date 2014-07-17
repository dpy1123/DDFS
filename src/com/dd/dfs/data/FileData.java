package com.dd.dfs.data;

import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.utils.StringUtils;


/**
 * 文件系统的文件类<br>
 * attributes中保存文件的存储对象数组，保存形式<code>["storage", DataStore[]]</code>
 * @author DD
 * 
 */
public class FileData extends StandardData {

	private static final long serialVersionUID = 1L;
	
	/**
	 * Set of attribute names which are not allowed to be persisted.
	 */
	protected static final String[] excludedAttributes = {  };
	
	public FileData(Manager manager) {
		super(manager);
	}
	
	/**
	 * 创建文件对象
	 * @param manager
	 * @param id 指定文件id
	 * @param name
	 * @param path
	 * @param parent 所属文件夹对象的id
	 */
	public FileData(Manager manager, String id, String name, String path,
			String parent) {
		super(manager, id, name, path, parent);
		if(this.id == null)
			this.id = StringUtils.generateID("FileData");
	}
	
	/**
	 * 新增存储对象
	 * @param newStore
	 */
	public synchronized void addDataStore(DataStore newStore) {
		if(newStore==null) return;
		DataStore[] dataStores = (DataStore[]) attributes.get("storage");
		if(dataStores == null)
			dataStores = new DataStore[0];
		DataStore[] newDataStores = new DataStore[dataStores.length+1];
		for (int i = 0; i < dataStores.length; i++) {
			newDataStores[i] = dataStores[i];
		}
		newDataStores[dataStores.length] = newStore;
		dataStores = newDataStores;
		attributes.put("storage", dataStores);
	}
	
	/**
	 * 移除存储对象
	 * @param oldStore
	 */
	public synchronized void removeDataStore(DataStore oldStore) {
		if(oldStore==null) return;
		DataStore[] dataStores = (DataStore[]) attributes.get("storage");
		if(dataStores == null)
			dataStores = new DataStore[0];
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
			attributes.put("storage", dataStores);
		}
	}
	
}
