package org.simple.springframework.cache.guava;

import org.springframework.cache.annotation.Cacheable;

public class DataBean {

    @Cacheable(value = "data")
    public Data getData(String name) {
        System.out.println("Create data expire after 1 minute");
        return new Data(name);
    }
    @Cacheable(value = "data2")
    public Data getData2(String name) {
        System.out.println("Create data expire after 2 minute");
        return new Data(name);
    }

    protected class Data {
        public Data(String name) {
            this.name = name;
            this.createTime=System.currentTimeMillis();
        }
        private String name;
        protected long createTime;
    }
}
