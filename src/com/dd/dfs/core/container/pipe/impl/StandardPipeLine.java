package com.dd.dfs.core.container.pipe.impl;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.container.pipe.inter.PipeLine;
import com.dd.dfs.core.container.pipe.inter.Valve;
import com.dd.dfs.core.container.pipe.inter.ValveContext;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;


/**
 * 本类是PipeLine接口的标准实现
 * @author DD
 *
 */
public class StandardPipeLine implements PipeLine, Lifecycle {

	ValveContext valveContext = null;
	Container container = null;
	LifecycleSupport lifecycle = null;//生命周期管理工具
	
	public StandardPipeLine(Container container) {
		this.container = container;
		this.valveContext = new StandardValveContext();
		lifecycle = new LifecycleSupport(this);
	}
	
	public StandardPipeLine(Container container, Valve[] valves, Valve basic) {
		this.container = container;
		this.valveContext = new StandardValveContext(valves, basic);
	}

	@Override
	public void invoke(HttpRequest request, HttpResponse response)
			throws IOException, Exception {
		valveContext.invokeNext(request, response);
	}

	@Override
	public void addValve(Valve valve) {
		valveContext.addValve(valve);
	}

	@Override
	public void removeValve(Valve valve) {
		valveContext.removeValve(valve);
	}

	@Override
	public Valve[] getValves() {
		return valveContext.getValves();
	}

	@Override
	public void setBasic(Valve valve) {
		valveContext.setBasic(valve);
	}

	@Override
	public Valve getBasic() {
		return valveContext.getBasic();
	}

	//=============以下是Lifecycle接口相关函数=============
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}

	@Override
	public void start() {
		System.out.println("["+container.getInfo()+"] PipeLine Start!");
	}

	@Override
	public void stop() {
		System.out.println("["+container.getInfo()+"] PipeLine Stop!");
	}

}
