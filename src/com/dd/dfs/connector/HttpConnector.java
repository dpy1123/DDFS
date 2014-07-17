package com.dd.dfs.connector;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Stack;
import java.util.Vector;

import com.dd.dfs.connector.request.HttpRequest;
import com.dd.dfs.connector.response.HttpResponse;
import com.dd.dfs.core.container.Container;
import com.dd.dfs.core.lifecycle.Lifecycle;
import com.dd.dfs.core.lifecycle.LifecycleListener;
import com.dd.dfs.core.lifecycle.LifecycleSupport;



/**
 * ������
 * @author DD
 *
 */
public class HttpConnector implements Runnable, Lifecycle {

	private int port = 6365;//���������web�˿�
	/**
	 * The accept count for this Connector.
	 */
	private int acceptCount = 10;
    /**
     * The IP address on which to bind, if any.  If <code>null</code>, all
     * addresses on the server will be bound.
     */
    private String address = null;
	
	private boolean start = false;
    private LifecycleSupport lifecycle = new LifecycleSupport(this);//�������ڹ�����
	
	private Stack<HttpProcessor> processors = new Stack<HttpProcessor>();//��ջ������HttpProcessor�߳�
	/**
     * The set of processors that have ever been created.
     */
    private Vector<HttpProcessor> created = new Vector<HttpProcessor>();//���Դ�����processor����ļ��ϣ�ֻ���������ñ���
    
	private int minProcessors = 5;//�����߳���
	private int maxProcessors = 20;//����߳���
	private int curProcessors = 0;//��ǰ�߳���
	
	private Container container = null;//������Ӧ��Servlet��������
	
	/**
     * Timeout value on the incoming connection.
     * Note : a value of 0 means no timeout.
     */
    private int connectionTimeout = 0;//����
    
	private ServerSocket serverSocket = null;

    /**
     * The thread synchronization object.
     */
    private Object threadSync = new Object();
    /**
     * The background thread.
     */
    private Thread thread = null;
    
    public HttpConnector() {
    	serverSocket = open();
	}
    
    /**
     * Open and return the server socket for this Connector.  If an IP
     * address has been specified, the socket will be opened only on that
     * address; otherwise it will be opened on all addresses.
     */
	private ServerSocket open() {
		// If address is null, it will default accepting connections on any/all
		// local addresses.
		if (address == null) {
			try {
				return new ServerSocket(port, acceptCount);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		// Open a server socket on the specified address
		try {
			InetAddress is = InetAddress.getByName(address);
			return new ServerSocket(port, acceptCount, is);
		} catch (Exception e) {
			System.out.println("httpConnector.noAddress: " + address);
			try {
				return new ServerSocket(port, acceptCount);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
		}
		return null;
	}
	 

	@Override
	public void run() {
		// Loop until we receive a shutdown command
		while (start) {
			Socket client = null;
			try {
				client = serverSocket.accept();
				if (connectionTimeout > 0)
					client.setSoTimeout(connectionTimeout);
			} catch (IOException ioe) {
				try {
					// If reopening fails, exit
					synchronized (threadSync) {
						if (start){
							System.out.println("accept error: " + ioe);
							serverSocket.close();
							serverSocket = open();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}

				continue;
			}

			// Hand this socket off to an appropriate processor
			HttpProcessor processor = getProcessor();
			if (processor == null) {
				try {
					System.out.println("httpConnector.noProcessor");
					client.close();
				} catch (IOException e) {
					;
				}
				continue;
			}

			processor.assign(client);
			// The processor will recycle itself when it finishes
		}

        synchronized (threadSync) {
            threadSync.notifyAll();
        }

	}

	/**
	 * ���һ�����õĴ����̣߳�����null��ʾ��ǰ�޿����߳�
	 * @return
	 */
	private HttpProcessor getProcessor() {
		if(processors.isEmpty()){//����߳�ջ��ǰ�޿����߳�
			if(curProcessors < maxProcessors){//���߳���δ�ﵽ���ֵ
				//�����´����̣߳�������
				return newProcessor();
			}else{//�߳����ﵽ���ֵ������null
				return null;
			}
		}else{//�߳�ջ�п����߳�
			return processors.pop();
		}
	}

    /**
     * Create and return a new processor suitable for processing HTTP
     * requests and returning the corresponding responses.
     */
    private HttpProcessor newProcessor() {
        HttpProcessor processor = new HttpProcessor(this, curProcessors++);
		if (processor instanceof Lifecycle) {
			((Lifecycle) processor).start();
		}
        created.addElement(processor);
        return (processor);
    }
    
	/**
	 * ���´�������ʹ�õ��߳���ջ
	 * @param processor
	 */
	public void recycle(HttpProcessor processor) {
		processors.push(processor);
	}
	
	public Container getContainer() {
		return container;
	}

	public void setContainer(Container container) {
		this.container = container;
	}

    /**
     * Create (or allocate) and return a Request object suitable for
     * specifying the contents of a Request to the responsible Container.
     */
	public HttpRequest createRequest() {
		HttpRequest request = new HttpRequest();
		return (request);
	}

	public HttpResponse createResponse() {
		HttpResponse response = new HttpResponse();
		return (response);
	}

	// =============������Lifecycle�ӿ���غ���=============
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

	
	/**
     * Start the background processing thread.
     */
    private void threadStart() {
        System.out.println("HttpConnector.starting");

        String threadName = "HttpConnector[" + port + "]";
        thread = new Thread(this, threadName);
        thread.setDaemon(true);
        thread.start();

    }
	
	/**
	 * �����������ļ����߳�
	 */
	public void start() {
		if (start) {// �ѱ�����
			return;
		}

		lifecycle.invoke(START_EVENT, null);
		start = true;

		// Start our background thread
		threadStart();

		// ������������С�����Ĵ����߳�
		while (curProcessors < minProcessors) {
			HttpProcessor processor = newProcessor();
			recycle(processor);
			if ((maxProcessors > 0) && (curProcessors >= maxProcessors))
				break;
		}
	}
	
    /**
     * Stop the background processing thread.
     */
    private void threadStop() {
    	System.out.println("httpConnector.stopping");

        try {
            threadSync.wait(5000);
        } catch (InterruptedException e) {
            ;
        }
        thread = null;
    }
    
	@Override
	public void stop() {
		if(!start){//δ����
			return;
		}
		
		//�����¼���STOP_EVENT
		this.lifecycle.invoke(STOP_EVENT, null);
		start = false;
		
		 // Gracefully shut down all processors we have created
        for (int i = created.size() - 1; i >= 0; i--) {
			HttpProcessor processor = (HttpProcessor) created.elementAt(i);
			if (processor instanceof Lifecycle) {
				((Lifecycle) processor).stop();
			}
        }

        synchronized (threadSync) {
            // Close the server socket we were using
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    ;
                }
            }
            // Stop our background thread
            threadStop();
        }
        serverSocket = null;
        
	}

}
