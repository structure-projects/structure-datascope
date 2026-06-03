package cn.structured.datascope.rule;

import lombok.Data;

@Data
public class RowRule {

    private String field;
    
    private String op;
    
    private Object value;
}
