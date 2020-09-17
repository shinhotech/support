package com.shinho.support.lock.redisson.annotation;

/**  
 * @author 傅为地
 */
public enum LockType {

	/** 可重入锁*/
	REENTRANT_LOCK(0,"可重入锁"),
	
	/** 公平锁*/
	FAIR_LOCK(1,"公平锁"),
	
	/** 读锁*/
	READ_LOCK(2,"读锁"),
	
	/**写锁*/
	WRITE_LOCK(3,"写锁"),

	/** 红锁*/
	RED_LOCK(4,"红锁"),

	/** 联锁*/
	MULTI_LOCK(5,"联锁");

	private int code;

	private String name;

	LockType(int code, String name) {
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
