package com.dd.dfs.core.lifecycle;

/**
 * 生命周期接口，实现该接口的类可以由容器统一启动/关闭
 * @author DD
 *
 */
public interface Lifecycle {
	//该接口可以触发的6个事件
	public static final String START_EVENT = "start";
	public static final String BEFORE_START_EVENT = "before_start";
	public static final String AFTER_START_EVENT = "after_start";
	public static final String STOP_EVENT = "stop";
	public static final String BEFORE_STOP_EVENT = "before_stop";
	public static final String AFTER_STOP_EVENT = "after_stop";
	
	/**
	 * 添加生命周期事件的监听器
	 * @param listener
	 */
	public void addLifecycleListener(LifecycleListener listener);
	
	public void removeLifecycleListener(LifecycleListener listener);
	
	public LifecycleListener[] findLifecycleListeners();
	
	/**
	 * 实现给类的组件必须实现本方法，供其父组件调用，已实现对其的开启操作。
	 */
	public void start();
	
	/**
	 * 实现给类的组件必须实现本方法，供其父组件调用，已实现对其的关闭操作。
	 */
	public void stop();
}
