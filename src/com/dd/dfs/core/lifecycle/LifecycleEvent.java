package com.dd.dfs.core.lifecycle;

import java.util.EventObject;

/**
 * 生命周期事件
 * @author DD
 *
 */
public final class LifecycleEvent extends EventObject{

	private static final long serialVersionUID = 1L;
	
	private Object data = null;//事件数据
	private Lifecycle lifecycle = null;//事件发送源
	private String type = null;//事件类型
	
	public LifecycleEvent(Lifecycle lifecycle, String type) {
		this(lifecycle, type, null);
	}
	
	public LifecycleEvent(Lifecycle lifecycle, String type, Object data) {
		super(lifecycle);
		this.lifecycle = lifecycle;
		this.type = type;
		this.data = data;
	}

	public Object getData() {
		return data;
	}

	/**
	 * 得到事件发生源
	 * @return
	 */
	public Lifecycle getLifecycle() {
		return lifecycle;
	}

	public String getType() {
		return type;
	}
	
}
