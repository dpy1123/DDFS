package com.dd.dfs.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.catalina.tribes.Member;

import com.dd.dfs.Constants;
import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.container.Contained;
import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.container.pipe.inter.Valve;
import com.dd.dfs.core.container.pipe.inter.ValveContext;
import com.dd.dfs.data.Data;
import com.dd.dfs.data.DataStore;
import com.dd.dfs.data.DirectoryData;
import com.dd.dfs.data.FileData;
import com.dd.dfs.data.manager.StandardManager;
import com.dd.dfs.filesystem.operator.LogicalFileOperator;
import com.dd.dfs.filesystem.operator.PhysicalFileOperator;
import com.dd.dfs.ha.Cluster;
import com.dd.dfs.ha.tcp.SimpleTcpCluster;

public class StandardFileOpsValve implements Valve, Contained {

	private Container container = null;
	private String info = "StandardFileOpsValve v1.0";
	
	public StandardFileOpsValve(Container container) {
		this.container = container;
	}
	
	@Override
	public void setContainer(Container container) {
		this.container = container;
	}

	@Override
	public Container getContainer() {
		return container;
	}

	@Override
	public String getInfo() {
		return this.info;
	}

	@Override
	public void invoke(HttpRequest request, HttpResponse response,
			ValveContext context) throws IOException, Exception {
		// http://127.0.0.1:6363/RequestURI?QueryString
		System.out.println("......"+request.getMethod());
		System.out.println("......"+request.getProtocol());
		System.out.println("......"+request.getRequestURI());
		System.out.println("......"+request.getQueryString());

		String uri = request.getRequestURI();
		
		StandardManager manager = (StandardManager) ((StandardDFSContainer)container).getManager();
		
		if("/fileops/upload_locate".equalsIgnoreCase(uri)){//获取上传到的地址
			String clientIP = request.getRemoteAddr();
			System.out.println("....clientIP: "+clientIP);
			
			//TODO 根据客户端IP地址选择一个合适的上传服务器
			Cluster cluster = container.getCluster();
			if(cluster != null && cluster instanceof SimpleTcpCluster){
				Member[] members = ((SimpleTcpCluster)container.getCluster()).getMembers();
				
			}
			
			String content = "{\"url\": \"http://127.0.0.1:6363\"}";
			response.setContentType("application/json;charset=UTF-8");
			response.setContentLength(content.length());
			response.write(content.getBytes());
			response.flushBuffer();
			
		}else if("/fileops/upload_file".equalsIgnoreCase(uri)){//真实向本机上传文件
			String overwrite = request.getParameter("overwrite");//是否覆盖已有文件
			String filename = request.getParameter("filename");
			String path = request.getParameter("path");
			System.out.println("......"+request.getHeader("Content-Length"));

			long size = request.getContentLength();//文件大小
			if(size == 0) return;
			
			//处理path的格式，生成形如dir1\dir2\dd.txt的路径
			if(path.startsWith(File.separator)) path = path.substring(1);
			if(path.endsWith(File.separator)) path = path.substring(0, path.length()-1);
			path = path + File.separator + filename;
			
			LogicalFileOperator logicOps = new LogicalFileOperator();
			PhysicalFileOperator physicOps =  new PhysicalFileOperator();
			
			FileData file = logicOps.createFileData((DirectoryData) manager.findData(Constants.ROOT_DATA_ID), path);
			DataStore newStore = new DataStore();
			newStore.setPhysicalName(filename);
			String fullPath = Constants.FILE_ROOT + File.separator + LogicalFileOperator.generateFilePath() + LogicalFileOperator.generateFileUUID(filename);
			newStore.setPhysicalPath(fullPath);
			//真实传输
			physicOps.uploadData(fullPath, request.getInputStream(), 0, size);
			newStore.setAvailable(true);
			file.addDataStore(newStore);
		}else if("/fileops/download_file".equalsIgnoreCase(uri)){
			//这里return一个真实文件地址就行了，如http://127.0.0.1:6363/14/04/04/20.mp3
			
		}else if("/fileops/metadata".equalsIgnoreCase(uri)){//获取文件或文件夹信息
			//Url：http://127.0.0.1:6363/metadata?path=
			LogicalFileOperator logicOps = new LogicalFileOperator();
			List<Data> results = logicOps.findData((DirectoryData) manager.findData(Constants.ROOT_DATA_ID), "dd.txt", false);
			for (Data data : results) {
				System.out.println(data.getName() +"  "+ data.getPath());
			}
		}
		
		
		
	}

}
