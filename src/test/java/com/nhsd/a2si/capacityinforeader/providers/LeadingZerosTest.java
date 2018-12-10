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

    @Test
    public void string_zero_only_becomes_null() {
        assertThat(LeadingZeros.strip("0"), nullValue());
    }

    @Test
    public void string_numerous_zeros_become_null() {
        assertThat(LeadingZeros.strip("0000000"), nullValue());
    }

    @Test
    public void empty_string_becomes_empty_string() {
        assertThat(LeadingZeros.strip(""), is(""));
    }

}