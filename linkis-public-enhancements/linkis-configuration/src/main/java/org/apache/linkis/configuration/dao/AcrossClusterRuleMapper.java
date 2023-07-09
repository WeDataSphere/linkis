package org.apache.linkis.configuration.dao;

import org.apache.ibatis.annotations.Param;
import org.apache.linkis.configuration.entity.AcrossClusterRule;


public interface AcrossClusterRuleMapper {

    AcrossClusterRule getAcrossClusterRule(@Param("creator") String creator, @Param("user") String user);

    void deleteAcrossClusterRule(@Param("creator") String creator, @Param("user") String user);

    void updateAcrossClusterRule(@Param("acrossClusterRule") AcrossClusterRule acrossClusterRule);

    void insertAcrossClusterRule(@Param("acrossClusterRule") AcrossClusterRule acrossClusterRule);

}
