package com.dd.dfs;


import java.io.File;

public class Constants {

	/**
	 * ��ʵ����ļ��ĸ�·��  /{user.dir}/DFS_ROOT
	 */
	public static final String FILE_ROOT = System.getProperty("user.dir")
			+ File.separator + "DFS_ROOT";
	
	/**
	 * ���Ԫ�����ļ��ĸ�·��  /{user.dir}/META_DATA
	 */
	public static final String META_DATA = System.getProperty("user.dir")
			+ File.separator + "META_DATA";
	/**
	 * �ļ�ϵͳ��Ŀ¼��id
	 */
	public static final String ROOT_DATA_ID = "DFS_ROOT_DATA";
	/**
	 * �ļ�ϵͳ��Ŀ¼��name
	 */
	public static final String ROOT_DATA_NAME = "DFS_ROOT_DATA";
	
}
