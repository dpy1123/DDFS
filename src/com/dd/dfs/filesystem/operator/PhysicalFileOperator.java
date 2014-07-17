package com.dd.dfs.filesystem.operator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class PhysicalFileOperator {
	
	int BUFFER_SIZE = 2048;//����2k
	
	/**
	 * �ϴ����ݵ�������
	 * @param dest Ŀ���ļ�·��
	 * @param in ��Դ������
	 * @param offset
	 * @param size
	 */
	public void uploadData(String dest, InputStream in, long offset, long size){
		//�����ļ�Ŀ¼
		File file = new File(dest);
		if(!file.getParentFile().exists()) 
			file.getParentFile().mkdirs();
		//�½������д�ķ������˵��ļ�
		RandomAccessFile randomAccessFile = null;
		try {
			file.createNewFile();
			randomAccessFile = new RandomAccessFile(file, "rw");
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�["+dest+"]�����ڣ�");
		} catch (IOException e) {
			System.out.println("�ļ�["+dest+"]����ʧ�ܣ�");
		}
		//�ƶ��������ϴ��Ĳ���
		try {
			randomAccessFile.seek(offset);
		} catch (IOException e) {
			System.out.println("�ļ�["+dest+"]seekʧ�ܣ�");
		}
		//�����ļ�
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
			//������ʱ�򣬿ͻ��˹ر�out�����ᱨ��ClientAbortException:java.io.IOException
		} finally {
			//�ر���Դ
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
	 * �ӷ��������������ݡ�
	 * data����ã�ÿ���߳�ʵ������ͬһ�δ���
	 * @param src �����ص�Դ�ļ�
	 * @param out �������������
	 * @param offset
	 * @param size
	 */
	public void downloadData(String src, OutputStream out, long offset, long size) {
		//�½���������ķ������˵��ļ�
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(src, "r");
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�["+src+"]�����ڣ�");
		}
		//�ƶ����������صĲ���
		try {
			randomAccessFile.seek(offset);
		} catch (IOException e) {
			System.out.println("�ļ�["+src+"]seekʧ�ܣ�");
		}
		//�����ļ�
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
//				e.printStackTrace();//����Ӧ�ͻ���ȡ�����µ�Software caused connection abort: socket write error �Ĵ��� 
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
//			//������ʱ�򣬿ͻ��˹ر�out�����ᱨ��ClientAbortException:java.io.IOException
//		} finally {
//			//�ر���Դ
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
		// �½���������ķ������˵��ļ�
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(src, "r");
		} catch (FileNotFoundException e) {
			System.out.println("�ļ�[" + src + "]�����ڣ�");
		}
		// �����ļ�
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
