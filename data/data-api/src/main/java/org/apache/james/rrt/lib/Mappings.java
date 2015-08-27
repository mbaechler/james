package org.apache.james.rrt.lib;


public interface Mappings extends Iterable<String> {

    boolean contains(String mapping);

    int size();

    Mappings remove(String mapping);

    boolean isEmpty();

}