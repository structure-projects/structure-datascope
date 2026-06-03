package cn.structured.datascope.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataRule {

    private String resource;
    
    private List<RowRule> rowRules = new ArrayList<>();
    
    private List<ColumnRule> columnRules = new ArrayList<>();
}
