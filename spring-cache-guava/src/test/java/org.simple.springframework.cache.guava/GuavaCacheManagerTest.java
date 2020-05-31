package org.simple.springframework.cache.guava;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GuavaCacheManagerTest {

	private ClassPathXmlApplicationContext ctx;
	@Before
	public void initApplicationContext() {
		ctx = new ClassPathXmlApplicationContext("config.xml");
	}
	@After
	public void closeApplicationContext() {
		if (ctx != null) {
			ctx.close();
			ctx = null;
		}
	}

	@Test
	public void test() {

		DataBean dataBean = (DataBean) ctx.getBean("dataBean");
        long created1= dataBean.getData("d1").createTime;
		long created2= dataBean.getData2("d2").createTime;
		System.out.println("loop 50 times to get d1 and d2, stay cached old creating time");
		for (int i=1; i< 50; i++){
			assertEquals("d1", dataBean.getData("d1").createTime, created1);
			assertEquals("d2", dataBean.getData2("d2").createTime, created2);
		}
		System.out.println("sleep 70 seconds for d1 cache to expire");
		try {
			Thread.sleep(70000);
		}catch (Exception e){
			e.printStackTrace();
		}
		// data 1 cache expired
		assertTrue("d1 expired", dataBean.getData("d1").createTime> created1);
		assertEquals("d2", dataBean.getData2("d2").createTime, created2);
	}

}
