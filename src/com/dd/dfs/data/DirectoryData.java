package com.dd.dfs.data;

import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.utils.StringUtils;


/**
 * �ļ�ϵͳ���ļ�����<br>
 * attributes�����ļ����е����ݣ�������ʽ<code>[name, dataId]</code>
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
	 * �����ļ��ж���
	 * @param manager
	 * @param id ָ���ļ���id
	 * @param name
	 * @param path
	 * @param parent �����ļ��ж����id�����null��ʾ���ļ�����Root
	 */
	public DirectoryData(Manager manager, String id, String name, String path,
			String parent) {
		super(manager, id, name, path, parent);
		if(this.id == null)
			this.id = StringUtils.generateID("DirectoryData");
	}
	
	/**
	 * �жϸ��ļ����Ƿ����name��������ļ����ļ���
	 * @param name
	 * @return
	 */
	public boolean contains(String name){
		return attributes.containsKey(name);
	}
	
	/**
	 * ��ȡ���ļ����£�name����Ӧ�ļ����ļ��е�id
	 * @param name
	 * @return
	 */
	public String get(String name){
		if(name == null) return null;
		return (String) attributes.get(name);
	}

	/**
	 * ��ȡ���ļ����£�name����Ӧ�ļ����ļ���
	 * @param name
	 * @return
	 */
	public Data getData(String name){
		if(name == null) return null;
		String id = (String) attributes.get(name);
		return manager.findData(id);
	}
	
	/**
	 * �����ļ��е����ݣ�������ʽ<code>[name, dataId]</code>
	 * @param name
	 * @param dataId
	 * @return ����ԭ����name����Ӧ��dataId����null���nameû�ж�Ӧ��ֵ
	 */
	public String put(String name, String dataId){
		return (String) attributes.put(name, dataId);
	}
}
