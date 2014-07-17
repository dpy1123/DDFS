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
 * �ļ�ϵͳ�߼�������
 * @author dd
 *
 */
public class LogicalFileOperator {
	/**
	 * �����ļ�
	 * @param dir ָ�����ĸ��ļ����´�����
	 * @param localPath ·��ȫ��������test\book.txt������ļ�·�������ڻ��Զ�������
	 * @return FileData
	 */
	public FileData createFileData(DirectoryData dir, String localPath){
		String[] paths = localPath.split("\\"+File.separator);
		//�ҵ������ļ���
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
		//׼���ô��ϴ�����Ŀ���ļ�
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
	 * �����ļ���UUID
	 * @param filename ԭ�ļ���
	 * @return dfs-randomUUID.��չ��
	 */
	public static String generateFileUUID(String filename) {
		String prefix = "dfs";
		String extName = filename.substring(filename.lastIndexOf("."));
		return prefix + "-" + UUID.randomUUID() + extName;
	}
	
	/**
	 * ���ݵ�ǰʱ��������մ����ļ�����·��
	 * @return ·���ַ���  eg: 14\03\19\
	 */
	public static String generateFilePath() {
		String year = DateUtils.getCurrentDateString(DateUtils.SHORT_YEAR);
		String month = DateUtils.getCurrentDateString(DateUtils.MONTH);
		String day = DateUtils.getCurrentDateString(DateUtils.DAY_IN_MONTH);
		return year + File.separator + month + File.separator + day + File.separator;
	}
	
	/**
	 * �ҵ�ָ�����Ƶ������ļ����ļ���
	 * @param dir ������Ŀ¼
	 * @param name ָ������
	 * @param fuzzyQuery �Ƿ�ģ����ѯ��ģ����ѯ֧��name�к���*
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
