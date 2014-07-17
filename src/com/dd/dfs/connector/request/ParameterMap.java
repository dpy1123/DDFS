package com.dd.dfs.connector.request;

import java.util.HashMap;
import java.util.Map;

/**
 * 存放请求参数的类，比普通的HashMap多了写锁
 * @author DD
 *
 */
public class ParameterMap extends HashMap<Object, Object>{

	private static final long serialVersionUID = 1L;
	
	private boolean locked = false;//写锁，标记当前HashMap是否上锁

	public boolean isLocked() {
		return locked;
	}

	public void setLocked(boolean locked) {
		this.locked = locked;
	}
	
	@Override
	public Object put(Object key, Object value) {
		if(this.locked){
			System.out.println("ParameterMap has been locked!");
			return null;
		}
		return super.put(key, value);
	}
	
	@Override
	public void putAll(Map<?, ?> m) {
		if(this.locked){
			System.out.println("ParameterMap has been locked!");
			return;
		}
		super.putAll(m);
	}
	
	@Override
	public Object remove(Object key) {
		if(this.locked){
			System.out.println("ParameterMap has been locked!");
			return null;
		}
		return super.remove(key);
	}
	
	@Override
	public void clear() {
		if(this.locked){
			System.out.println("ParameterMap has been locked!");
			return;
		}
		super.clear();
	}
}
