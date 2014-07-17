package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * �ܵ��д�ִ�е�����--�� �Ľӿ�
 * @author DD
 *
 */
public interface Valve {

	/**
	 * ���ظ÷�����Ϣ
	 * @return
	 */
	public String getInfo();
	
	/**
	 * 
	 * @param request
	 * @param response
	 * @param context
	 * @throws IOException
	 * @throws ServletException
	 */
	public void invoke(HttpRequest request, HttpResponse response, ValveContext context) throws IOException,Exception;
}
