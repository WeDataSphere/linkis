package org.apache.linkis.configuration.service;

import org.apache.linkis.configuration.entity.AcrossClusterRule;

public interface AcrossClusterRuleService {
    AcrossClusterRule getAcrossClusterRule(String creator, String user) throws Exception;

    void deleteAcrossClusterRule(String creator, String user) throws Exception;

    void updateAcrossClusterRule(String clusterName, String creator, String user, String updateBy, String rules) throws Exception;

    void insertAcrossClusterRule(String clusterName, String creator, String user, String createBy, String rules) throws Exception;
}
