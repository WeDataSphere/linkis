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

package org.apache.linkis.configuration.restful.api;

import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.configuration.entity.AcrossClusterRule;
import org.apache.linkis.configuration.service.AcrossClusterRuleService;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.utils.ModuleUserUtils;

import org.apache.commons.lang3.StringUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.util.Map;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = "across cluster rule api")
@RestController
@RequestMapping(path = "/configuration/acrossClusterRule")
public class AcrossClusterRuleRestfulApi {

  @Autowired private AcrossClusterRuleService acrossClusterRuleService;

  private Logger log = LoggerFactory.getLogger(this.getClass());

  @ApiOperation(
      value = "get-acrossClusterRule",
      notes = "get acrossClusterRule given creator and user",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "req", dataType = "HttpServletRequest", value = "req"),
    @ApiImplicitParam(name = "creator", dataType = "String", value = "creator"),
    @ApiImplicitParam(name = "user", dataType = "String", value = "user"),
  })
  @RequestMapping(path = "/get-acrossClusterRule", method = RequestMethod.GET)
  public Message getAcrossClusterRule(
      HttpServletRequest req,
      @RequestParam(value = "creator", required = false) String creator,
      @RequestParam(value = "user", required = false) String user) {
    String username = ModuleUserUtils.getOperationUser(req, "execute get-acrossClusterRule");
    if (!Configuration.isAdmin(username)) {
      return Message.error(
          "Failed to get acrossClusterRule,msg: only administrators can configure");
    }

    if (StringUtils.isBlank(creator) || StringUtils.isBlank(user)) {
      return Message.error("Failed to get tenant: Illegal Input Param");
    }

    AcrossClusterRule acrossClusterRule = null;
    try {
      acrossClusterRule = acrossClusterRuleService.getAcrossClusterRule(creator, user);
    } catch (Exception e) {
      return Message.error("get acrossClusterRule failed");
    }

    return Message.ok().data("acrossClusterRule", acrossClusterRule);
  }

  @ApiOperation(
      value = "delete-acrossClusterRule",
      notes = "delete acrossClusterRule given creator and user",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "req", dataType = "HttpServletRequest", value = "req"),
    @ApiImplicitParam(name = "creator", dataType = "String", value = "creator"),
    @ApiImplicitParam(name = "user", dataType = "String", value = "user"),
  })
  @RequestMapping(path = "/delete-acrossClusterRule", method = RequestMethod.GET)
  public Message deleteAcrossClusterRule(
      HttpServletRequest req,
      @RequestParam(value = "creator", required = false) String creator,
      @RequestParam(value = "user", required = false) String user) {
    String username = ModuleUserUtils.getOperationUser(req, "execute delete-acrossClusterRule");
    if (!Configuration.isAdmin(username)) {
      return Message.error(
          "Failed to delete acrossClusterRule,msg: only administrators can configure");
    }

    if (StringUtils.isBlank(creator) || StringUtils.isBlank(user)) {
      return Message.error("Failed to get tenant: Illegal Input Param");
    }

    try {
      acrossClusterRuleService.deleteAcrossClusterRule(creator, user);
    } catch (Exception e) {
      return Message.error("delete acrossClusterRule failed");
    }

    return Message.ok();
  }

  @ApiOperation(
      value = "update-acrossClusterRule",
      notes = "insert acrossClusterRule ",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "req", dataType = "HttpServletRequest", value = ""),
    @ApiImplicitParam(name = "clusterName", dataType = "String", value = "clusterName"),
    @ApiImplicitParam(name = "creator", dataType = "String", value = "creator"),
    @ApiImplicitParam(name = "user", dataType = "String", value = "user"),
    @ApiImplicitParam(name = "rules", dataType = "String", value = "rules"),
  })
  @RequestMapping(path = "/update-acrossClusterRule", method = RequestMethod.PUT)
  public Message updateAcrossClusterRule(
      HttpServletRequest req, @RequestBody Map<String, Object> json) {
    String username = ModuleUserUtils.getOperationUser(req, "execute update-acrossClusterRule");
    if (!Configuration.isAdmin(username)) {
      return Message.error(
          "Failed to update-acrossClusterRule,msg: only administrators can configure");
    }

    String clusterName = (String) json.get("clusterName");
    String creator = (String) json.get("creator");
    String user = (String) json.get("user");
    String rules = (String) json.get("rules");
    if (StringUtils.isBlank(clusterName)
        || StringUtils.isBlank(creator)
        || StringUtils.isBlank(user)
        || StringUtils.isBlank(rules)) {
      return Message.error("Failed to get tenant: Illegal Input Param");
    }

    try {
      acrossClusterRuleService.updateAcrossClusterRule(clusterName, creator, user, username, rules);
    } catch (Exception e) {
      return Message.error("update acrossClusterRule failed");
    }
    return Message.ok();
  }

  @ApiOperation(
      value = "insert-acrossClusterRule",
      notes = "insert acrossClusterRule ",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "req", dataType = "HttpServletRequest", value = ""),
    @ApiImplicitParam(name = "clusterName", dataType = "String", value = "clusterName"),
    @ApiImplicitParam(name = "creator", dataType = "String", value = "creator"),
    @ApiImplicitParam(name = "user", dataType = "String", value = "user"),
    @ApiImplicitParam(name = "rules", dataType = "String", value = "rules"),
  })
  @RequestMapping(path = "/insert-acrossClusterRule", method = RequestMethod.POST)
  public Message insertAcrossClusterRule(
      HttpServletRequest req, @RequestBody Map<String, Object> json) {
    String username = ModuleUserUtils.getOperationUser(req, "execute insert-acrossClusterRule");
    if (!Configuration.isAdmin(username)) {
      return Message.error(
          "Failed to insert-acrossClusterRule,msg: only administrators can configure");
    }

    String clusterName = (String) json.get("clusterName");
    String creator = (String) json.get("creator");
    String user = (String) json.get("user");
    String rules = (String) json.get("rules");
    if (StringUtils.isBlank(clusterName)
        || StringUtils.isBlank(creator)
        || StringUtils.isBlank(user)
        || StringUtils.isBlank(rules)) {
      return Message.error("Failed to get tenant: Illegal Input Param");
    }

    try {
      acrossClusterRuleService.insertAcrossClusterRule(clusterName, creator, user, username, rules);
    } catch (Exception e) {
      return Message.error("insert acrossClusterRule failed");
    }
    return Message.ok();
  }
}
