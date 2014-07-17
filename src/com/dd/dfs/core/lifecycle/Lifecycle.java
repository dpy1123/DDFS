package com.dd.dfs.core.lifecycle;

/**
 * �������ڽӿڣ�ʵ�ָýӿڵ������������ͳһ����/�ر�
 * @author DD
 *
 */
public interface Lifecycle {
	//�ýӿڿ��Դ�����6���¼�
	public static final String START_EVENT = "start";
	public static final String BEFORE_START_EVENT = "before_start";
	public static final String AFTER_START_EVENT = "after_start";
	public static final String STOP_EVENT = "stop";
	public static final String BEFORE_STOP_EVENT = "before_stop";
	public static final String AFTER_STOP_EVENT = "after_stop";
	
	/**
	 * ������������¼��ļ�����
	 * @param listener
	 */
	public void addLifecycleListener(LifecycleListener listener);
	
	public void removeLifecycleListener(LifecycleListener listener);
	
	public LifecycleListener[] findLifecycleListeners();
	
	/**
	 * ʵ�ָ�����������ʵ�ֱ����������丸������ã���ʵ�ֶ���Ŀ���������
	 */
	public void start();
	
	/**
	 * ʵ�ָ�����������ʵ�ֱ����������丸������ã���ʵ�ֶ���Ĺرղ�����
	 */
	public void stop();
}
