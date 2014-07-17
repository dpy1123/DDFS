package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * ����ܵ��еķ��Ľӿ�
 * @author DD
 *
 */
public interface ValveContext {
	
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
	 * ��ͣ������һ�����õ�Valve��invoke������ֱ�����õ�������
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws Exception
	 */
	public void invokeNext(HttpRequest request, HttpResponse response) throws IOException,Exception;
}
