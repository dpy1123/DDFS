package com.dd.dfs.data;

import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.utils.StringUtils;


/**
 * 文件系统的文件夹类<br>
 * attributes保存文件夹中的内容，保存形式<code>[name, dataId]</code>
 * @author DD
 * 
 */
public class DirectoryData extends StandardData {

	private static final long serialVersionUID = 1L;

	/**
	 * Set of attribute names which are not allowed to be persisted.
	 */
	protected static final String[] excludedAttributes = {  };
	
	public DirectoryData(Manager manager) {
		super(manager);
	}
	
	/**
	 * 创建文件夹对象
	 * @param manager
	 * @param id 指定文件夹id
	 * @param name
	 * @param path
	 * @param parent 所属文件夹对象的id，如果null表示本文件夹是Root
	 */
	public DirectoryData(Manager manager, String id, String name, String path,
			String parent) {
		super(manager, id, name, path, parent);
		if(this.id == null)
			this.id = StringUtils.generateID("DirectoryData");
	}
	
	/**
	 * 判断该文件夹是否包含name所代表的文件或文件夹
	 * @param name
	 * @return
	 */
	public boolean contains(String name){
		return attributes.containsKey(name);
	}
	
	/**
	 * 获取本文件夹下，name所对应文件或文件夹的id
	 * @param name
	 * @return
	 */
	public String get(String name){
		if(name == null) return null;
		return (String) attributes.get(name);
	}

	/**
	 * 获取本文件夹下，name所对应文件或文件夹
	 * @param name
	 * @return
	 */
	public Data getData(String name){
		if(name == null) return null;
		String id = (String) attributes.get(name);
		return manager.findData(id);
	}
	
	/**
	 * 新增文件夹的内容，保存形式<code>[name, dataId]</code>
	 * @param name
	 * @param dataId
	 * @return 返回原来的name所对应的dataId，或null如果name没有对应的值
	 */
	public String put(String name, String dataId){
		return (String) attributes.put(name, dataId);
	}
}
