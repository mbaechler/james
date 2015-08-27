package org.apache.james.rrt.lib;

import java.util.Collection;
import java.util.List;

public interface Mappings extends Iterable<String> {

    Collection<String> getMappings();
    
    void addAll(Mappings toAdd);

    void add(String mapping);

    boolean contains(String mapping);

    int size();

    boolean remove(String mapping);

    void addAll(List<String> target);

    boolean isEmpty();

    String[] toArray(String[] strings);

}