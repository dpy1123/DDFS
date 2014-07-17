package com.dd.dfs.filesystem.operator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import com.dd.dfs.data.Data;
import com.dd.dfs.data.DirectoryData;
import com.dd.dfs.data.FileData;
import com.dd.dfs.utils.DateUtils;

/**
 * 文件系统逻辑操作类
 * @author dd
 *
 */
public class LogicalFileOperator {
	/**
	 * 创建文件
	 * @param dir 指定在哪个文件夹下创建。
	 * @param localPath 路径全名，形如test\book.txt。如果文件路径不存在会自动创建。
	 * @return FileData
	 */
	public FileData createFileData(DirectoryData dir, String localPath){
		String[] paths = localPath.split("\\"+File.separator);
		//找到所在文件夹
		for (int i = 0; i < paths.length - 1; i++) {
			String path = paths[i];
			if(dir.contains(path)){
				dir = (DirectoryData)dir.getData(path);
			}else{
				DirectoryData newDir = new DirectoryData(dir.getManager(), UUID.randomUUID().toString(), path, dir.getPath()+File.separator+path, dir.getId()); 
				dir.put(path, newDir.getId());
				dir.getManager().add(newDir);
				dir = newDir;
			}
		}
		//准备好待上传到的目标文件
		String fileName = paths[paths.length-1];
		FileData file = null;
		if(dir.contains(fileName)){
			file = (FileData) dir.getData(fileName);
		}else{
			FileData newFile = new FileData(dir.getManager(), UUID.randomUUID().toString(), fileName, dir.getPath()+File.separator+fileName, dir.getId()); 
			dir.put(fileName, newFile.getId());
			dir.getManager().add(newFile);
			file = newFile;
		}
		return file;
	}

	/**
	 * 生成文件名UUID
	 * @param filename 原文件名
	 * @return dfs-randomUUID.扩展名
	 */
	public static String generateFileUUID(String filename) {
		String prefix = "dfs";
		String extName = filename.substring(filename.lastIndexOf("."));
		return prefix + "-" + UUID.randomUUID() + extName;
	}
	
	/**
	 * 根据当前时间的年月日创建文件储存路径
	 * @return 路径字符串  eg: 14\03\19\
	 */
	public static String generateFilePath() {
		String year = DateUtils.getCurrentDateString(DateUtils.SHORT_YEAR);
		String month = DateUtils.getCurrentDateString(DateUtils.MONTH);
		String day = DateUtils.getCurrentDateString(DateUtils.DAY_IN_MONTH);
		return year + File.separator + month + File.separator + day + File.separator;
	}
	
	/**
	 * 找到指定名称的所有文件或文件夹
	 * @param dir 搜索根目录
	 * @param name 指定名称
	 * @param fuzzyQuery 是否模糊查询，模糊查询支持name中含有*
	 * @return
	 */
	public List<Data> findData(DirectoryData dir, String name, boolean fuzzyQuery) {
		ArrayList<Data> result = new ArrayList<Data>();
		if(!fuzzyQuery && dir.getData(name)!=null)
			result.add(dir.getData(name));
		for(String key : dir.keys()) {
			Data data = dir.getData(key);
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
}
