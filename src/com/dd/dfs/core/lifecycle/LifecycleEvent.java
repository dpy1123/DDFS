package com.dd.dfs.core.lifecycle;

import java.util.EventObject;

/**
 * ���������¼�
 * @author DD
 *
 */
public final class LifecycleEvent extends EventObject{

	private static final long serialVersionUID = 1L;
	
	private Object data = null;//�¼�����
	private Lifecycle lifecycle = null;//�¼�����Դ
	private String type = null;//�¼�����
	
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
	 * �õ��¼�����Դ
	 * @return
	 */
	public Lifecycle getLifecycle() {
		return lifecycle;
	}

	public String getType() {
		return type;
	}
	
}
