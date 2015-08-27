package org.apache.james.rrt.lib;

import java.util.Collection;

public interface Mappings extends Iterable<String> {

    Collection<String> getMappings();
    
    boolean contains(String mapping);

    int size();

    boolean remove(String mapping);

    boolean isEmpty();

}