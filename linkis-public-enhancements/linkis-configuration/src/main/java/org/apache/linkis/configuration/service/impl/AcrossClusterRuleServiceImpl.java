/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.configuration.service.impl;

import org.apache.linkis.configuration.dao.AcrossClusterRuleMapper;
import org.apache.linkis.configuration.entity.AcrossClusterRule;
import org.apache.linkis.configuration.service.AcrossClusterRuleService;
import org.apache.linkis.governance.common.constant.job.JobRequestConstants;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AcrossClusterRuleServiceImpl implements AcrossClusterRuleService {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  @Autowired private AcrossClusterRuleMapper ruleMapper;

  @Override
  public void deleteAcrossClusterRule(String creator, String user) throws Exception {
    ruleMapper.deleteAcrossClusterRule(creator, user);
    logger.info("delete acrossClusterRule success");
    return;
  }

  @Override
  public void updateAcrossClusterRule(
      String id,
      String clusterName,
      String creator,
      String user,
      String updateBy,
      String rules,
      String isValid)
      throws Exception {
    Long ruleId = new Long(id);
    AcrossClusterRule acrossClusterRule = ruleMapper.getAcrossClusterRule(ruleId);
    if (acrossClusterRule == null) {
      logger.info("acrossClusterRule not exit");
      throw new Exception("acrossClusterRule not exit");
    }

    Date time = new Date();
    acrossClusterRule.setClusterName(clusterName);
    acrossClusterRule.setCreator(creator);
    acrossClusterRule.setUser(user);
    acrossClusterRule.setUpdateTime(time);
    acrossClusterRule.setUpdateBy(updateBy);
    acrossClusterRule.setRules(rules);
    acrossClusterRule.setIsValid(isValid);
    ruleMapper.updateAcrossClusterRule(acrossClusterRule);
    logger.info("update acrossClusterRule success");
    return;
  }

  @Override
  public void insertAcrossClusterRule(
      String clusterName,
      String creator,
      String user,
      String createBy,
      String rules,
      String isValid)
      throws Exception {
    Date time = new Date();
    AcrossClusterRule acrossClusterRule = new AcrossClusterRule();
    acrossClusterRule.setClusterName(clusterName);
    acrossClusterRule.setCreator(creator);
    acrossClusterRule.setUser(user);
    acrossClusterRule.setCreateBy(createBy);
    acrossClusterRule.setCreateTime(time);
    acrossClusterRule.setUpdateBy(createBy);
    acrossClusterRule.setUpdateTime(time);
    acrossClusterRule.setRules(rules);
    acrossClusterRule.setIsValid(isValid);
    ruleMapper.insertAcrossClusterRule(acrossClusterRule);
    logger.info("insert acrossClusterRule success");
    return;
  }

  @Override
  public Map<String, Object> queryAcrossClusterRuleList(
      String creator, String user, String clusterName, Integer pageNow, Integer pageSize) {
    Map<String, Object> result = new HashMap<>(2);
    List<AcrossClusterRule> acrossClusterRules = null;
    if (Objects.isNull(pageNow)) {
      pageNow = 1;
    }
    if (Objects.isNull(pageSize)) {
      pageSize = 20;
    }
    PageHelper.startPage(pageNow, pageSize);

    try {
      acrossClusterRules = ruleMapper.queryAcrossClusterRuleList(user, creator, clusterName);
    } finally {
      PageHelper.clearPage();
    }
    PageInfo<AcrossClusterRule> pageInfo = new PageInfo<>(acrossClusterRules);
    result.put("acrossClusterRuleList", acrossClusterRules);
    result.put(JobRequestConstants.TOTAL_PAGE(), pageInfo.getTotal());
    return result;
  }
}
