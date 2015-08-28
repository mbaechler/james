package org.apache.james.rrt.lib;


public interface Mapping {

    String asString();

    boolean hasDomain();

    Mapping appendDomain(String domain);

}