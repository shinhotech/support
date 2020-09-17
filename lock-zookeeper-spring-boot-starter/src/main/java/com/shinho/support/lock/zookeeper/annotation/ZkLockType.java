package com.shinho.support.lock.zookeeper.annotation;

/**  
 * @author 傅为地
 */
public enum ZkLockType {
	/** 可重入锁*/
	REENTRANT_LOCK(0,"可重入互斥锁"),
	
	/** 互斥不可重入锁*/
	UN_REENTRANT_LOCK(1,"互斥不可重入锁"),

	/** 读锁*/
	READ_LOCK(2,"读锁"),
	
	/**写锁*/
	WRITE_LOCK(3,"写锁");

	private int code;

	private String name;

	ZkLockType(int code, String name) {
		this.code = code;
		this.name = name;
	}

	public int getCode() {
		return code;
	}

	public String getName() {
		return name;
	}
}
