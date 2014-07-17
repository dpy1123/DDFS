package com.dd.dfs;

import java.io.IOException;
import com.dd.dfs.connector.HttpConnector;
import com.dd.dfs.core.StandardDFSContainer;
import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleEvent;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.data.DirectoryData;
import com.dd.dfs.data.manager.Manager;
import com.dd.dfs.ha.tcp.SimpleTcpCluster;


public class Bootstrap {
	
	HttpConnector httpConnector = new HttpConnector();
	Container container = new StandardDFSContainer();
	
	public static void main(String[] args) {
		Bootstrap bootstrap = new Bootstrap();
		bootstrap.init();
		
		//ע�����رյĹ��Ӻ�������ֹ�û��������˳�ʱ������û����ȷ�ر�
		ShutdownHook shutdownHook = bootstrap.new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		try {
			//�����̵߳ȴ�����Ҫ�˳�
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(1);
	}
		
	private void init() {
		//������������ص�����
		httpConnector.setContainer(container);
		
		//���cluster
		SimpleTcpCluster cluster = new SimpleTcpCluster();
		((StandardDFSContainer) container).setCluster(cluster);
		
		//����Զ����Valve
//		((StandardDFSContainer)container).getPipeLine().addValve(new StandardFileOpsValve());
		
		//���һ��LifecycleListener
		((StandardDFSContainer)container).addLifecycleListener(new LifecycleListener() {
			@Override
			public void lifecycleEventInvoke(LifecycleEvent event) {
				if(Lifecycle.BEFORE_START_EVENT.equals(event.getType())){
					System.out.println(">>>from LifecycleListener: StandardDFSContainer--BEFORE_START_EVENT");
				}
				if(Lifecycle.START_EVENT.equals(event.getType())){
					System.out.println(">>>from LifecycleListener: StandardDFSContainer--START_EVENT");
					//ע��Container��start�¼�����manager������data��ʱ����֤�Ƿ������Ŀ¼
					//������������򴴽��ļ�ϵͳ�ĸ�Ŀ¼
					Manager manager = ((StandardDFSContainer)event.getLifecycle()).getManager();
					if (manager.findData(Constants.ROOT_DATA_ID) == null) {
						DirectoryData root = new DirectoryData(manager,
								Constants.ROOT_DATA_ID,
								Constants.ROOT_DATA_NAME,
								Constants.FILE_ROOT, null);
						manager.add(root);
					}
				}
				if(Lifecycle.BEFORE_STOP_EVENT.equals(event.getType())){
					System.out.println(">>>from LifecycleListener: StandardDFSContainer--BEFORE_STOP_EVENT");
				}
			}
		});
		
		//����
		((StandardDFSContainer) container).start();
		httpConnector.start();
		
		System.out.println("=======Server Start========");
	}

	private void shutdown() {
		//�ر�
		((StandardDFSContainer) container).stop();
		httpConnector.stop();
		System.out.println("=======Server Stop========");
	}
	
	
	protected class ShutdownHook extends Thread {
		@Override
		public void run() {
			shutdown();
		}
	}
	
	/*
	public FileData getFileData(DirectoryData dir, String localPath){
		String[] paths = localPath.split("\\"+File.separator);
		//�ҵ������ļ���
		for (int i = 0; i < paths.length - 1; i++) {
			String path = paths[i];
			if(dir.contains(path)){
				dir = (DirectoryData) dir.get(path);
			}
		}
		//׼���ô��ϴ�����Ŀ���ļ�
		String fileName = paths[paths.length-1];
		FileData file = null;
		if(dir.contains(fileName)){
			file = (FileData) dir.get(fileName);
		}
		return file;
	}

	/**
	 * �ҵ�ָ�����Ƶ������ļ����ļ���
	 * @param dir ������Ŀ¼
	 * @param name ָ������
	 * @return
	 */
	
	/*
	public List<IData> findData(DirectoryData dir, String name, boolean fuzzyQuery) {
		ArrayList<IData> result = new ArrayList<IData>();
		if(!fuzzyQuery && dir.get(name)!=null)
			result.add(dir.get(name));
		for(String key : dir.getContents().keySet()) {
			IData data = dir.get(key);
			if(fuzzyQuery && data instanceof FileData) {
				String fileName = ((FileData)data).getName();
				name = name.replaceAll("\\*", "[\u4e00-\u9fa5A-Za-z0-9\\.\\_\\ ]*");
				if(Pattern.matches(name, fileName))
					result.add(data);
			}
			if(data instanceof DirectoryData) {
				result.addAll(findData((DirectoryData)data, name, fuzzyQuery));
			}
		}
		return result;
	}
	
	public void listDirectory(DirectoryData dir){
		System.out.println("ls <"+dir.getName()+">: ");
		for (String key : dir.getContents().keySet() ) {
			IData data = dir.get(key);
			boolean isDri = data instanceof DirectoryData;
			if(isDri){
				System.out.println("<"+key+">	");
				listDirectory((DirectoryData) data);
			}else {
				System.out.print(key+"	");
			}
		}
	}*/
}
