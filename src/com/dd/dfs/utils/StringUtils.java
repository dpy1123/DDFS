package com.dd.dfs.utils;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;


/**
 * �ַ�����������
 * @author chenlei
 *
 */
public class StringUtils {
	
	public static final String PREFIX = "dd.tv";
	
	private static long code;
	
	public static final String PASSWORD_DIC = "0123456789abcdefghijklmnopqrstuvwxyz";
	
	/**
	 * �������ʶ
	 * @param prefix����ʶǰ׺
	 * @return
	 */
	public static String generateID(String prefix) {
		return prefix + "-" + UUID.randomUUID();
	}
	
	/**
	 * ���ɶ�����
	 * @return
	 */
	 public static synchronized String generateOrderNo() {
        code++;
        String str = new SimpleDateFormat("yyyyMMdd").format(new Date());
        long m = Long.valueOf(str) * 1000000000;
        m += code;
        return PREFIX + m;
    }
	
	/**
	 * �ַ����Ƿ�Ϊ��
	 * @param str
	 * @return
	 */
	public static boolean isNull(String str) {
		return null == str;
	}
	
	/**
	 * �������Ƿ�Ϊ��
	 * @param str
	 * @return
	 */
	public static boolean isNull(Long lon) {
		return null == lon;
	}
	
	/**
	 * �����Ƿ�Ϊ��
	 * @param str
	 * @return
	 */
	public static boolean isNull(Integer integer) {
		return null == integer;
	}
	
	/**
	 * ����nullֵ
	 * @param str
	 * @return
	 */
	public static String processNull(String str) {
		return isNull(str) ? "":str;
	}
	
	/**
	 * ����nullֵ
	 * @param l
	 * @return
	 */
	public static Long processNull(Long l) {
		return isNull(l) ? new Long(0) : l;
	}
	
	/**
	 * ����nullֵ
	 * @param i
	 * @return
	 */
	public static Integer processNull(Integer i) {
		return isNull(i) ? new Integer(0) : i;
	}
	
	/**
	 * ���ַ�����iso-8859-1ת��Ϊutf-8����
	 * @param str
	 * @return
	 */
	public static String convertToUTF8(String str) {
		if(isNull(str)) {
			return "";
		}
		
		try {
			return new String(str.getBytes("iso-8859-1"),"utf-8");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
	
	/**
	 * ���ַ�����gbkת��Ϊiso-8859-1����
	 * @param str
	 * @return
	 */
	public static String convertToISO8859(String str) {
		if(isNull(str)) {
			return "";
		}
		
		try {
			return new String(str.getBytes("gbk"),"iso-8859-1");
		} catch (UnsupportedEncodingException e) {
			return "";
		}
	}
	
	/**
	 * �ж��Ƿ�Ϊ���ַ������磺null/''������true,' '����false;
	 * @param str
	 * @return
	 */
	public static boolean isEmpty(String str) {
		return (null == str || "".equals(str));
	}
	
	/**
	 * ����ָ������ȥ������htmlԪ��
	 * @param input
	 * @param length
	 * @return
	 */
	public static String splitAndFilterString(String input, int length) {   
        if (isEmpty(input.trim())) {   
            return "";   
        }
        
        //ȥ������htmlԪ�� 
        String str = input.replaceAll("\\&[a-zA-Z]{1,10};", "").replaceAll("<[^>]*>", "");
        str = str.replaceAll("[(/>)<]", "").replaceAll("\\s", "").trim();
        int len = str.length();
        if (len <= length) {
            return str;
        } else {
            str = str.substring(0, length);
            str += "...";
        }
        return str;
    }
	
	/**
	 * ת���ַ���ΪHTML����
	 * @param inStr
	 * @return
	 */
	public static String convertToHTMLCode(String inStr) {
		String outStr = "";
	    if(!isNull(inStr)) {
	      inStr = inStr.replaceAll("\n", "<br>");
	      char[] contents = inStr.toCharArray();
	      for (int i = 0; i < contents.length; i++) {
	        char ch = contents[i];
	        if ((ch != '"') && (ch != '<') && (ch != '>') && (ch != '&') && (ch != ' ')) {
	          outStr = String.valueOf(outStr) + ch;
	        } else {
	          if (ch == '"') {
	            outStr = String.valueOf(outStr) + "&quot;";
	          }
	          if (ch == '<')
	          {
	            outStr = String.valueOf(outStr) + "<";
	          }
	          if (ch == '>')
	          {
	            outStr = String.valueOf(outStr) + ">";
	          }
	          if (ch == '&') {
	            outStr = String.valueOf(outStr) + "&amp;";
	          }
	          if (ch == ' ') {
	            outStr = String.valueOf(outStr) + "&nbsp;";
	          }
	        }
	      }
	      contents = null;
	    }
	    inStr = null;

	    return outStr;
	}
	
	/**
	 * URL����
	 * @param str
	 * @return
	 */
	public static String urlEncode(String str){
		if (isEmpty(str)){
	      return str;
	    }
		StringBuffer sb = new StringBuffer();
		int length = str.length();

		for (int i = 0; i < length; i++) {
			char ch = str.charAt(i);

			if (ch == ' '){
				sb.append('+');
			}else if (((ch >= '0') && (ch <= '9')) || ((ch >= 'a') && (ch <= 'z')) || ((ch >= 'A') && (ch <= 'Z')) || (ch == '_')) {
				sb.append(ch);
			}else if (ch < '\020'){
				sb.append("%0" + Integer.toHexString(ch & 0xFF));
			}else {
				sb.append(String.valueOf('%') + Integer.toHexString(ch & 0xFF));
			}
		}

		str = null;

		return sb.toString();
	}
	
	/**
	 * str1 �Ƿ���� str2
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static boolean isContainsStr(String str1,String str2) {
		if(isEmpty(str1) || isEmpty(str2)) {
			return false;
		}
		return str1.indexOf(str2) > 0;
	}
	
	/**
	 * �����ļ�����ú�׺
	 * @param fileName
	 * @return
	 */
	public static String getFileSuffix(String fileName) {
		if(isEmpty(fileName.trim()) || !isContainsStr(fileName, ".")) {
			return "";
		}
		
		return fileName.substring(fileName.lastIndexOf(".") + 1,fileName.length());
	}
	
	/**
	* �����������
	* @param passLenth ���ɵ����볤��
	* @return �������
	*/
	public static String getPass(int passLenth) {

	   StringBuffer buffer = new StringBuffer(PASSWORD_DIC);
	   StringBuffer sb = new StringBuffer();
	   Random r = new Random();
	   int range = buffer.length();
	   for (int i = 0; i < passLenth; i++) {
	    //����ָ����Χ��������0���ַ�������(����0���������ַ�������)
	    sb.append(buffer.charAt(r.nextInt(range)));
	   }
	   return sb.toString();
	}
	
}
