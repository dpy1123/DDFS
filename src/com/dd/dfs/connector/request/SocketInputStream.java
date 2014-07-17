package com.dd.dfs.connector.request;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;

import javax.servlet.ServletInputStream;

/**
 * 用于解析请求头
 * @author dd
 *
 */
public class SocketInputStream extends ServletInputStream{
	/*
	 * 请求报文格式，CRLF表示回车，*表示有多个，()表示可选的，SP表示空格
	 *	HTTP Request = Request-Line CRLF   	//请求行
	 *   				*((request-header : request-value) CRLF)  //请求头
	 *   				CRLF
	 *   				[message-body]		//请求体
	 * 其中  	Request-Line = Method SP URL SP Protocol-Version CRLF
	 *  
	 * */
	private InputStream inputStream;//输入流
	/**
	 * Internal buffer.
	 */
	protected byte buf[];
	protected int bufferSize = 2048;
	/**
	 * Last valid byte.
	 */
	protected int count = -1;
	/**
	 * Position in the buffer.
	 */
	protected int pos = 0;
    
	protected String method;
	protected String uri;
	protected String protocol;

	private char[] methodChar;
	private char[] uriChar;
	private char[] protocolChar;
	private char[] name;
	private char[] value;

	
	// -------------------------------------------------------------- Constants
    public static final int INITIAL_METHOD_SIZE = 8;
    public static final int INITIAL_URI_SIZE = 64;
    public static final int INITIAL_PROTOCOL_SIZE = 8;
    public static final int MAX_METHOD_SIZE = 1024;
    public static final int MAX_URI_SIZE = 32768;
    public static final int MAX_PROTOCOL_SIZE = 1024;
    
    private static final byte CR = (byte) '\r';
    private static final byte LF = (byte) '\n';
    private static final byte SP = (byte) ' ';
    private static final byte COLON = (byte) ':';
    /**
     * Lower case offset.
     */
//    private static final int LC_OFFSET = 'A' - 'a';
    /**
     * 水平制表位
     */
    private static final byte HT = (byte) '\t';
    
    public static final int INITIAL_NAME_SIZE = 32;
    public static final int INITIAL_VALUE_SIZE = 64;
    public static final int MAX_NAME_SIZE = 128;
    public static final int MAX_VALUE_SIZE = 4096;
    
	public SocketInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
		this.buf = new byte[bufferSize];
		this.methodChar = new char[INITIAL_METHOD_SIZE];
		this.uriChar = new char[INITIAL_URI_SIZE];
		this.protocolChar = new char[INITIAL_PROTOCOL_SIZE];
		this.name = new char[INITIAL_NAME_SIZE];
		this.value = new char[INITIAL_VALUE_SIZE];
	}

	/**
	 * 读取请求行内容，得到请求的方法、uri、协议版本
	 * @throws IOException 
	 */
	public void readRequestLine() throws IOException {
		//从InputStream中一个字节一个字节开始读，当读完第一个换号符时停止。已读部分是请求行。
		/*
		StringBuffer requestLine = new StringBuffer(1024);//只读取1024字节的请求行
		int readChar = 0;
		try {
			while((readChar = read()) != -1){
				if(readChar == 13){//读到换号符 '\r'
					if((readChar = read()) == 10)//读到换号符 '\n'
						break;
				}
				requestLine.append((char)readChar);
			}
		} catch (IOException e) {
			// this.read()方法出错
			e.printStackTrace();
		}
		if (readChar == -1)
            throw new EOFException("requestStream.readRequestLine.error");
		
		String requestStr = requestLine.toString();
		int indexFirstSpace = requestStr.indexOf(" ");//请求头中，第一个空格的位置
		if(indexFirstSpace != -1){//表示找到第一个空格
			int indexSecondSpace = requestStr.indexOf(" ", indexFirstSpace+1);//第二个空格的位置
			if(indexSecondSpace > indexFirstSpace){//表示找到第二个空格
				//第一个空格和第二个空格之间是请求的Uri
				//GET /index.html HTTP/1.1
				this.method = requestStr.substring(0, indexFirstSpace);
				this.uri = requestStr.substring(indexFirstSpace+1, indexSecondSpace);
				this.protocol = requestStr.substring(indexSecondSpace+1);
			}
		}
		*/
		// Checking for a blank line
		int readChar = 0;
		do { // Skipping CR or LF
            try {
            	readChar = read();
            } catch (IOException e) {
            	readChar = -1;
            }
        } while ((readChar == CR) || (readChar == LF));
        if (readChar == -1)
            throw new EOFException("requestStream.readRequestLine.error");
		pos--;
		
		 // Reading the method name

        int maxRead = methodChar.length;//method length
        int readCount = 0;

        boolean space = false;

        while (!space) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= MAX_METHOD_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(methodChar, 0, newBuffer, 0, maxRead);
                    methodChar = newBuffer;
                    maxRead = methodChar.length;
                } else {
                    throw new IOException("requestStream.readline.toolong");
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            }
            methodChar[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }
        method = new String(methodChar, 0, readCount-1);
        
        
        // Reading URI

        maxRead = uriChar.length;
        readCount = 0;

        space = false;
        boolean eol = false;

        while (!space) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= MAX_URI_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(uriChar, 0, newBuffer, 0, maxRead);
                    uriChar = newBuffer;
                    maxRead = uriChar.length;
                } else {
                    throw new IOException("requestStream.readline.toolong");
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1)
                    throw new IOException("requestStream.readline.error");
                pos = 0;
            }
            if (buf[pos] == SP) {
                space = true;
            } else if ((buf[pos] == CR) || (buf[pos] == LF)) {
                // HTTP/0.9 style request
                eol = true;
                space = true;
            }
            uriChar[readCount] = (char) buf[pos];
            readCount++;
            pos++;
        }
        uri = new String(uriChar, 0, readCount-1);
        
        // Reading protocol

        maxRead = protocolChar.length;
        readCount = 0;

        while (!eol) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= MAX_PROTOCOL_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(protocolChar, 0, newBuffer, 0, maxRead);
                    protocolChar = newBuffer;
                    maxRead = protocolChar.length;
                } else {
                    throw new IOException("requestStream.readline.toolong");
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                // Copying part (or all) of the internal buffer to the line
                // buffer
                int val = read();
                if (val == -1)
                    throw new IOException("requestStream.readline.error");
                pos = 0;
            }
            if (buf[pos] == CR) {
                // Skip CR.
            } else if (buf[pos] == LF) {
                eol = true;
            } else {
            	protocolChar[readCount] = (char) buf[pos];
                readCount++;
            }
            pos++;
        }
        protocol = new String(protocolChar, 0, readCount);
        
	}

	/**
	 * 读取请求头内容，应该在读取完请求行后调用。返回一条请求头key:value，或null表示没读到请求头
	 * @throws IOException 
	 */
	public String readHeader() throws IOException{
//		StringBuffer requestHeader = new StringBuffer(1024);
//		int readChar = 0;
//		try {
//			while((readChar = read()) != -1){
//				if(readChar == 13){//读到换号符 '\r'
//					if((readChar = read()) == 10)//读到换号符 '\n'
//						break;
//				}
//				requestHeader.append((char)readChar);
//			}
//		} catch (IOException e) {
//			// this.read()方法出错
//			e.printStackTrace();
//		}
//		if(requestHeader.length() != 0 && requestHeader.indexOf(":") > 0){
//			return requestHeader.toString();
//		}else{
//			return null;
//		}
		
		// Checking for a blank line
        int chr = read();
        if ((chr == CR) || (chr == LF)) { // Skipping CR
            if (chr == CR)
                read(); // Skipping LF
            return null;
        } else {
            pos--;
        }
        
        // Reading the header name
        int maxRead = name.length;
        int readCount = 0;

        boolean colon = false;

        while (!colon) {
            // if the buffer is full, extend it
            if (readCount >= maxRead) {
                if ((2 * maxRead) <= MAX_NAME_SIZE) {
                    char[] newBuffer = new char[2 * maxRead];
                    System.arraycopy(name, 0, newBuffer, 0, maxRead);
                    name = newBuffer;
                    maxRead = name.length;
                } else {
                    throw new IOException("requestStream.readline.toolong");
                }
            }
            // We're at the end of the internal buffer
            if (pos >= count) {
                int val = read();
                if (val == -1) {
                    throw new IOException("requestStream.readline.error");
                }
                pos = 0;
            }
            if (buf[pos] == COLON) {
                colon = true;
            }
            char val = (char) buf[pos];
//            if ((val >= 'A') && (val <= 'Z')) {//大写转小写
//                val = (char) (val - LC_OFFSET);
//            }
            name[readCount] = val;
            readCount++;
            pos++;
        }
        String headerName = new String(name, 0, readCount-1);
        
        
        // Reading the header value (which can be spanned over multiple lines)
        maxRead = value.length;
        readCount = 0;

        boolean eol = false;
        boolean validLine = true;

        while (validLine) {

            boolean space = true;

            // Skipping spaces
            // Note : Only leading white spaces are removed. Trailing white spaces are not.
            while (space) {
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException("requestStream.readline.error");
                    pos = 0;
                }
                if ((buf[pos] == SP) || (buf[pos] == HT)) {
                    pos++;
                } else {
                    space = false;
                }
            }

            while (!eol) {
                // if the buffer is full, extend it
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(value, 0, newBuffer, 0, maxRead);
                        value = newBuffer;
                        maxRead = value.length;
                    } else {
                        throw new IOException("requestStream.readline.toolong");
                    }
                }
                // We're at the end of the internal buffer
                if (pos >= count) {
                    // Copying part (or all) of the internal buffer to the line
                    // buffer
                    int val = read();
                    if (val == -1)
                        throw new IOException("requestStream.readline.error");
                    pos = 0;
                }
                if (buf[pos] == CR) {
                } else if (buf[pos] == LF) {
                    eol = true;
                } else {
                    // FIXME : Check if binary conversion is working fine
                    int ch = buf[pos] & 0xff;
                    value[readCount] = (char) ch;
                    readCount++;
                }
                pos++;
            }

            int nextChr = read();

            if ((nextChr != SP) && (nextChr != HT)) {
                pos--;
                validLine = false;
            } else {
                eol = false;
                // if the buffer is full, extend it
                if (readCount >= maxRead) {
                    if ((2 * maxRead) <= MAX_VALUE_SIZE) {
                        char[] newBuffer = new char[2 * maxRead];
                        System.arraycopy(value, 0, newBuffer, 0, maxRead);
                        value = newBuffer;
                        maxRead = value.length;
                    } else {
                        throw new IOException("requestStream.readline.toolong");
                    }
                }
                value[readCount] = ' ';
                readCount++;
            }

        }

        String headerVaule = new String(value, 0, readCount);
		return headerName + ":" + headerVaule;
	}
	
	
    /**
     * Read byte.
     */
	@Override
	public int read() throws IOException {
		if (pos >= count) {//读到buf尾部了，则重新读取填充buf
			fill();
			if (pos >= count)//没读到返回-1
				return -1;
		}
		return buf[pos++] & 0xff;//返回下一个字符
	}

	/**
	 * Fill the internal buffer using data from the undelying input stream.
	 */
	protected void fill() throws IOException {
		pos = 0;
		count = 0;
		int nRead = 0;
		try {
			nRead = inputStream.read(buf, 0, buf.length);//尝试一次性往buf读入数据
		} catch (SocketException e) {
			// TODO: [fix me]忽略了java.net.SocketException: Connection reset
		}
		if (nRead > 0) {
			count = nRead;
		}
	}
	
	/**
	 * Returns the number of bytes that can be read from this input stream
	 * without blocking.
	 */
	public int available() throws IOException {
		return (count - pos) + inputStream.available();
	}

	public String getMethod() {
		return method;
	}

	public String getUri() {
		return uri;
	}

	public String getProtocol() {
		return protocol;
	}

}
