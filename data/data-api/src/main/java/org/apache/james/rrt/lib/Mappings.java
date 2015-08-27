package org.apache.james.rrt.lib;

import java.util.Collection;

public interface Mappings extends Iterable<String> {

    Collection<String> getMappings();
    
    Mappings addAll(Mappings toAdd);

    boolean contains(String mapping);

    int size();

    boolean remove(String mapping);

    boolean isEmpty();

}