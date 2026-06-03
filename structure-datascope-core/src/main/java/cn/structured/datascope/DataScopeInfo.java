package cn.structured.datascope;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DataScopeInfo {

    private String dataScopeId;
    
    private List<String> roles = new ArrayList<>();
    
    private String orgId;
    
    private List<String> deptIds = new ArrayList<>();
    
    private String userId;
}
