package org.apache.james.rrt.lib;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MappingImplTest {

    @Test
    public void hasDomainshouldReturnTrueWhenMappingContainAtMark() {
        assertThat(MappingImpl.of("a@b").hasDomain()).isTrue();
    }
    
    @Test
    public void hasDomainshouldReturnFalseWhenMappingIsEmpty() {
        assertThat(MappingImpl.of("").hasDomain()).isFalse();
    }

    @Test
    public void hasDomainshouldReturnFalseWhenMappingIsBlank() {
        assertThat(MappingImpl.of(" ").hasDomain()).isFalse();
    }

    @Test
    public void hasDomainshouldReturnFalseWhenMappingDoesntContainAtMark() {
        assertThat(MappingImpl.of("abc").hasDomain()).isFalse();
    }
    
    @Test
    public void appendDomainShouldWorkOnValidDomain() {
        assertThat(MappingImpl.of("abc").appendDomain("domain")).isEqualTo(MappingImpl.of("abc@domain"));
    }
    
    @Test
    public void appendDomainShouldWorkWhenMappingAlreadyContainsDomains() {
        assertThat(MappingImpl.of("abc@d").appendDomain("domain")).isEqualTo(MappingImpl.of("abc@d@domain"));
    }
    
    @Test(expected=NullPointerException.class)
    public void appendDomainShouldThrowWhenNullDomain() {
        MappingImpl.of("abc@d").appendDomain(null);
    }
    
    @Test
    public void appendDomainShouldWorkWhenEmptyDomain() {
        assertThat(MappingImpl.of("abc").appendDomain("")).isEqualTo(MappingImpl.of("abc@"));
    }

}
