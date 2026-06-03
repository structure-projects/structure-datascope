package cn.structured.datascope.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ColumnRule {

    private String field;
    
    private boolean visible = true;
    
    private List<String> visibleIfRoleIn = new ArrayList<>();
    
    private List<String> hiddenIfRoleIn = new ArrayList<>();
}
