package com.dd.dfs.core;

import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.container.ContainerBase;
import com.dd.dfs.core.container.pipe.impl.StandardPipeLine;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.data.manager.StandardManager;
import com.dd.dfs.ha.Cluster;
import com.dd.dfs.ha.data.DeltaManager;



public class StandardDFSContainer extends ContainerBase {

	private String info = "StandardDFSContainer v1.0";
	
	private boolean started = false;//记录本容器是否启动
	private Manager manager = null;//元数据管理器
	private Cluster cluster = null;

	public StandardDFSContainer() {
		pipeLine = new StandardPipeLine(this);
		pipeLine.setBasic(new StandardFileOpsValve(this));
	}
	
	@Override
	public Container getParent() {
		return null;
	}

	@Override
	public void setParent(Container container) {
		return;
	}
	
	@Override
	public void addChild(Container child) {
		return;
	}

	@Override
	public void removeChild(Container child) {
		return;
	}

	@Override
	public Container findChild(String name) {
		return null;
	}

	@Override
	public Container[] findChildren() {
		return null;
	}

	@Override
	public String getInfo() {
		return this.info;
	}

	@Override
	public void setInfo(String info) {
		this.info = info;
	}



	@Override
	public synchronized void start() {
		System.out.println("["+this.getInfo()+"] Start!");
		if(started){//已被启动
			return;
		}
		//触发事件：BEFORE_START_EVENT
		this.lifecycle.invoke(BEFORE_START_EVENT, null);
		
		//开始启动
		started = true;
		
		//如果有集群，启动集群
		if(cluster != null && cluster instanceof Lifecycle)
			((Lifecycle) cluster).start();
		
		//如果有子容器，启动子容器
		Container[] children = findChildren();
		if(children != null){
			for (int i = 0; i < children.length; i++) {
				if(children[i] instanceof Lifecycle)
					((Lifecycle) children[i]).start();
			}
		}
		
		//初始化管道任务
		if(pipeLine instanceof Lifecycle)
			((Lifecycle) pipeLine).start();
		
		//创建manager
		Manager containerManager = null;
		if (manager == null) {//如果manager没有设置
			if ((getCluster() != null)) {
				containerManager = new DeltaManager();//集群默认使用DeltaManager
			} else {
				containerManager = new StandardManager();//非集群默认使用StandardManager
			}
		}
		//Configure default manager if none was specified
		if (containerManager != null) {
			setManager(containerManager);
		}
		manager.setContainer(this);
        
		//启动manager
		if(manager instanceof Lifecycle)
			((Lifecycle) manager).start();
		
		//触发事件：START_EVENT
		this.lifecycle.invoke(START_EVENT, null);
		
		//触发事件：AFTER_START_EVENT
		this.lifecycle.invoke(AFTER_START_EVENT, null);
		
		//Start ContainerBackgroundProcessor thread
		super.setBackgroundProcessorDelay(1);
        super.threadStart();
	}

	@Override
	public synchronized void stop() {
		System.out.println("["+this.getInfo()+"] Stop!");
		if(!started){//未启动
			return;
		}
		//触发事件：BEFORE_STOP_EVENT
		this.lifecycle.invoke(BEFORE_STOP_EVENT, null);
		//触发事件：STOP_EVENT
		this.lifecycle.invoke(STOP_EVENT, null);
		
		//关闭
		started = false;
		
		// 如果有集群，关闭集群
		if (cluster != null && cluster instanceof Lifecycle)
			((Lifecycle) cluster).stop();

		// 如果有子容器，关闭子容器
		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof Lifecycle)
					((Lifecycle) children[i]).stop();
			}
		}
				
		//关闭管道任务
		if(pipeLine instanceof Lifecycle)
			((Lifecycle) pipeLine).stop();
		
		//关闭manager
		if(manager instanceof Lifecycle)
			((Lifecycle) manager).stop();
				
		//触发事件：AFTER_STOP_EVENT
		this.lifecycle.invoke(AFTER_STOP_EVENT, null);
		
		// Stop ContainerBackgroundProcessor thread
        super.threadStop();
	}

	public Manager getManager() {
		return manager;
	}

	public void setManager(Manager manager) {
		this.manager = manager;
	}

	@Override
	public void backgroundProcess() {
		if(!started)
			return;
		
		if(cluster != null)
			cluster.backgroundProcess();
//		System.out.println("will do sth...");
	}

	@Override
	public Cluster getCluster() {
		if(cluster==null && getParent()!=null)
			return getParent().getCluster();
		return this.cluster;
	}

	@Override
	public void setCluster(Cluster cluster) {
		this.cluster = cluster;
	}

}
