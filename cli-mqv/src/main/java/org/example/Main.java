package org.example;

import java.util.List;

// Press Shift twice to open the Search Everywhere dialog and type `show whitespaces`,
// then press Enter. You can now see whitespace characters in your code.
public class Main {

    // checking the entries of the cigar string
    public static void testCigarEntryGen(){
        String cigarString = "10M1I10M1D10M5S6H7N8P9=10X";
        List<CigarEntry> cigarEntries = CigarEntryGen.cigarEntryList(cigarString);

        System.out.println("The generated cigar string");
        for (CigarEntry cigarEntry : cigarEntries){
            System.out.print(cigarEntry.getValue());
            System.out.print(cigarEntry.getOperator());
            System.out.print(" ");
        }

        System.out.println("\nThe original cigar string");
        System.out.println(cigarString);
    }

    // testing the getPosition method
    public static void testGetPosition(){
        String cigarString = "10M1I10M2D10M";
        List<CigarEntry> cigarEntries = CigarEntryGen.cigarEntryList(cigarString);

        Pair<Integer,List<CigarEntry>> obj = MQVClass.getPosition(cigarEntries, 22);

        cigarEntries = obj.second();

        System.out.println("The Position "+ obj.first());

        System.out.println("Cigar String after finding the position");
        for (CigarEntry cigarEntry: cigarEntries){
            System.out.print(cigarEntry.getValue());
            System.out.print(cigarEntry.getOperator());
        }

    }

    public static void testMQVClass(){
        MQVClass mqvClass = new MQVClass("/home/cicada/sls/cli-mqv/src/main/resources/mapt.NA12156.altex.bam");
        Variant variant = new Variant("chr17", 43971748, 43971748, "", "G", vType.SNV);
        mqvClass.variantSetter(variant);
        mqvClass.print();

    }


    public static void main(String[] args) {
         testCigarEntryGen();
//         testGetPosition();
//        testMQVClass();
    }
}