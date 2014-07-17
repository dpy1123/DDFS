package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * Container��Invokeʱ���õĹܵ�����ӿ�
 * @author DD
 *
 */
public interface PipeLine {
	
	/**
	 * ��ܵ������Ҫ���õķ�
	 * @param valve
	 */
	public void addValve(Valve valve);

	/**
	 * �Ƴ�ָ����
	 * @param valve
	 */
	public void removeValve(Valve valve);
	
	/**
	 * �õ�������ӽ��ܵ��ķ�
	 * @return
	 */
	public Valve[] getValves();
	
	/**
	 * ���û�����
	 * @param valve
	 */
	public void setBasic(Valve valve);
	
	/**
	 * �õ�������
	 * @return
	 */
	public Valve getBasic();
	
	/**
	 * Container���invoke����
	 * @param request
	 * @param response
	 */
	public void invoke(HttpRequest request, HttpResponse response) throws IOException,Exception;
}
