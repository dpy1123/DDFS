package com.dd.dfs.filesystem.operator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class PhysicalFileOperator {
	
	int BUFFER_SIZE = 2048;//缓存2k
	
	/**
	 * 上传数据到服务器
	 * @param dest 目标文件路径
	 * @param in 来源数据流
	 * @param offset
	 * @param size
	 */
	public void uploadData(String dest, InputStream in, long offset, long size){
		//创建文件目录
		File file = new File(dest);
		if(!file.getParentFile().exists()) 
			file.getParentFile().mkdirs();
		//新建可随机写的服务器端的文件
		RandomAccessFile randomAccessFile = null;
		try {
			file.createNewFile();
			randomAccessFile = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			System.out.println("文件["+dest+"]不存在！");
		} catch (IOException e) {
			System.out.println("文件["+dest+"]创建失败！");
		}
		//移动到请求上传的部分
		try {
			randomAccessFile.seek(offset);
		} catch (IOException e) {
			System.out.println("文件["+dest+"]seek失败！");
		}
		//保存文件
		int readSize = 0;
		long uploadSize = 0;
		byte[] buffer = new byte[BUFFER_SIZE];
		try {
			while ((readSize = in.read(buffer, 0, BUFFER_SIZE)) != -1) {
				if(uploadSize > size - BUFFER_SIZE) {//last read
					readSize = (int) (size - uploadSize);
				}
				randomAccessFile.write(buffer, 0, readSize);
				uploadSize += readSize;
				if(uploadSize >= size)
					break;
			}
		} catch (IOException e) {
			e.printStackTrace();
			//续传的时候，客户端关闭out流，会报错ClientAbortException:java.io.IOException
		} finally {
			//关闭资源
			if (randomAccessFile != null) {
				try {
					randomAccessFile.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * 从服务器端下载数据。
	 * data结点用，每个线程实例运行同一段代码
	 * @param src 待下载的源文件
	 * @param out 输出到的数据流
	 * @param offset
	 * @param size
	 */
	public void downloadData(String src, OutputStream out, long offset, long size) {
		//新建可随机读的服务器端的文件
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(src, "r");
		} catch (FileNotFoundException e) {
			System.out.println("文件["+src+"]不存在！");
		}
		//移动到请求下载的部分
		try {
			randomAccessFile.seek(offset);
		} catch (IOException e) {
			System.out.println("文件["+src+"]seek失败！");
		}
		//保存文件
//		int readSize = 0;
//		long downloadSize = 0;
		
		long bytesToRead = size;
		byte[] buffer = new byte[BUFFER_SIZE];
		int len = buffer.length;
		while ((bytesToRead > 0) && (len >= buffer.length)) {
			try {
				len = randomAccessFile.read(buffer);
				if (bytesToRead >= len) {
					out.write(buffer, 0, len);
					bytesToRead -= len;
				} else {
					out.write(buffer, 0, (int) bytesToRead);
					bytesToRead = 0;
				}
			} catch (IOException e) {
//				e.printStackTrace();//忽略应客户端取消导致的Software caused connection abort: socket write error 的错误 
				len = -1;
			}
			if (len < buffer.length)
				break;
		}
		try {
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	        
	        
//		try {
//			while ((readSize = randomAccessFile.read(buffer, 0, BUFFER_SIZE)) != -1) {
//				if(downloadSize > size - BUFFER_SIZE) 
//					readSize = (int) (size - downloadSize);
//				out.write(buffer, 0, readSize);
//				downloadSize += readSize;
//				if(downloadSize >= size)
//					break;
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//			//续传的时候，客户端关闭out流，会报错ClientAbortException:java.io.IOException
//		} finally {
//			//关闭资源
//			if (randomAccessFile != null) {
//				try {
//					randomAccessFile.close();
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//		}
	}
	
	public void downloadData(String src, OutputStream out) {
		// 新建可随机读的服务器端的文件
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(src, "r");
		} catch (FileNotFoundException e) {
			System.out.println("文件[" + src + "]不存在！");
		}
		// 保存文件
		byte buffer[] = new byte[BUFFER_SIZE];
		int len = buffer.length;
		while (true) {
			try {
				len = randomAccessFile.read(buffer);
				if (len == -1)
					break;
				out.write(buffer, 0, len);
			} catch (IOException e) {
//				e.printStackTrace();
				len = -1;
				break;
			}
		}
		try {
			randomAccessFile.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
