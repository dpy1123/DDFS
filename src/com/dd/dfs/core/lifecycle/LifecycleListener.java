package com.dd.dfs.core.lifecycle;

/**
 * �������ڷ����ļ������ӿ�
 * @author DD
 *
 */
public interface LifecycleListener {
	
	/**
	 * ִ�����������¼�.</br>
	 * ��ʵ�����У����ݴ���event��type�������ǲ���ʵ��������ע���¼�.
	 * @param event
	 */
	public void lifecycleEventInvoke(LifecycleEvent event);
}
