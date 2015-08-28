package org.apache.james.rrt.lib;

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;


public class MappingImpl implements Mapping {

    public static MappingImpl of(String mapping) {
        return new MappingImpl(mapping);
    }

    private final String mapping;

    public MappingImpl(String mapping) {
        this.mapping = mapping;
    }
    
    @Override
    public String asString() {
        return mapping;
    }
    
    @Override
    public boolean hasDomain() {
        return mapping.contains("@");
    }
    
    @Override
    public Mapping appendDomain(String domain) {
        Preconditions.checkNotNull(domain);
        return new MappingImpl(mapping + "@" + domain);
    }
    
    @Override
    public boolean equals(Object other) {
        if (other instanceof MappingImpl) {
            MappingImpl otherMapping = (MappingImpl) other;
            return Objects.equal(mapping, otherMapping.mapping);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(mapping);
    }
    
}