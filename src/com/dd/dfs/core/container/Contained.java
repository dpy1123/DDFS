package com.dd.dfs.core.container;

/**
 * 阀的可选实现接口，该接口的实现类可以通过接口中的方法，至多与一个容器相关联。
 * @author DD
 *
 */
public interface Contained {
	
	public void setContainer(Container container);
	
	public Container getContainer();
}
