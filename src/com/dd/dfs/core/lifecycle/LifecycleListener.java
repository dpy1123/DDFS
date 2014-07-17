package com.dd.dfs.core.lifecycle;

/**
 * 声明周期方法的监听器接口
 * @author DD
 *
 */
public interface LifecycleListener {
	
	/**
	 * 执行生命周期事件.</br>
	 * 在实现类中，根据传入event的type来决定是不是实现类所关注的事件.
	 * @param event
	 */
	public void lifecycleEventInvoke(LifecycleEvent event);
}
