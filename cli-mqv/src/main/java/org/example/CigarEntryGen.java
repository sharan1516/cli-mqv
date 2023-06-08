package org.example;

import java.util.ArrayList;
import java.util.List;


public class CigarEntryGen {


    // validate cigar strings
    /*
        input: cigar string
        output: boolean
     */
    private static boolean checkCigar(String cigar){
        String ops = "MINDSHPX=0123456789";

        for (int i =0;i<cigar.length();i++){
            if (!ops.contains(String.valueOf(cigar.charAt(i)))){
                return false;
            }
        }
        return true;

    }

    // generate cigar entries of the cigar string
    /*
        input: cigar string
        output: list of cigar entries
     */
    public static List<CigarEntry> cigarEntryList(String cigarString){
        StringBuilder num = new StringBuilder();
        List<CigarEntry> cigarEntries = new ArrayList<>();

        String ops = "MINDSHPX=";

        assert checkCigar(cigarString) : "Invalid Cigar String";

        for (int i =0;i<cigarString.length();i++){
            if (ops.contains(String.valueOf(cigarString.charAt(i)))){
                int value = Integer.parseInt(num.toString());
                char operator = cigarString.charAt(i);
                CigarEntry cigarEntry = new CigarEntry(value, operator);
                cigarEntries.add(cigarEntry);
                num = new StringBuilder();
            }
            else {
                num.append(cigarString.charAt(i));
            }
        }
        return cigarEntries;
    }

}
