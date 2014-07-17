package com.dd.dfs.data.manager;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;
import com.dd.dfs.data.Data;

/**
 * 本类是实现了Manager接口的抽象类
 * @author DD
 *
 */
public abstract class ManagerBase implements Manager, Lifecycle {
	
	/**
	 * The Container with which this Manager is associated.
	 */
	protected Container container;
	
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);//生命周期管理工具
	
	/**
	 * The descriptive information string for this implementation.
	 */
	protected static final String info = "ManagerBase/1.0";

	 /**
     * The set of currently active metaDatas for this Manager, keyed by identifier.
     */
    protected Map<String, Data> metaDatas = new ConcurrentHashMap<String, Data>();

	@Override
	public Container getContainer() {
		return container;
	}

	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public String getInfo() {
		return info;
	}

	@Override
	public void add(Data data) {
		metaDatas.put(data.getId(), data);
	}

	@Override
	public Data findData(String id) {
		if (id == null)
			return (null);
		return metaDatas.get(id);
	}

	@Override
	public Data[] findDatas() {
		return metaDatas.values().toArray(new Data[0]);
	}

	@Override
	public void remove(Data data) {
		metaDatas.remove(data.getId());
	}
	
	//=============以下是Lifecycle接口相关函数=============
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}
}
