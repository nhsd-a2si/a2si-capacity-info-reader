package com.nhsd.a2si.capacityinforeader.providers;

public interface LeadingZeros {

    /**
     * Strips leading zeros from a string that holds a number.
     *
     * @implNote As this methods use is expected to be minimal, 1 or 2 leading zeros here and there, this method uses recursion.
     *
     * @param integer String representation of an integer
     * @return The input string with any leading zeros removed
     */
    static String strip(String integer){
        if(integer.charAt(0) == '0'){
            return LeadingZeros.strip(integer.substring(1));
        }
        return integer;
    }

}
