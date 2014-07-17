package com.dd.dfs.filesystem.data.manager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;



import com.dd.dfs.Constants;
import com.dd.dfs.filesystem.data.Data;
import com.dd.dfs.filesystem.data.DirectoryData;
import com.dd.dfs.filesystem.data.FileData;
import com.dd.dfs.filesystem.data.StandardData;

/**
 * Manager的标准实现
 * 
 * @author DD
 * 
 */
public class StandardManager extends ManagerBase {
	
	/**
	 * The descriptive information about this implementation.
	 */
	protected static final String info = "StandardManager/1.0";

	/**
	 * Path name of the disk file in which active MetaDatas are saved when we
	 * stop, and from which these MetaDatas are loaded when we start. A
	 * <code>null</code> value indicates that no persistence is desired. If this
	 * pathname is relative, it will be resolved against the temporary working
	 * directory provided by our context, available via the
	 * <code>com.dd.dfs.Constants.META_DATA</code> context attribute.
	 */
	protected String pathname = "META.dddfs";
	
	/**
	 * Load any currently active sessions that were previously unloaded to the
	 * appropriate persistence mechanism, if any. If persistence is not
	 * supported, this method returns without doing anything.
	 * 
	 * @exception ClassNotFoundException
	 *                if a serialized class cannot be found during the reload
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	@Override
	public void load() throws ClassNotFoundException, IOException {
		// Initialize our internal data structures
		metaDatas.clear();

		// Open an input stream to the specified pathname, if any
		File file = file();
		if (file == null)
			return;
		System.out.println("standardManager.loading " + pathname);

		FileInputStream fis = null;
		BufferedInputStream bis = null;
		ObjectInputStream ois = null;
		try {
			fis = new FileInputStream(file.getAbsolutePath());
			bis = new BufferedInputStream(fis);
			ois = new ObjectInputStream(bis);
		} catch (FileNotFoundException e) {
			System.out.println("No persisted data file found");
			return;
		} catch (IOException e) {
			System.out.println("standardManager.loading.IOException "
					+ e.getMessage());
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException f) {
					// Ignore
				}
			}
			if (bis != null) {
				try {
					bis.close();
				} catch (IOException f) {
					// Ignore
				}
			}
			throw e;
		}

		// Load the previously unloaded active metaDatas
		synchronized (metaDatas) {
			try {
				Integer count = (Integer) ois.readObject();
				int n = count.intValue();
				System.out.println("Loading " + n + " persisted metaDatas");
				for (int i = 0; i < n; i++) {
					Object dataType = ois.readObject();//先获取metaData的类型
					//根据类型创建相应子类的对象
					if (FileData.class == dataType) {
						FileData data = new FileData(this);
						data.readObjectData(ois);
						metaDatas.put(data.getId(), data);
					} else if (DirectoryData.class == dataType) {
						DirectoryData data = new DirectoryData(this);
						data.readObjectData(ois);
						metaDatas.put(data.getId(), data);
					}
				}
			} catch (ClassNotFoundException e) {
				System.out
						.println("standardManager.loading.ClassNotFoundException "
								+ e.getMessage());
				try {
					ois.close();
				} catch (IOException f) {
					// Ignore
				}
				throw e;
			} catch (IOException e) {
				System.out.println("standardManager.loading.IOException "
						+ e.getMessage());
				try {
					ois.close();
				} catch (IOException f) {
					// Ignore
				}
				throw e;
			} finally {
				// Close the input stream
				try {
					ois.close();
				} catch (IOException f) {
					// ignored
				}

				// Delete the persistent storage file
				if (file.exists())
					file.delete();
			}
		}
	}

	/**
	 * Save any currently active sessions in the appropriate persistence
	 * mechanism, if any. If persistence is not supported, this method returns
	 * without doing anything.
	 * 
	 * @exception IOException
	 *                if an input/output error occurs
	 */
	@Override
	public void unload() throws IOException {

		if (metaDatas.isEmpty()) {
			System.out.println("standardManager.unloading.nodata");
			return; // nothing to do
		}

		// Open an output stream to the specified pathname, if any
		File file = file();
		if (file == null)
			return;
		System.out.println("standardManager.unloading " + pathname);
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		ObjectOutputStream oos = null;
		boolean error = false;
		try {
			fos = new FileOutputStream(file.getAbsolutePath());
			bos = new BufferedOutputStream(fos);
			oos = new ObjectOutputStream(bos);
		} catch (IOException e) {
			error = true;
			System.out.println("standardManager.unloading.IOException "
					+ e.getMessage());
			throw e;
		} finally {
			if (error) {
				if (oos != null) {
					try {
						oos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
				if (bos != null) {
					try {
						bos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
				if (fos != null) {
					try {
						fos.close();
					} catch (IOException ioe) {
						// Ignore
					}
				}
			}
		}

		// Write the number of active metaDatas, followed by the details
		synchronized (metaDatas) {
			System.out.println("Unloading " + metaDatas.size() + " metaDatas");
			try {
				oos.writeObject(new Integer(metaDatas.size()));
				Iterator<Data> elements = metaDatas.values().iterator();
				while (elements.hasNext()) {
					Data data = elements.next();
					oos.writeObject(data.getClass());//针对每一个metaData元素，先写入它的类型，以便读取的时候根据类型创建相应子类
					((StandardData)data).writeObjectData(oos);
				}
			} catch (IOException e) {
				System.out.println("standardManager.unloading.IOException " + e.getMessage());
				try {
					oos.close();
				} catch (IOException f) {
					// Ignore
				}
				throw e;
			}
		}

		// Flush and close the output stream
		try {
			oos.flush();
		} finally {
			try {
				oos.close();
			} catch (IOException f) {
				// Ignore
			}
		}

		System.out.println("Unloading complete");

	}

	/**
	 * Return a File object representing the pathname to our persistence file,
	 * if any.
	 */
	protected File file() {
		if ((pathname == null) || (pathname.length() == 0))
			return (null);
		File file = new File(pathname);
		if (!file.isAbsolute()) {
			file = new File(Constants.META_DATA, pathname);
		}
		return (file);
	}

}
