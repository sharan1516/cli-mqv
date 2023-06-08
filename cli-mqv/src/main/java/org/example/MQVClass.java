package org.example;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import lombok.Getter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
public class MQVClass {

    private int total_reads_1;
    private int total_reads_2;
    private int supporting_reads_1;
    private int supporting_reads_2;

    private String alt;
    private String ref;

    private int start;
    private int end;
    private String chr;
    private vType type;

    private List<CigarEntry> altCigarEntries;

    private SamReader reader;


    // Constructor with bam_file path as input
    public MQVClass(String bam_file) {
        this.reader = SamReaderFactory.makeDefault().open(new File(bam_file));
        this.total_reads_1 = -1;
        this.total_reads_2 = -1;
        this.supporting_reads_1 = -1;
        this.supporting_reads_2 = -1;
        this.altCigarEntries = new ArrayList<>();
    }

    public void setReader(SamReader reader) {
        this.reader = reader;
    }

    public void variantSetter(Variant variant) {
        this.alt = (Objects.equals(variant.getAlt(), "."))? "" : variant.getAlt();
        this.ref = (Objects.equals(variant.getRef(), "."))? "" : variant.getRef();
        this.start = variant.getStart();
        this.end = variant.getEnd();
        this.chr = variant.getChromosome();
        this.type = variant.getType();

        // find and store the corresponding reads
        findReads();
    }


    /*
        not required, just for printing purpose
     */
    public void print(){

        int i =0 ;
        for (SAMRecord record : this.reader){

            if (i < 10) {
                System.out.println(record.getReadString());
                System.out.println(record.getReadLength());
                System.out.println(record.getAlignmentStart());
                System.out.println(record.getAlignmentEnd());
                System.out.println(record.getAlignmentEnd() -
                        record.getAlignmentStart() + 1);

                System.out.println(record.getCigarString());
            }
            i++;

            if (record.getCigarString().contains("X") || record.getCigarString().contains("=") ||
                record.getCigarString().contains("S")) {
                System.out.println("There are Xs and =x in the cigar string");
            }
        }
        System.out.println("the print ended here");
    }


    // gets the position of the variant in the read
    public static Pair<Integer,List<CigarEntry>> getPosition(List<CigarEntry> cigarEntries, int basesBeforeVariant){
        int readPtr = 0;
        List<CigarEntry> remCigarEntries = new ArrayList<>();

        String onlyReadAffectedOps = "IS";
        String onlyGenomeAffectedOps = "DN";
        String bothAffectedOps = "M=X";

        for (int i =0 ;i< cigarEntries.size(); i++){
            CigarEntry cigarEntry = cigarEntries.get(i);
            int val = cigarEntry.getValue();
            char op = cigarEntry.getOperator();

            if (basesBeforeVariant == 0) {
                remCigarEntries = cigarEntries.subList(i, cigarEntries.size());
                break;
            }

            if (onlyReadAffectedOps.contains(String.valueOf(op))) {
                readPtr += val;
            }
            else if (bothAffectedOps.contains(String.valueOf(op)) ){

                if (basesBeforeVariant < val) {
                    readPtr += basesBeforeVariant;
                    remCigarEntries = cigarEntries.subList(i+1, cigarEntries.size());
                    remCigarEntries.add(0, new CigarEntry(val - basesBeforeVariant, op));
                    break;
                }
                else {
                    basesBeforeVariant -= val;
                    readPtr += val;
                }

            }
            else if (onlyGenomeAffectedOps.contains(String.valueOf(op))){
                basesBeforeVariant -= val;
            }
        }
        return new Pair<>(readPtr, remCigarEntries);
    }

    /* check if read is supporting read
        if read is supporting read return true
        else return false
    */
    private boolean isSupportingRead(SAMRecord record) {
        String readCIGAR = record.getCigarString();
        String alt = this.alt;
        String read = record.getReadString();

        int readStart = record.getAlignmentStart();
        int readEnd = record.getAlignmentEnd();

        int basesBeforeVariant = this.start - readStart;
        List<CigarEntry> cigarEntries = CigarEntryGen.cigarEntryList(readCIGAR);

        Pair<Integer,List<CigarEntry>> obj = getPosition(cigarEntries, basesBeforeVariant);
        int readPtr = obj.first();
        cigarEntries = obj.second();


        int expectedAltLen = 0;

        // considering X as an operation for substitution
        int flag = cigarEntries.get(0).getOperator() == 'X' ? 1 : 0;

        for (CigarEntry cigarEntry: cigarEntries){
            int val = cigarEntry.getValue();
            char op = cigarEntry.getOperator();
            if (flag == 1){
                if (op == 'X') {
                    expectedAltLen += val;
                    this.altCigarEntries.add(cigarEntry);
                }
                else break;
            }
            else{
                if (op == 'I'){
                    expectedAltLen += val;
                    this.altCigarEntries.add(cigarEntry);
                }
                else if (op == 'D') {
                    this.altCigarEntries.add(cigarEntry);
                }
                else break;
            }
        }

        if (expectedAltLen != alt.length()){
            return false;
        }

        for (int i = readPtr ; i< readPtr + alt.length() ;i++){
            if (read.charAt(i) != alt.charAt(i-readPtr)){
                return false;
            }
        }

        return true;
    }


    // find the total reads and supporting reads
    private void findReads() {
        int total_reads_1 = 0;
        int total_reads_2 = 0;
        int supporting_reads_1 = 0;
        int supporting_reads_2 = 0;

        SAMRecordIterator iterator = reader.queryOverlapping(this.chr, this.start, this.end);

        while (iterator.hasNext()) {
            SAMRecord record = iterator.next();

            // skip unmapped reads, mate unmapped reads, secondary and supplementary alignments
            // to check
            if (record.getReadUnmappedFlag() || record.getMateUnmappedFlag() ||
                    record.isSecondaryOrSupplementary() || record.isSecondaryAlignment()) {
                continue;
            }

            if ( record.getAlignmentStart() <= this.start && record.getAlignmentEnd() >= this.end) {
                total_reads_1++;

                boolean isSRead = isSupportingRead(record);
                if (isSRead)  supporting_reads_1++;

                // check if mapping quality is less than 70 increment total_reads_2
                if (record.getMappingQuality() < 70) {
                    total_reads_2++;
                    if (isSRead)  supporting_reads_2++;
                }

            }
        }
        this.total_reads_1 = total_reads_1;
        this.total_reads_2 = total_reads_2;
        this.supporting_reads_1 = supporting_reads_1;
        this.supporting_reads_2 = supporting_reads_2;

    }


    public float getReadsLoss(){
        return (float) (this.total_reads_1 - this.total_reads_2) / this.total_reads_1;
    }

    public float getSRChange(){
        return (float) (this.supporting_reads_1 - this.supporting_reads_2) / this.supporting_reads_1;
    }


}
