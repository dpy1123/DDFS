package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * 管理管道中的阀的接口
 * @author DD
 *
 */
public interface ValveContext {
	
	/**
	 * 向管道中添加要调用的阀
	 * @param valve
	 */
	public void addValve(Valve valve);

	/**
	 * 移除指定阀
	 * @param valve
	 */
	public void removeValve(Valve valve);
	
	/**
	 * 得到所有添加进管道的阀
	 * @return
	 */
	public Valve[] getValves();
	
	/**
	 * 设置基础阀
	 * @param valve
	 */
	public void setBasic(Valve valve);
	
	/**
	 * 得到基础阀
	 * @return
	 */
	public Valve getBasic();
	
	/**
	 * 不停调用下一个可用的Valve的invoke方法，直到调用到基础阀
	 * @param request
	 * @param response
	 * @throws IOException
	 * @throws Exception
	 */
	public void invokeNext(HttpRequest request, HttpResponse response) throws IOException,Exception;
}
