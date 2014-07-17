package com.dd.dfs.core;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import com.dd.dfs.Constants;
import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.filesystem.operator.PhysicalFileOperator;


/**
 * 静态资源请求的处理类
 * @author DD
 *
 */
public class StaticResourceProcessor {

	public void process(HttpRequest request, HttpResponse response) throws IOException {
		if (request.getRequestURI() == null)
			return;
		
		File file = new File(Constants.FILE_ROOT, request.getRequestURI());
		System.out.println("请求资源位置：" + Constants.FILE_ROOT + request.getRequestURI());
		if (file.exists()) {// 如果请求的静态资源存在

			long length = file.length();
			OutputStream out = response.getOutputStream();
			PhysicalFileOperator physicOps =  new PhysicalFileOperator();
			
			//设置返回数据头
			response.setContentType("application/octet-stream");
			//TODO filename要替换成原来的名字
			response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
			
			String range = request.getHeader("Range");
			if(range == null){//Http头部没有RANGE字段，普通传输
				response.setContentLength((int) length);
				response.sendHeaders();
				physicOps.downloadData(file.getAbsolutePath(), out, 0, length);
			}else{//开始部分传输
				int start = 0, end = (int) length;
				range = range.substring(6);//Range: bytes=200-299
				if(range.startsWith("-")){//表示最后500个字节：bytes=-500
					start = end - Integer.parseInt(range.substring(1));
				} else if(range.endsWith("-")){//表示500字节以后的范围：bytes=500-
					start = Integer.parseInt(range.substring(0, range.indexOf("-")));
				} else {//表示第二个500字节：bytes=500-999
					start = Integer.parseInt(range.substring(0, range.indexOf("-")));
					end = Integer.parseInt(range.substring(range.indexOf("-")+1));
				}
				
				response.setStatus(206);
				response.setContentLength(end - start);
				response.setHeader("Content-Range", "bytes "+start+"-"+end+"/"+length);
//				response.setContentType("video/mp4");
				physicOps.downloadData(file.getAbsolutePath(), out, start, (end - start));
				response.sendHeaders();
			}
			
		} else {
			// 如果资源找不到，返回错误信息
			String errorMsg = "HTTP/1.1 404 File Not Found\r\n"
					+ "Content-Type: text/html\r\n" + "Content-Length: 23\r\n"
					+ "\r\n" + "<h1>File Not Found</h1>";
			try {
				response.write(errorMsg.getBytes());
				response.flushBuffer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

	
	public void serveResource(HttpRequest request, HttpResponse response) throws IOException{
		if (request.getRequestURI() == null)
			return;
		
		File file = new File(Constants.FILE_ROOT, request.getRequestURI());
		System.out.println("请求资源位置：" + Constants.FILE_ROOT + request.getRequestURI());
	}
}
