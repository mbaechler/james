package org.apache.james.rrt.lib;

import java.util.Collection;

public interface Mappings extends Iterable<String> {

    Collection<String> getMappings();
    
    void addAll(Mappings toAdd);

    void add(String mapping);

    boolean contains(String mapping);

    int size();

    boolean remove(String mapping);

    boolean isEmpty();

}