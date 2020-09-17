package com.shinho.support.lock.zookeeper.repository;

import com.shinho.support.lock.zookeeper.properties.ZookeeperLockProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**  
 * @author 傅为地
 * zookeeper分布式锁实现类
 */

@Slf4j
public class ZookeeperLock {
	
	private CuratorFramework curatorFramework;

	private ZookeeperLockProperties zookeeperLockProperties;

	private static final  String zookeeperLockPrefix="/zookeeper_lock/";

	private  volatile  Map<Long, InterProcessMutex> lockMap = new ConcurrentHashMap<Long, InterProcessMutex>();

	private  volatile  Map<Long, InterProcessSemaphoreMutex> lockSemaphoreMap = new ConcurrentHashMap<Long, InterProcessSemaphoreMutex>();

	public ZookeeperLock(CuratorFramework curatorFramework, ZookeeperLockProperties zookeeperLockProperties) {
		this.curatorFramework = curatorFramework;
		this.zookeeperLockProperties = zookeeperLockProperties;
	}

	/**
	 * 获取锁操作
	 * @param key  加锁key
	 * @return lock
	 */
	public boolean lock(String key) {
		boolean lock =false;
		synchronized(key+0){
			long threadId = Thread.currentThread().getId();
			try {
				//可重入锁
				InterProcessMutex interProcessMutex = null;
				if(lockMap.containsKey(threadId)){
					interProcessMutex = lockMap.get(threadId);
				} else{
					interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+0);
					lockMap.put(threadId, interProcessMutex);
				}
				lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
				if(lock){
					log.debug("zookeeper locker with thread:[ "+threadId + " ] hold reentrant lock success key:[" + zookeeperLockPrefix + key+0+"] lock type:["+0+"]");
				}
				return lock;
			} catch (Exception e) {
				lockMap.remove(threadId);
				log.error("zookeeper locker acquire a lock with error:{}",e);
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * 获取锁操作
	 * @param key  加锁key
	 * @param type 锁类型 0：可重入互斥锁 ,1：互斥不可重入锁 ，2:读锁 ，3:写锁
	 * @return lock
	 */
	public boolean lock(String key, int type) {
		boolean lock =false;
		synchronized(key+type){
			long threadId = Thread.currentThread().getId();
			try {
				if(type==0){
					//可重入锁
					InterProcessMutex interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold reentrant lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==1){
					//不可重入排他锁
					InterProcessSemaphoreMutex interProcessMutex = null;
					if(lockSemaphoreMap.containsKey(threadId)){
						interProcessMutex = lockSemaphoreMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessSemaphoreMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockSemaphoreMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold semaphore lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==2){
					//读锁
					InterProcessMutex  interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessReadWriteLock(curatorFramework, zookeeperLockPrefix + key+type).readLock();
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold read lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==3){
					//写锁
					InterProcessMutex  interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessReadWriteLock(curatorFramework, zookeeperLockPrefix + key+type).writeLock();
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold write lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}
				else{
					//可重入锁
					type=0;
					//可重入锁
					InterProcessMutex interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(zookeeperLockProperties.getZk().getHoldTime(), TimeUnit.SECONDS);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold reentrant lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}
				return lock;
			} catch (Exception e) {
				if(type==1){
					lockSemaphoreMap.remove(threadId);
				}else{
					lockMap.remove(threadId);
				}
				log.error("zookeeper locker acquire a lock with error:{}",e);
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * 获取锁操作
	 * @param key  加锁key
	 * @param type 锁类型 0：可重入互斥锁 ,1：互斥不可重入锁 ，2:读锁 ，3:写锁
	 * @param expire 过期时间数量
	 * @param unit   过期时间单位
	 * @return lock
	 */
	public boolean lock(String key, int type,long expire,TimeUnit unit) {
		boolean lock =false;
		synchronized(key+type){
			long threadId = Thread.currentThread().getId();
			try {
				if(type==0){
					//可重入锁
					InterProcessMutex interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(expire, unit);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold reentrant lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==1){
					//不可重入排他锁
					InterProcessSemaphoreMutex interProcessMutex = null;
					if(lockSemaphoreMap.containsKey(threadId)){
						interProcessMutex = lockSemaphoreMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessSemaphoreMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockSemaphoreMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(expire, unit);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold semaphore lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==2){
					//读锁
					InterProcessMutex  interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessReadWriteLock(curatorFramework, zookeeperLockPrefix + key+type).readLock();
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(expire, unit);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold read lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}else if(type==3){
					//写锁
					InterProcessMutex  interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessReadWriteLock(curatorFramework, zookeeperLockPrefix + key+type).writeLock();
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(expire, unit);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold write lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}
				else{
					//可重入锁
					type=0;
					//可重入锁
					InterProcessMutex interProcessMutex = null;
					if(lockMap.containsKey(threadId)){
						interProcessMutex = lockMap.get(threadId);
					} else{
						interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+type);
						lockMap.put(threadId, interProcessMutex);
					}
					lock = interProcessMutex.acquire(expire, unit);
					if(lock){
						log.debug("zookeeper locker with thread:[ "+threadId + " ] hold reentrant lock success key:[" + zookeeperLockPrefix + key+type+"] lock type:["+type+"]");
					}
				}
				return lock;
			} catch (Exception e) {
				if(type==1){
					lockSemaphoreMap.remove(threadId);
				}else{
					lockMap.remove(threadId);
				}
				log.error("zookeeper locker acquire a lock with error:{}",e);
				throw new RuntimeException(e);
			}
		}
	}


	/**
	 * 释放锁
	 * @param key 加锁key
	 * @param type 锁类型 0：可重入互斥锁 ,1：互斥不可重入锁 ，2:读锁 ，3:写锁	 *
	 * @return lock
	 */
	public boolean releaseLock(String key,int type) {
		long threadId = Thread.currentThread().getId();
		try {
			//互斥不可重入锁
			if(type==1) {
				InterProcessSemaphoreMutex interProcessMutex = null;
				if (lockSemaphoreMap.containsKey(threadId)) {
					interProcessMutex = lockSemaphoreMap.get(threadId);
					lockSemaphoreMap.remove(threadId);
				} else {
					interProcessMutex = new InterProcessSemaphoreMutex(curatorFramework, zookeeperLockPrefix + key+type);
				}
				interProcessMutex.release();
			}else{//0：可重入互斥锁 ，2:读锁 ，3:写锁
				InterProcessMutex interProcessMutex = null;
				if (lockMap.containsKey(threadId)) {
					interProcessMutex = lockMap.get(threadId);
					lockMap.remove(threadId);
				} else {
					interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key+type);
				}
				interProcessMutex.release();
			}
			log.debug("zookeeper locker with thread[ " + threadId + " ] release lock success! key:[" + zookeeperLockPrefix + key+type + "]");
		} catch (Exception e) {
			e.printStackTrace();
			log.error("zookeeper locker releaseLock with an error:{}", e);
		} finally {
			if(type==1){
				lockSemaphoreMap.remove(threadId);
			}else{
				lockMap.remove(threadId);
			}
		}
		return true;
	}


	/**
	 * 释放锁
	 * @param key 加锁key
	 * @return lock
	 */
	public boolean releaseLock(String key) {
		long threadId = Thread.currentThread().getId();
		try {
			InterProcessMutex interProcessMutex = null;
			if (lockMap.containsKey(threadId)) {
				interProcessMutex = lockMap.get(threadId);
				lockMap.remove(threadId);
			} else {
				interProcessMutex = new InterProcessMutex(curatorFramework, zookeeperLockPrefix + key + 0);
			}
			interProcessMutex.release();
			log.debug("zookeeper locker with thread[ " + threadId + " ] release lock success! key:[" + zookeeperLockPrefix + key + 0 + "]");
		} catch (Exception e) {
			e.printStackTrace();
			log.error("zookeeper locker releaseLock with an error:{}", e);
		} finally {
			lockMap.remove(threadId);
		}
		return true;
	}

}
