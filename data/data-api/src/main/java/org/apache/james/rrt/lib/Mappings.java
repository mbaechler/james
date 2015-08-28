package org.apache.james.rrt.lib;


public interface Mappings {

    boolean contains(String mapping);

    int size();

    Mappings remove(String mapping);

    boolean isEmpty();

    Iterable<String> asStrings();
    
}