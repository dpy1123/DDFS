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
	
	private boolean started = false;//��¼�������Ƿ�����
	private Manager manager = null;//Ԫ���ݹ�����
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
		if(started){//�ѱ�����
			return;
		}
		//�����¼���BEFORE_START_EVENT
		this.lifecycle.invoke(BEFORE_START_EVENT, null);
		
		//��ʼ����
		started = true;
		
		//����м�Ⱥ��������Ⱥ
		if(cluster != null && cluster instanceof Lifecycle)
			((Lifecycle) cluster).start();
		
		//�����������������������
		Container[] children = findChildren();
		if(children != null){
			for (int i = 0; i < children.length; i++) {
				if(children[i] instanceof Lifecycle)
					((Lifecycle) children[i]).start();
			}
		}
		
		//��ʼ���ܵ�����
		if(pipeLine instanceof Lifecycle)
			((Lifecycle) pipeLine).start();
		
		//����manager
		Manager containerManager = null;
		if (manager == null) {//���managerû������
			if ((getCluster() != null)) {
				containerManager = new DeltaManager();//��ȺĬ��ʹ��DeltaManager
			} else {
				containerManager = new StandardManager();//�Ǽ�ȺĬ��ʹ��StandardManager
			}
		}
		//Configure default manager if none was specified
		if (containerManager != null) {
			setManager(containerManager);
		}
		manager.setContainer(this);
        
		//����manager
		if(manager instanceof Lifecycle)
			((Lifecycle) manager).start();
		
		//�����¼���START_EVENT
		this.lifecycle.invoke(START_EVENT, null);
		
		//�����¼���AFTER_START_EVENT
		this.lifecycle.invoke(AFTER_START_EVENT, null);
		
		//Start ContainerBackgroundProcessor thread
		super.setBackgroundProcessorDelay(1);
        super.threadStart();
	}

	@Override
	public synchronized void stop() {
		System.out.println("["+this.getInfo()+"] Stop!");
		if(!started){//δ����
			return;
		}
		//�����¼���BEFORE_STOP_EVENT
		this.lifecycle.invoke(BEFORE_STOP_EVENT, null);
		//�����¼���STOP_EVENT
		this.lifecycle.invoke(STOP_EVENT, null);
		
		//�ر�
		started = false;
		
		// ����м�Ⱥ���رռ�Ⱥ
		if (cluster != null && cluster instanceof Lifecycle)
			((Lifecycle) cluster).stop();

		// ��������������ر�������
		Container[] children = findChildren();
		if (children != null) {
			for (int i = 0; i < children.length; i++) {
				if (children[i] instanceof Lifecycle)
					((Lifecycle) children[i]).stop();
			}
		}
				
		//�رչܵ�����
		if(pipeLine instanceof Lifecycle)
			((Lifecycle) pipeLine).stop();
		
		//�ر�manager
		if(manager instanceof Lifecycle)
			((Lifecycle) manager).stop();
				
		//�����¼���AFTER_STOP_EVENT
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
