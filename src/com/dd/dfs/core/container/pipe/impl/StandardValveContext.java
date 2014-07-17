package com.dd.dfs.core.container.pipe.impl;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.container.pipe.inter.Valve;
import com.dd.dfs.core.container.pipe.inter.ValveContext;


/**
 * ������ValveContext�ӿڵı�־ʵ��
 * @author DD
 *
 */
public class StandardValveContext implements ValveContext{

	Valve[] valves = null;//���д����õķ�
	int invokeIndex = 0;//���õ��ķ����±�
	Valve basic = null;//������
	
	public StandardValveContext() {
		valves = new Valve[0];
	}
	
	public StandardValveContext(Valve[] valves, Valve basic) {
		this.valves = valves;
		this.basic = basic;
	}
	
	@Override
	public void invokeNext(HttpRequest request, HttpResponse response)
			throws IOException, Exception {
		int cur = invokeIndex;
		invokeIndex = invokeIndex + 1;
		System.out.println("-------cur invoke: "+cur);//--------------->>
		System.out.println("-------valves.length: "+valves.length);//--------------->>
		if(cur < valves.length){
			valves[cur].invoke(request, response, this);
		}else if(cur == valves.length && basic != null){
			basic.invoke(request, response, this);
			invokeIndex = 0;//��ֻ�ǵ���һ�Σ���������Ҫ����һ��
		}else{
			//log("pipeLine no valve");
		}
		
	}

	@Override
	public synchronized void addValve(Valve valve) {
		if(valve == null) return;
	
		Valve[] newValves = new Valve[valves.length+1];
		for (int i = 0; i < valves.length; i++) {
			newValves[i] = valves[i];
		}
		newValves[valves.length] = valve;
		valves = newValves;
	}

	@Override
	public synchronized void removeValve(Valve valve) {
		if(valve == null) return;
		
		int deleteIndex = -1;//��¼��ɾ����valve���±�
		for (int i = 0; i < valves.length; i++) {
			if(valve.getInfo().equals(valves[i].getInfo())){
				deleteIndex = i;
			}
		}
		if(deleteIndex > 0){
			Valve[] newValves = new Valve[valves.length-1];
			for (int i = 0; i < deleteIndex; i++) {
				newValves[i] = valves[i];
			}
			for (int i = deleteIndex + 1; i < valves.length; i++) {
				newValves[i] = valves[i];
			}
			valves = newValves;
		}
	}

	@Override
	public Valve[] getValves() {
		return this.valves;
	}

	@Override
	public void setBasic(Valve valve) {
		this.basic = valve;
	}

	@Override
	public Valve getBasic() {
		return this.basic;
	}

}
