package com.dd.dfs.data.manager;

import java.io.IOException;

import com.dd.dfs.core.container.Container;
import com.dd.dfs.data.Data;

/**
 * 管理文件系统的data对象
 * @author DD
 *
 */
public interface Manager {
	public Container getContainer();
	public void setContainer(Container container);
	public String getInfo();
	public void add(Data data);
	public Data findData(String id);
	public Data[] findDatas();
	public void remove(Data data);
	public void load() throws ClassNotFoundException, IOException;
	public void unload() throws IOException;
}
