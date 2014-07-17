package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * 管道中待执行的任务--阀 的接口
 * @author DD
 *
 */
public interface Valve {

	/**
	 * 返回该阀的信息
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
