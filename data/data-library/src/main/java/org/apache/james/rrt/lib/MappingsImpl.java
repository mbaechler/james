package org.apache.james.rrt.lib;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class MappingsImpl implements Mappings {

    public static MappingsImpl empty() {
        return new MappingsImpl(new ArrayList<String>());
    }
    
    public static MappingsImpl fromRawString(String raw) {
        return new MappingsImpl(mappingToCollection(raw));
    }
    
    private static ArrayList<String> mappingToCollection(String rawMapping) {
        ArrayList<String> map = new ArrayList<String>();
        StringTokenizer tokenizer = new StringTokenizer(rawMapping, RecipientRewriteTableUtil.getSeparator(rawMapping));
        while (tokenizer.hasMoreTokens()) {
            final String raw = tokenizer.nextToken().trim();
            map.add(raw);
        }
        return map;
    }
    
    public static Mappings fromCollection(Collection<String> mappings) {
        return new MappingsImpl(mappings);
    }
    
    public static Builder from(Mappings from) {
        Builder builder = new Builder();
        builder.addAll(from);
        return builder;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        
        private final ImmutableList.Builder<String> mappings;
        
        private Builder() {
            mappings = ImmutableList.builder();
        }

        public Builder add(String mapping) {
            mappings.add(mapping);
            return this;
        }

        public Builder addAll(Mappings mappings) {
            this.mappings.addAll(mappings);
            return this;
        }
        
        public Mappings build() {
            return new MappingsImpl(mappings.build());
        }
        
    }
    
    private final ImmutableList<String> mappings;

    private MappingsImpl(Collection<String> mappings) {
        this.mappings = ImmutableList.copyOf(mappings);
    }
    
    @Override
    public Iterator<String> iterator() {
        return mappings.iterator();
    }

    @Override
    public boolean contains(String mapping) {
        return mappings.contains(mapping);
    }

    @Override
    public int size() {
        return mappings.size();
    }

    @Override
    public Mappings remove(String mapping) {
        if (mappings.contains(mapping)) {
            ArrayList<String> updatedMappings = Lists.newArrayList(mappings);
            updatedMappings.remove(mapping);
            return MappingsImpl.fromCollection(updatedMappings);
        }
        return this;
    }

    @Override
    public boolean isEmpty() {
        return mappings.isEmpty();
    }

}