package com.dd.dfs.core.container.pipe.inter;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;

/**
 * Container类Invoke时调用的管道任务接口
 * @author DD
 *
 */
public interface PipeLine {
	
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
	 * Container类的invoke方法
	 * @param request
	 * @param response
	 */
	public void invoke(HttpRequest request, HttpResponse response) throws IOException,Exception;
}
