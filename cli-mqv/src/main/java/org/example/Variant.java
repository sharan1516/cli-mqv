package org.example;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class Variant {
    private String chromosome;
    private int start;
    private int end;
    private String ref;
    private String alt;
    private vType type;
}
