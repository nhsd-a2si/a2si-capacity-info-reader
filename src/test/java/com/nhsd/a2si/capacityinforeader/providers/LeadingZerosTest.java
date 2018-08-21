package com.nhsd.a2si.capacityinforeader.providers;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.*;

public class LeadingZerosTest {

    @Test
    public void stirng_no_zero() {
        assertThat(LeadingZeros.strip("123"), is("123"));
    }

    @Test
    public void string_with_zero() {
        assertThat(LeadingZeros.strip("0123"), is("123"));
    }

    @Test
    public void string_with_2_zero() {
        assertThat(LeadingZeros.strip("00123"), is("123"));
    }

    @Test
    public void string_with_many_zero() {
        assertThat(LeadingZeros.strip("0000000000000000000000000000123"), is("123"));
    }

}