package cn.structured.datascope.scanner;

import cn.structured.datascope.annotation.DataScopeField;
import cn.structured.datascope.annotation.DataScopeRule;
import cn.structured.datascope.engine.DataRuleEngine;
import cn.structured.datascope.rule.ColumnRule;
import cn.structured.datascope.rule.DataRule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * 数据权限规则扫描器
 * <p>
 * 自动扫描带有 @DataScopeRule 注解的实体类，
 * 从 @DataScopeField 注解中提取列级权限规则并注册到规则引擎
 * </p>
 *
 * @author chuck
 */
@Slf4j
public class DataRuleScanner {

    private final DataRuleEngine ruleEngine;
    private final String[] basePackages;

    public DataRuleScanner(DataRuleEngine ruleEngine, String[] basePackages) {
        this.ruleEngine = ruleEngine;
        this.basePackages = basePackages;
    }

    /**
     * 执行规则扫描和注册
     */
    public void scanAndRegister() {
        if (basePackages == null || basePackages.length == 0) {
            log.warn("No base packages configured for DataRuleScanner, skipping scan");
            return;
        }

        log.info("Starting DataRule scan for packages: {}", Arrays.toString(basePackages));

        ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DataScopeRule.class));

        int registeredCount = 0;

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidates = scanner.findCandidateComponents(basePackage);

            for (BeanDefinition bd : candidates) {
                try {
                    String className = bd.getBeanClassName();
                    Class<?> clazz = Class.forName(className);

                    DataRule rule = buildRuleFromClass(clazz);
                    if (rule != null) {
                        ruleEngine.registerRule(rule);
                        registeredCount++;
                        log.debug("Registered data rule for class: {}", className);
                    }
                } catch (ClassNotFoundException e) {
                    log.warn("Failed to load class for data rule: {}", bd.getBeanClassName(), e);
                }
            }
        }

        log.info("DataRule scan completed, registered {} rules", registeredCount);
    }

    /**
     * 从类构建数据规则
     */
    private DataRule buildRuleFromClass(Class<?> clazz) {
        DataScopeRule classAnnotation = clazz.getAnnotation(DataScopeRule.class);
        if (classAnnotation == null) {
            return null;
        }

        DataRule rule = new DataRule();
        rule.setResource(classAnnotation.resource());

        // 扫描字段注解，构建列级规则
        List<ColumnRule> columnRules = new ArrayList<>();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            DataScopeField fieldAnnotation = field.getAnnotation(DataScopeField.class);
            if (fieldAnnotation != null) {
                ColumnRule columnRule = buildColumnRule(field, fieldAnnotation);
                columnRules.add(columnRule);
            }
        }

        if (!columnRules.isEmpty()) {
            rule.setColumnRules(columnRules);
            log.debug("Built {} column rules for resource: {}", columnRules.size(), rule.getResource());
        }

        return rule;
    }

    /**
     * 从字段注解构建列级规则
     */
    private ColumnRule buildColumnRule(Field field, DataScopeField annotation) {
        ColumnRule rule = new ColumnRule();

        // 字段名称
        String fieldName = annotation.name().isEmpty() ? field.getName() : annotation.name();
        rule.setField(fieldName);

        // 可见性设置
        rule.setVisible(annotation.visible());

        // 角色可见条件
        if (annotation.visibleIfRoleIn().length > 0) {
            rule.setVisibleIfRoleIn(Arrays.asList(annotation.visibleIfRoleIn()));
        }

        // 角色隐藏条件
        if (annotation.hiddenIfRoleIn().length > 0) {
            rule.setHiddenIfRoleIn(Arrays.asList(annotation.hiddenIfRoleIn()));
        }

        // 权限可见条件
        if (annotation.visibleIfPermissionIn().length > 0) {
            rule.setVisibleIfPermissionIn(Arrays.asList(annotation.visibleIfPermissionIn()));
        }

        // 权限隐藏条件
        if (annotation.hiddenIfPermissionIn().length > 0) {
            rule.setHiddenIfPermissionIn(Arrays.asList(annotation.hiddenIfPermissionIn()));
        }

        return rule;
    }
}