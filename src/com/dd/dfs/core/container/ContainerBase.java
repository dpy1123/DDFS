package com.dd.dfs.core.container;

import java.io.IOException;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.container.pipe.inter.PipeLine;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;


/**
 * 本类是实现了Container接口的抽象类
 * @author DD
 *
 */
public abstract class ContainerBase implements Container, Lifecycle {
	
	protected PipeLine pipeLine = null;
	protected LifecycleSupport lifecycle = new LifecycleSupport(this);//生命周期管理工具
	
	
	@Override
	public void invoke(HttpRequest request, HttpResponse response) 
			throws IOException,Exception{
		pipeLine.invoke(request, response);
	}

	public PipeLine getPipeLine() {
		return pipeLine;
	}

	public void setPipeLine(PipeLine pipeLine) {
		this.pipeLine = pipeLine;
	}
	
	//=============以下是Lifecycle接口相关函数=============
	@Override
	public void addLifecycleListener(LifecycleListener listener) {
		this.lifecycle.addLifecycleListener(listener);
	}

	@Override
	public void removeLifecycleListener(LifecycleListener listener) {
		this.lifecycle.removeLifecycleListener(listener);
	}

	@Override
	public LifecycleListener[] findLifecycleListeners() {
		return this.lifecycle.findLifecycleListeners();
	}
	
	
	// -------------------- Background Thread --------------------
	/**
	 * The background thread.
	 */
	private Thread thread = null;
	/**
	 * 后台线程处理结束标志
	 */
	private volatile boolean threadDone = false;
	/**
	 * The processor delay for this component.
	 */
	protected int backgroundProcessorDelay = -1;
	/**
	 * 启动一个后台线程，专门用来处理Container中的各组件需要执行的一些后台任务。在Container实现类的start方法中调用次方法。
	 */
    protected void threadStart() {
        if (thread != null)
            return;
        if (backgroundProcessorDelay <= 0)
            return;

        threadDone = false;
        String threadName = "ContainerBackgroundProcessor[" + getInfo() + "]";
        thread = new Thread(new ContainerBackgroundProcessor(), threadName);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * 停止后台Container的处理线程
     */
    protected void threadStop() {
        if (thread == null)
            return;

        threadDone = true;
        thread.interrupt();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // Ignore
        }
        thread = null;
    }

    /**
     * Set the delay between the invocation of the execute method on this
     * container and its children.
     * 
     * @param delay The delay in seconds between the invocation of 
     *              backgroundProcess methods
     */
    @Override
    public void setBackgroundProcessorDelay(int delay) {
        backgroundProcessorDelay = delay;
    }
    
    /**
     * Get the delay between the invocation of the backgroundProcess method on
     * this container and its children. Child containers will not be invoked
     * if their delay value is not negative (which would mean they are using 
     * their own thread). Setting this to a positive value will cause 
     * a thread to be spawn. After waiting the specified amount of time, 
     * the thread will invoke the executePeriodic method on this container 
     * and all its children.
     */
    @Override
    public int getBackgroundProcessorDelay() {
        return backgroundProcessorDelay;
    }

    // -------------------------------------- ContainerExecuteDelay Inner Class

    /**
     * Private thread class to invoke the backgroundProcess method 
     * of this container and its children after a fixed delay.
     */
    protected class ContainerBackgroundProcessor implements Runnable {

        @Override
        public void run() {
            while (!threadDone) {
                try {
                    Thread.sleep(backgroundProcessorDelay * 1000L);
                } catch (InterruptedException e) {
                    // Ignore
                }
                if (!threadDone) {
                    processChildren(ContainerBase.this);
                }
            }
        }

        protected void processChildren(Container container) {
            try {
                container.backgroundProcess();
            } catch (Throwable t) {
                System.out.println("Exception invoking periodic operation: " + t.getMessage());
            }
            Container[] children = container.findChildren();
            if(children == null) return;
            for (int i = 0; i < children.length; i++) {
                if (children[i].getBackgroundProcessorDelay() <= 0) {
                    processChildren(children[i]);
                }
            }
        }
    }
}
