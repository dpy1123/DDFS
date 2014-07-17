package com.dd.dfs.core.lifecycle;

/**
 * 帮助实现了Lifecycle接口的组件：管理其下注册的监听器，并触发相应的生命周期事件。</br>
 * 本类是工具类，组件直接持有本类就可以实现生命周期管理。
 * @author DD
 *
 */
public final class LifecycleSupport {

	private Lifecycle lifecycle = null;//保存所属组件
	private LifecycleListener[] listeners = null;//管理组件下注册的所有监听器
	
	public LifecycleSupport(Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
		this.listeners = new LifecycleListener[0];
	}
	
	/**
	 * 
	 * @param lifecycle 所属组件
	 * @param listeners 组件下注册的所有监听器
	 */
	public LifecycleSupport(Lifecycle lifecycle, LifecycleListener[] listeners) {
		this.lifecycle = lifecycle;
		this.listeners = listeners;
	}
	
	/**
	 * 添加监听器
	 * @param listener
	 */
	public synchronized void addLifecycleListener(LifecycleListener listener){
		if(listener == null) return;
		
		LifecycleListener[] newListeners = new LifecycleListener[listeners.length+1];
		for (int i = 0; i < listeners.length; i++) {
			newListeners[i] = listeners[i];
		}
		newListeners[listeners.length] = listener;
		listeners = newListeners;
	}
	
	/**
	 * 移除已有的监听器实例
	 * @param listener
	 */
	public synchronized void removeLifecycleListener(LifecycleListener listener){
		if(listener == null) return;
		
		int deleteIndex = -1;//记录待删除的valve的下标
		for (int i = 0; i < listeners.length; i++) {
			if(listener == listeners[i]){
				deleteIndex = i;
			}
		}
		if(deleteIndex > 0){
			LifecycleListener[] newListeners = new LifecycleListener[listeners.length-1];
			for (int i = 0; i < deleteIndex; i++) {
				newListeners[i] = listeners[i];
			}
			for (int i = deleteIndex + 1; i < listeners.length; i++) {
				newListeners[i] = listeners[i];
			}
			listeners = newListeners;
		}
	}
	
	/**
	 * 返回所有管理的监听器
	 * @return
	 */
	public LifecycleListener[] findLifecycleListeners(){
		return this.listeners;
	}
	
	/**
	 * 触发事件
	 * @param type 事件类型
	 * @param data 事件数据
	 */
	public void invoke(String type, Object data){
		//生成事件对象
		LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
		//将现有的监听器数组复制一份，防止执行过程中监听器有变化[线程安全]
		LifecycleListener[] newListeners = null;
		synchronized (listeners) {
			newListeners = listeners.clone();
		}
		//通知所有注册监听器，调用其lifecycleEventInvoke方法
		for (int i = 0; i < newListeners.length; i++) {
			newListeners[i].lifecycleEventInvoke(event);
		}
	}
}
