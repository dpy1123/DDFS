package com.dd.dfs;


import java.io.File;

public class Constants {

	/**
	 * 真实存放文件的根路径  /{user.dir}/DFS_ROOT
	 */
	public static final String FILE_ROOT = System.getProperty("user.dir")
			+ File.separator + "DFS_ROOT";
	
	/**
	 * 存放元数据文件的根路径  /{user.dir}/META_DATA
	 */
	public static final String META_DATA = System.getProperty("user.dir")
			+ File.separator + "META_DATA";
	/**
	 * 文件系统根目录的id
	 */
	public static final String ROOT_DATA_ID = "DFS_ROOT_DATA";
	/**
	 * 文件系统根目录的name
	 */
	public static final String ROOT_DATA_NAME = "DFS_ROOT_DATA";
	
}
