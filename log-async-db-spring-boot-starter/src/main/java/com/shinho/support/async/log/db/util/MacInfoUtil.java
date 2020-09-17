package com.shinho.support.async.log.db.util;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;

/**
 * MAC地址工具
 * @author 傅为地
 */
public class MacInfoUtil {

	/**
	 * 获取当前操作系统名称. return 操作系统名称 例如:windows,Linux,Unix等.
	 */
	public static String getOSName() {
		return System.getProperty("os.name").toLowerCase();
	}

	/**
	 * 获取Unix网卡的mac地址.
	 * 
	 * @return mac地址
	 */
	public static String getUnixMACAddress() {
		String mac = null;
		BufferedReader bufferedReader = null;
		Process process = null;
		try {
			/**
			 * Unix下的命令，一般取eth0作为本地主网卡 显示信息中包含有mac地址信息
			 */
			process = Runtime.getRuntime().exec("ifconfig eth0");
			bufferedReader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = bufferedReader.readLine()) != null) {
				/**
				 * 寻找标示字符串[hwaddr]
				 */
				index = line.toLowerCase().indexOf("hwaddr");
				/**
				 * 找到了
				 */
				if (index != -1) {
					/**
					 * 取出mac地址并去除2边空格
					 */
					mac = line.substring(index + "hwaddr".length() + 1).trim();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			bufferedReader = null;
			process = null;
		}

		return mac;
	}

	/**
	 * 获取Linux网卡的mac地址.
	 * 
	 * @return mac地址
	 */
	public static String getLinuxMACAddress() {
		String mac = null;
		BufferedReader bufferedReader = null;
		Process process = null;
		try {
			/**
			 * linux下的命令，一般取eth0作为本地主网卡 显示信息中包含有mac地址信息
			 */
			process = Runtime.getRuntime().exec("ifconfig eth0");
			bufferedReader = new BufferedReader(new InputStreamReader(
					process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = bufferedReader.readLine()) != null) {
				index = line.toLowerCase().indexOf("硬件地址");
				/**
				 * 找到了
				 */
				if (index != -1) {
					/**
					 * 取出mac地址并去除2边空格
					 */
					mac = line.substring(index + 4).trim();
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			bufferedReader = null;
			process = null;
		}
		
		// 取不到，试下Unix取发
		if (mac == null){
			return getUnixMACAddress();
		}

		return mac;
	}

	/**
	 * 获取widnows网卡的mac地址.
	 * 
	 * @return mac地址
	 */
	public static String getWindowsMACAddress() {
		String mac = null;
		BufferedReader bufferedReader = null;
		Process process = null;
		try {
			/**
			 * windows下的命令，显示信息中包含有mac地址信息
			 */
			process = Runtime.getRuntime().exec("ipconfig /all");
			bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			int index = -1;
			while ((line = bufferedReader.readLine()) != null) {
				/**
				 * 寻找标示字符串[physical address]
				 */
//				index = line.toLowerCase().indexOf("physical address");
//				if (index != -1) {
				if (line.split("-").length == 6){
					index = line.indexOf(":");
					if (index != -1) {
						/**
						 * 取出mac地址并去除2边空格
						 */
						mac = line.substring(index + 1).trim();
					}
					break;
				}
				index = line.toLowerCase().indexOf("物理地址");
				if (index != -1) {
					index = line.indexOf(":");
					if (index != -1) {
						/**
						 * 取出mac地址并去除2边空格
						 */
						mac = line.substring(index + 1).trim();
					}
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (bufferedReader != null) {
					bufferedReader.close();
				}
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			bufferedReader = null;
			process = null;
		}

		return mac;
	}

	public static String getMac(){
		String os = getOSName();
		String mac;
		if (os.startsWith("windows")) {
			mac = getWindowsMACAddress();
		} else if (os.startsWith("linux")) {
			mac = getLinuxMACAddress();
		} else {
			mac = getUnixMACAddress();
		}
		return mac == null ? "" : mac;
	}
	
	
	 /**   
     * 通过HttpServletRequest返回IP地址   
     * @param request HttpServletRequest   
     * @return ip String   
     * @throws Exception   
     */      
    public static String getIpAddr(HttpServletRequest request) throws Exception {      
        String ip = request.getHeader("x-forwarded-for");      
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {      
            ip = request.getHeader("Proxy-Client-IP");      
        }      
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {      
            ip = request.getHeader("WL-Proxy-Client-IP");      
        }      
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {      
            ip = request.getHeader("HTTP_CLIENT_IP");      
        }      
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {      
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");      
        }      
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {      
            ip = request.getRemoteAddr();      
        }      
        return ip;      
    }      
        
    /**   
     * 通过IP地址获取MAC地址   
     * @param ip String,127.0.0.1格式   
     * @return mac String   
     * @throws Exception   
     */      
    public static String getMACAddress(String ip) throws Exception {      
        String line = "";      
        String macAddress = "";      
        final String MAC_ADDRESS_PREFIX = "MAC Address = ";      
        final String LOOPBACK_ADDRESS = "127.0.0.1";      
        //如果为127.0.0.1,则获取本地MAC地址。      
        if (LOOPBACK_ADDRESS.equals(ip)) {      
            InetAddress inetAddress = InetAddress.getLocalHost();      
            //貌似此方法需要JDK1.6。      
            byte[] mac = NetworkInterface.getByInetAddress(inetAddress).getHardwareAddress();      
            //下面代码是把mac地址拼装成String      
            StringBuilder sb = new StringBuilder();      
            for (int i = 0; i < mac.length; i++) {      
                if (i != 0) {      
                    sb.append("-");      
                }      
                //mac[i] & 0xFF 是为了把byte转化为正整数      
                String s = Integer.toHexString(mac[i] & 0xFF);      
                sb.append(s.length() == 1 ? 0 + s : s);      
            }      
            //把字符串所有小写字母改为大写成为正规的mac地址并返回      
            macAddress = sb.toString().trim().toUpperCase();      
            return macAddress;      
        }      
        //获取非本地IP的MAC地址      
        try {      
            Process p = Runtime.getRuntime().exec("nbtstat -A " + ip);      
            InputStreamReader isr = new InputStreamReader(p.getInputStream());      
            BufferedReader br = new BufferedReader(isr);      
            while ((line = br.readLine()) != null) {      
                if (line != null) {      
                    int index = line.indexOf(MAC_ADDRESS_PREFIX);      
                    if (index != -1) {      
                        macAddress = line.substring(index + MAC_ADDRESS_PREFIX.length()).trim().toUpperCase();      
                    }      
                }      
            }      
            br.close();      
        } catch (IOException e) {      
            e.printStackTrace(System.out);      
        }      
        return macAddress;      
    }      
        
	
	/**
	 * 测试用的main方法.
	 * 
	 * @param argc 运行参数.
	 */
	public static void main(String[] argc) {
		System.err.println(getMac());
		String os = getOSName();
		System.out.println("os: " + os);
		if (os.startsWith("windows")) {
			String mac = getWindowsMACAddress();
			System.out.println("mac: " + mac);
		} else if (os.startsWith("linux")) {
			String mac = getLinuxMACAddress();
			System.out.println("mac: " + mac);
		} else {
			String mac = getUnixMACAddress();
			System.out.println("mac: " + mac);
		}
	}

}