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
		
		//注册程序关闭的钩子函数，防止用户非正常退出时，程序没有正确关闭
		ShutdownHook shutdownHook = bootstrap.new ShutdownHook();
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		
		try {
			//让主线程等待，不要退出
			System.in.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.exit(1);
	}
		
	private void init() {
		//设置连接器相关的容器
		httpConnector.setContainer(container);
		
		//添加cluster
		SimpleTcpCluster cluster = new SimpleTcpCluster();
		((StandardDFSContainer) container).setCluster(cluster);
		
		//添加自定义的Valve
//		((StandardDFSContainer)container).getPipeLine().addValve(new StandardFileOpsValve());
		
		//添加一个LifecycleListener
		((StandardDFSContainer)container).addLifecycleListener(new LifecycleListener() {
			@Override
			public void lifecycleEventInvoke(LifecycleEvent event) {
				if(Lifecycle.BEFORE_START_EVENT.equals(event.getType())){
					System.out.println(">>>from LifecycleListener: StandardDFSContainer--BEFORE_START_EVENT");
				}
				if(Lifecycle.START_EVENT.equals(event.getType())){
					System.out.println(">>>from LifecycleListener: StandardDFSContainer--START_EVENT");
					//注册Container的start事件，当manager加载完data的时候，验证是否包含根目录
					//如果不包含，则创建文件系统的根目录
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
		
		//启动
		((StandardDFSContainer) container).start();
		httpConnector.start();
		
		System.out.println("=======Server Start========");
	}

	private void shutdown() {
		//关闭
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
		//找到所在文件夹
		for (int i = 0; i < paths.length - 1; i++) {
			String path = paths[i];
			if(dir.contains(path)){
				dir = (DirectoryData) dir.get(path);
			}
		}
		//准备好待上传到的目标文件
		String fileName = paths[paths.length-1];
		FileData file = null;
		if(dir.contains(fileName)){
			file = (FileData) dir.get(fileName);
		}
		return file;
	}

	/**
	 * 找到指定名称的所有文件或文件夹
	 * @param dir 搜索根目录
	 * @param name 指定名称
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
