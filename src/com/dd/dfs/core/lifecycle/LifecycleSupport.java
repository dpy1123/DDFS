package com.dd.dfs.core.lifecycle;

/**
 * ����ʵ����Lifecycle�ӿڵ��������������ע��ļ���������������Ӧ�����������¼���</br>
 * �����ǹ����࣬���ֱ�ӳ��б���Ϳ���ʵ���������ڹ���
 * @author DD
 *
 */
public final class LifecycleSupport {

	private Lifecycle lifecycle = null;//�����������
	private LifecycleListener[] listeners = null;//���������ע������м�����
	
	public LifecycleSupport(Lifecycle lifecycle) {
		this.lifecycle = lifecycle;
		this.listeners = new LifecycleListener[0];
	}
	
	/**
	 * 
	 * @param lifecycle �������
	 * @param listeners �����ע������м�����
	 */
	public LifecycleSupport(Lifecycle lifecycle, LifecycleListener[] listeners) {
		this.lifecycle = lifecycle;
		this.listeners = listeners;
	}
	
	/**
	 * ��Ӽ�����
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
	 * �Ƴ����еļ�����ʵ��
	 * @param listener
	 */
	public synchronized void removeLifecycleListener(LifecycleListener listener){
		if(listener == null) return;
		
		int deleteIndex = -1;//��¼��ɾ����valve���±�
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
	 * �������й���ļ�����
	 * @return
	 */
	public LifecycleListener[] findLifecycleListeners(){
		return this.listeners;
	}
	
	/**
	 * �����¼�
	 * @param type �¼�����
	 * @param data �¼�����
	 */
	public void invoke(String type, Object data){
		//�����¼�����
		LifecycleEvent event = new LifecycleEvent(lifecycle, type, data);
		//�����еļ��������鸴��һ�ݣ���ִֹ�й����м������б仯[�̰߳�ȫ]
		LifecycleListener[] newListeners = null;
		synchronized (listeners) {
			newListeners = listeners.clone();
		}
		//֪ͨ����ע���������������lifecycleEventInvoke����
		for (int i = 0; i < newListeners.length; i++) {
			newListeners[i].lifecycleEventInvoke(event);
		}
	}
}
