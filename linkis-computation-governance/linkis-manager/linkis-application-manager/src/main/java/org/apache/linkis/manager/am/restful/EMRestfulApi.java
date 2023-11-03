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

package org.apache.linkis.manager.am.restful;

import org.apache.linkis.common.ServiceInstance;
import org.apache.linkis.common.conf.Configuration;
import org.apache.linkis.common.utils.ByteTimeUtils;
import org.apache.linkis.common.utils.JsonUtils;
import org.apache.linkis.manager.am.conf.AMConfiguration;
import org.apache.linkis.manager.am.converter.DefaultMetricsConverter;
import org.apache.linkis.manager.am.exception.AMErrorCode;
import org.apache.linkis.manager.am.exception.AMErrorException;
import org.apache.linkis.manager.am.manager.EngineNodeManager;
import org.apache.linkis.manager.am.service.ECResourceInfoService;
import org.apache.linkis.manager.am.service.em.ECMOperateService;
import org.apache.linkis.manager.am.service.em.EMInfoService;
import org.apache.linkis.manager.am.util.EMUtils;
import org.apache.linkis.manager.am.utils.AMUtils;
import org.apache.linkis.manager.am.vo.ConfigVo;
import org.apache.linkis.manager.am.vo.EMNodeVo;
import org.apache.linkis.manager.common.entity.enumeration.NodeHealthy;
import org.apache.linkis.manager.common.entity.metrics.NodeHealthyInfo;
import org.apache.linkis.manager.common.entity.node.EMNode;
import org.apache.linkis.manager.common.entity.node.EngineNode;
import org.apache.linkis.manager.common.entity.persistence.ECResourceInfoRecord;
import org.apache.linkis.manager.common.entity.persistence.PersistenceLabelRel;
import org.apache.linkis.manager.common.entity.persistence.PersistenceResource;
import org.apache.linkis.manager.common.entity.resource.NodeResource;
import org.apache.linkis.manager.common.entity.resource.ResourceType;
import org.apache.linkis.manager.common.entity.resource.YarnResource;
import org.apache.linkis.manager.common.exception.RMErrorException;
import org.apache.linkis.manager.common.protocol.OperateRequest$;
import org.apache.linkis.manager.common.protocol.em.ECMOperateRequest;
import org.apache.linkis.manager.common.protocol.em.ECMOperateRequest$;
import org.apache.linkis.manager.common.protocol.em.ECMOperateResponse;
import org.apache.linkis.manager.exception.PersistenceErrorException;
import org.apache.linkis.manager.label.LabelManagerUtils;
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactory;
import org.apache.linkis.manager.label.builder.factory.LabelBuilderFactoryContext;
import org.apache.linkis.manager.label.entity.Label;
import org.apache.linkis.manager.label.entity.UserModifiable;
import org.apache.linkis.manager.label.entity.cluster.ClusterLabel;
import org.apache.linkis.manager.label.exception.LabelErrorException;
import org.apache.linkis.manager.label.service.NodeLabelService;
import org.apache.linkis.manager.persistence.LabelManagerPersistence;
import org.apache.linkis.manager.persistence.ResourceManagerPersistence;
import org.apache.linkis.manager.rm.domain.RMLabelContainer;
import org.apache.linkis.manager.rm.external.service.ExternalResourceService;
import org.apache.linkis.manager.rm.external.yarn.YarnResourceIdentifier;
import org.apache.linkis.manager.rm.restful.vo.UserResourceVo;
import org.apache.linkis.manager.rm.utils.RMUtils;
import org.apache.linkis.server.Message;
import org.apache.linkis.server.utils.ModuleUserUtils;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.github.xiaoymin.knife4j.annotations.ApiOperationSupport;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Api(tags = "ECM(engineconnmanager) operation")
@RequestMapping(
    path = "/linkisManager",
    produces = {"application/json"})
@RestController
public class EMRestfulApi {

  public static final String KEY_TENANT = "tenant";

  @Autowired private EMInfoService emInfoService;

  @Autowired private NodeLabelService nodeLabelService;

  @Autowired private DefaultMetricsConverter defaultMetricsConverter;

  @Autowired private EngineNodeManager engineNodeManager;

  @Autowired private ECMOperateService ecmOperateService;

  @Autowired private ECResourceInfoService ecResourceInfoService;

  @Autowired private ResourceManagerPersistence resourceManagerPersistence;

  @Autowired private LabelManagerPersistence labelManagerPersistence;

  @Autowired private ExternalResourceService externalResourceService;

  private LabelBuilderFactory stdLabelBuilderFactory =
      LabelBuilderFactoryContext.getLabelBuilderFactory();

  private Logger logger = LoggerFactory.getLogger(EMRestfulApi.class);

  private String[] adminOperations = AMConfiguration.ECM_ADMIN_OPERATIONS().getValue().split(",");

  private void checkAdmin(String userName) throws AMErrorException {
    if (Configuration.isNotAdmin(userName)) {
      throw new AMErrorException(210003, "Only admin can modify ECMs(只有管理员才能修改ECM).");
    }
  }

  @ApiOperation(value = "listAllEMs", notes = "get all ECM service list", response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "instance", dataType = "String", value = "Instance"),
    @ApiImplicitParam(
        name = "nodeHealthy",
        dataType = "String",
        value = "node  healthy status",
        example = "Healthy, UnHealthy, WARN, StockAvailable, StockUnavailable"),
    @ApiImplicitParam(name = "owner", dataType = "String", value = "Owner"),
    @ApiImplicitParam(name = "tenantLabel", dataType = "String", value = "tenantLabel like")
  })
  // todo add healthInfo
  @RequestMapping(path = "/listAllEMs", method = RequestMethod.GET)
  public Message listAllEMs(
      HttpServletRequest req,
      @RequestParam(value = "instance", required = false) String instance,
      @RequestParam(value = "nodeHealthy", required = false) String nodeHealthy,
      @RequestParam(value = "owner", required = false) String owner,
      @RequestParam(value = "tenantLabel", required = false) String tenantLabel)
      throws AMErrorException {
    checkAdmin(ModuleUserUtils.getOperationUser(req, "listAllEMs"));
    List<EMNodeVo> emNodeVos = AMUtils.copyToEMVo(emInfoService.getAllEM());
    if (CollectionUtils.isNotEmpty(emNodeVos)) {
      Stream<EMNodeVo> stream = emNodeVos.stream();
      if (StringUtils.isNotBlank(instance)) {
        stream = stream.filter(em -> em.getInstance().contains(instance));
      }
      if (StringUtils.isNotBlank(nodeHealthy)) {
        stream =
            stream.filter(
                em ->
                    em.getNodeHealthy() == null
                        || em.getNodeHealthy().equals(NodeHealthy.valueOf(nodeHealthy)));
      }
      if (StringUtils.isNotBlank(owner)) {
        stream = stream.filter(em -> em.getOwner().equalsIgnoreCase(owner));
      }

      if (StringUtils.isNotBlank(tenantLabel)) {
        stream =
            stream.filter(
                em -> {
                  List<Label> labels = em.getLabels();
                  labels =
                      labels.stream()
                          .filter(
                              label ->
                                  KEY_TENANT.equals(label.getLabelKey())
                                      && label.getStringValue().contains(tenantLabel))
                          .collect(Collectors.toList());
                  return labels.size() > 0 ? true : false;
                });
      }

      emNodeVos = stream.collect(Collectors.toList());

      // sort
      if (StringUtils.isNotBlank(tenantLabel)) {
        Collections.sort(
            emNodeVos,
            new Comparator<EMNodeVo>() {
              @Override
              public int compare(EMNodeVo a, EMNodeVo b) {
                String aLabelStr =
                    a.getLabels().stream()
                        .filter(label -> KEY_TENANT.equals(label.getLabelKey()))
                        .collect(Collectors.toList())
                        .get(0)
                        .getStringValue();
                String bLabelStr =
                    b.getLabels().stream()
                        .filter(label -> KEY_TENANT.equals(label.getLabelKey()))
                        .collect(Collectors.toList())
                        .get(0)
                        .getStringValue();
                return aLabelStr.compareTo(bLabelStr);
              }
            });
      } else {
        Collections.sort(emNodeVos, Comparator.comparing(EMNodeVo::getInstance));
      }
    }
    return Message.ok().data("EMs", emNodeVos);
  }

  @ApiOperation(
      value = "listAllECMHealthyStatus",
      notes = "get all ECM healthy status",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "onlyEditable", dataType = "Boolean", value = "only editable")
  })
  @RequestMapping(path = "/listAllECMHealthyStatus", method = RequestMethod.GET)
  public Message listAllNodeHealthyStatus(
      HttpServletRequest req,
      @RequestParam(value = "onlyEditable", required = false) Boolean onlyEditable) {
    NodeHealthy[] nodeHealthy = NodeHealthy.values();
    if (onlyEditable) {
      nodeHealthy =
          new NodeHealthy[] {
            NodeHealthy.Healthy,
            NodeHealthy.UnHealthy,
            NodeHealthy.WARN,
            NodeHealthy.StockAvailable,
            NodeHealthy.StockUnavailable
          };
    }
    return Message.ok().data("nodeHealthy", nodeHealthy);
  }

  @ApiOperation(value = "modifyEMInfo", notes = "modify ECM info", response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "applicationName",
        dataType = "String",
        example = "linkis-cg-engineconnmanager"),
    @ApiImplicitParam(
        name = "emStatus",
        dataType = "String",
        example = "Healthy, UnHealthy, WARN, StockAvailable, StockUnavailable"),
    @ApiImplicitParam(name = "instance", dataType = "String", example = "bdp110:9102"),
    @ApiImplicitParam(name = "labels", dataType = "List", value = "Labels"),
    @ApiImplicitParam(name = "labelKey", dataType = "String", example = "emInstance"),
    @ApiImplicitParam(
        name = "stringValue",
        dataType = "String",
        example = "linkis-cg-engineconn-bdp110:12295")
  })
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "/modifyEMInfo", method = RequestMethod.PUT)
  @Transactional(rollbackFor = Exception.class)
  public Message modifyEMInfo(HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws AMErrorException, LabelErrorException {
    String username = ModuleUserUtils.getOperationUser(req, "modifyEMInfo");
    checkAdmin(username);
    String applicationName = jsonNode.get("applicationName").asText();
    String instance = jsonNode.get("instance").asText();
    if (StringUtils.isEmpty(applicationName)) {
      throw new AMErrorException(
          AMErrorCode.QUERY_PARAM_NULL.getErrorCode(),
          "applicationName cannot be null(请求参数applicationName不能为空)");
    }
    if (StringUtils.isEmpty(instance)) {
      throw new AMErrorException(
          AMErrorCode.QUERY_PARAM_NULL.getErrorCode(), "instance cannot be null(请求参数instance不能为空)");
    }
    ServiceInstance serviceInstance = ServiceInstance.apply(applicationName, instance);
    if (serviceInstance == null) {
      throw new AMErrorException(
          AMErrorCode.QUERY_PARAM_NULL.getErrorCode(),
          "serviceInstance:" + applicationName + " non-existent(" + applicationName + ")");
    }
    String healthyStatus = jsonNode.get("emStatus").asText();
    if (healthyStatus != null) {
      NodeHealthyInfo nodeHealthyInfo = new NodeHealthyInfo();
      nodeHealthyInfo.setNodeHealthy(NodeHealthy.valueOf(healthyStatus));
      emInfoService.updateEMInfo(serviceInstance, nodeHealthyInfo);
    }
    JsonNode labels = jsonNode.get("labels");
    Set<String> labelKeySet = new HashSet<>();
    if (labels != null) {
      ArrayList<Label<?>> newLabelList = new ArrayList<>();
      Iterator<JsonNode> iterator = labels.iterator();
      while (iterator.hasNext()) {
        JsonNode label = iterator.next();
        String labelKey = label.get("labelKey").asText();
        String stringValue = label.get("stringValue").asText();
        Label newLabel = stdLabelBuilderFactory.createLabel(labelKey, stringValue);
        if (newLabel instanceof UserModifiable) {
          ((UserModifiable) newLabel).valueCheck(stringValue);
        }
        labelKeySet.add(labelKey);
        newLabelList.add(newLabel);
      }
      if (labelKeySet.size() != newLabelList.size()) {
        throw new AMErrorException(
            210003, "Failed to update label, include repeat labels(更新label失败，包含重复label)");
      }
      nodeLabelService.updateLabelsToNode(serviceInstance, newLabelList);
      logger.info("success to update label of instance: " + serviceInstance.getInstance());
    }
    return Message.ok("success");
  }

  @ApiOperation(
      value = "executeECMOperationByEC",
      notes = "EC execute ECM operation",
      response = Message.class)
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "/executeECMOperationByEC", method = RequestMethod.POST)
  public Message executeECMOperationByEC(HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws AMErrorException {

    ServiceInstance serviceInstance = EngineRestfulApi.getServiceInstance(jsonNode);
    String userName = ModuleUserUtils.getOperationUser(req, "executeECMOperationByEC");
    logger.info(
        "User {} try to execute ECM Operation by EngineConn {}.", userName, serviceInstance);
    EngineNode engineNode = engineNodeManager.getEngineNode(serviceInstance);
    Map<String, Object> parameters;
    try {
      parameters =
          JsonUtils.jackson()
              .readValue(
                  jsonNode.get("parameters").toString(),
                  new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      logger.error(
          "Fail to process the operation parameters: [{}] in request",
          jsonNode.get("parameters").toString(),
          e);
      return Message.error(
          "Fail to process the operation parameters, cased by "
              + ExceptionUtils.getRootCauseMessage(e));
    }
    parameters.put(ECMOperateRequest.ENGINE_CONN_INSTANCE_KEY(), serviceInstance.getInstance());
    if (!userName.equals(engineNode.getOwner()) && Configuration.isNotAdmin(userName)) {
      return Message.error(
          "You have no permission to execute ECM Operation by this EngineConn " + serviceInstance);
    }
    return executeECMOperation(
        engineNode.getEMNode(),
        engineNode.getServiceInstance().getInstance(),
        new ECMOperateRequest(userName, parameters));
  }

  @ApiOperation(
      value = "executeECMOperation",
      notes = "execute ECM operation",
      response = Message.class)
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "/executeECMOperation", method = RequestMethod.POST)
  public Message executeECMOperation(HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws AMErrorException {
    String userName = ModuleUserUtils.getOperationUser(req, "executeECMOperation");
    ServiceInstance serviceInstance = EngineRestfulApi.getServiceInstance(jsonNode);
    logger.info("User {} try to execute ECM Operation with {}.", userName, serviceInstance);
    EMNode ecmNode = this.emInfoService.getEM(serviceInstance);
    Map<String, Object> parameters;
    try {
      parameters =
          JsonUtils.jackson()
              .readValue(
                  jsonNode.get("parameters").toString(),
                  new TypeReference<Map<String, Object>>() {});
    } catch (JsonProcessingException e) {
      logger.error(
          "Fail to process the operation parameters: [{}] in request",
          jsonNode.get("parameters").toString(),
          e);
      return Message.error(
          "Fail to process the operation parameters, cased by "
              + ExceptionUtils.getRootCauseMessage(e));
    }
    if (parameters.containsKey("engineConnInstance")) {
      return executeECMOperation(
          ecmNode,
          parameters.get("engineConnInstance").toString(),
          new ECMOperateRequest(userName, parameters));
    } else {
      return executeECMOperation(ecmNode, "", new ECMOperateRequest(userName, parameters));
    }
  }

  @ApiOperation(value = "openEngineLog", notes = "open Engine log", response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(
        name = "applicationName",
        dataType = "String",
        example = "linkis-cg-engineconn"),
    @ApiImplicitParam(name = "emInstance", dataType = "String", example = "bdp110:9100"),
    @ApiImplicitParam(name = "instance", dataType = "String", example = "bdp110:21976"),
    @ApiImplicitParam(name = "parameters", dataType = "Map", value = "Parameters"),
    @ApiImplicitParam(name = "logType", dataType = "String", example = "stdout"),
    @ApiImplicitParam(name = "fromLine", dataType = "String", example = "0"),
    @ApiImplicitParam(name = "pageSize", dataType = "String", defaultValue = "1000"),
  })
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "/openEngineLog", method = RequestMethod.POST)
  public Message openEngineLog(HttpServletRequest req, @RequestBody JsonNode jsonNode)
      throws AMErrorException {
    String userName = ModuleUserUtils.getOperationUser(req, "openEngineLog");
    EMNode ecmNode;
    Map<String, Object> parameters;
    String engineInstance;
    try {
      String emInstance = jsonNode.get("emInstance").asText();
      engineInstance = jsonNode.get("instance").asText();
      ServiceInstance serviceInstance = EngineRestfulApi.getServiceInstance(jsonNode);
      logger.info("User {} try to open engine: {} log.", userName, serviceInstance);
      ecmNode =
          this.emInfoService.getEM(
              ServiceInstance.apply("linkis-cg-engineconnmanager", emInstance));
      logger.info("ecm node info:{}", ecmNode);
      parameters =
          JsonUtils.jackson()
              .readValue(
                  jsonNode.get("parameters").toString(),
                  new TypeReference<Map<String, Object>>() {});
      String logType = (String) parameters.get("logType");
      if (!logType.equals("stdout")
          && !logType.equals("stderr")
          && !logType.equals("gc")
          && !logType.equals("yarnApp")) {
        throw new AMErrorException(
            AMErrorCode.PARAM_ERROR.getErrorCode(), AMErrorCode.PARAM_ERROR.getErrorDesc());
      }
      parameters.put(OperateRequest$.MODULE$.OPERATOR_NAME_KEY(), "engineConnLog");
      parameters.put(ECMOperateRequest$.MODULE$.ENGINE_CONN_INSTANCE_KEY(), engineInstance);
      if (!parameters.containsKey("enableTail")) {
        parameters.put("enableTail", true);
      }
    } catch (JsonProcessingException e) {
      logger.error(
          "Fail to process the operation parameters: [{}] in request",
          jsonNode.get("parameters").toString(),
          e);
      return Message.error(
          "Fail to process the operation parameters, cased by "
              + ExceptionUtils.getRootCauseMessage(e));
    } catch (Exception e) {
      logger.error("Failed to open engine log, error:", e);
      return Message.error(e.getMessage());
    }
    return executeECMOperation(
        ecmNode, engineInstance, new ECMOperateRequest(userName, parameters));
  }

  private Message executeECMOperation(
      EMNode ecmNode, String engineInstance, ECMOperateRequest ecmOperateRequest) {
    if (Objects.isNull(ecmNode)) {
      return Message.error(
          MessageFormat.format(
              "ECM node :[{0}]  does not exist, Unable to open engine log(ECM节点:[{1}] 异常，无法打开日志，可能是该节点服务重启或者服务异常导致)",
              engineInstance, engineInstance));
    }
    String operationName = OperateRequest$.MODULE$.getOperationName(ecmOperateRequest.parameters());
    String userName = ecmOperateRequest.user();
    if (ArrayUtils.contains(adminOperations, operationName) && Configuration.isNotAdmin(userName)) {
      logger.warn(
          "User {} has no permission to execute {} admin Operation in ECM {}.",
          userName,
          operationName,
          ecmNode.getServiceInstance());
      return Message.error(
          "You have no permission to execute "
              + operationName
              + " admin Operation in ECM "
              + ecmNode.getServiceInstance());
    }

    // fill in logDirSuffix
    if (StringUtils.isNotBlank(engineInstance)
        && Objects.isNull(ecmOperateRequest.parameters().get("logDirSuffix"))) {
      ECResourceInfoRecord ecResourceInfoRecord =
          ecResourceInfoService.getECResourceInfoRecordByInstance(engineInstance);
      if (Objects.isNull(ecResourceInfoRecord)) {
        return Message.error("EC instance: " + engineInstance + " not exist ");
      }
      // eg logDirSuffix -> root/20230705/io_file/6d48068a-0e1e-44b5-8eb2-835034db5b30/logs
      String logDirSuffix = ecResourceInfoRecord.getLogDirSuffix();
      if (!userName.equals(ecResourceInfoRecord.getCreateUser())
          && Configuration.isNotJobHistoryAdmin(userName)) {
        logger.warn(
            "User {} has no permission to get log with path: {} in ECM:{}.",
            userName,
            logDirSuffix,
            ecmNode.getServiceInstance());
        return Message.error(
            "You have no permission to get log with path:"
                + logDirSuffix
                + " in ECM:"
                + ecmNode.getServiceInstance());
      }
      ecmOperateRequest.parameters().put("logDirSuffix", logDirSuffix);
    }

    ECMOperateResponse engineOperateResponse =
        ecmOperateService.executeOperation(ecmNode, ecmOperateRequest);

    return Message.ok()
        .data("result", engineOperateResponse.getResult())
        .data("errorMsg", engineOperateResponse.errorMsg())
        .data("isError", engineOperateResponse.isError());
  }

  @ApiOperation(
      value = "taskprediction",
      notes = "linkis task taskprediction",
      response = Message.class)
  @ApiImplicitParams({
    @ApiImplicitParam(name = "username", dataType = "String", example = "hadoop"),
    @ApiImplicitParam(name = "engineType", dataType = "String", example = "spark/hive"),
    @ApiImplicitParam(name = "creator", dataType = "String", value = "ide"),
    @ApiImplicitParam(name = "clustername", dataType = "String", example = "clustername"),
    @ApiImplicitParam(name = "queueName", dataType = "String", example = "queueName"),
    @ApiImplicitParam(name = "tenant", dataType = "String", defaultValue = "tenant"),
  })
  @ApiOperationSupport(ignoreParameters = {"jsonNode"})
  @RequestMapping(path = "/taskprediction", method = RequestMethod.GET)
  public Message taskprediction(
      HttpServletRequest req,
      @RequestParam(value = "username", required = false) String username,
      @RequestParam(value = "engineType", required = false) String engineType,
      @RequestParam(value = "creator", required = false) String creator,
      @RequestParam(value = "clustername", required = false) String clusterName,
      @RequestParam(value = "queueName", required = false) String queueName,
      @RequestParam(value = "tenant", required = false) String tenant)
          throws PersistenceErrorException, RMErrorException {
    String tokenName = ModuleUserUtils.getOperationUser(req, "taskprediction");
    if (StringUtils.isBlank(username)) {
      username = tokenName;
    }
    if (StringUtils.isBlank(engineType)) {
      Message.error("parameters:engineType can't be null (请求参数【engineType】不能为空)");
    }
    if (StringUtils.isBlank(creator)) {
      Message.error("parameters:creator can't be null (请求参数【creator】不能为空)");
    }

    // 获取用户配置信息
    List<ConfigVo> configlist = EMUtils.getUserConf(username, creator, engineType);
    configlist.addAll(EMUtils.getUserConf(username, "全局设置", engineType));

    // 获取租户标签数据
    if (StringUtils.isBlank(tenant)) {
      tenant = EMUtils.getTenant(username, creator);
    }
    if (StringUtils.isBlank(clusterName)) {
      clusterName = "default";
    }

    // 获取用户资源数据
    String labelValuePattern =
        MessageFormat.format("%{0}%,%{1}%,%{2}%,%", creator, username, engineType);
    List<PersistenceLabelRel> userLabels =
        labelManagerPersistence.getLabelByPattern(
            labelValuePattern, RMUtils.getCombinedLabel(), 0, 0);
    List<PersistenceResource> resources =
        resourceManagerPersistence.getResourceByLabels(userLabels);
    ArrayList<UserResourceVo> userResources = RMUtils.getUserResources(userLabels, resources);

    Map<String, Object> yarnResource = new HashMap<>();
    boolean checkYarnResult = false;
    if (engineType.toLowerCase().contains("spark")) {
      if (StringUtils.isBlank(queueName)) {
        // 如果没有传 队列名称，从用户配置获取
        queueName = EMUtils.getConfValue(configlist, AMConfiguration.YARN_QUEUE_NAME_CONFIG_KEY());
      }
      // 获取yarn资源数据
      ClusterLabel clusterLabel = LabelManagerUtils.labelFactory().createLabel(ClusterLabel.class);
      clusterLabel.setClusterName(clusterName);
      RMLabelContainer labelContainer = new RMLabelContainer(Lists.newArrayList(clusterLabel));
      YarnResourceIdentifier yarnIdentifier = new YarnResourceIdentifier(queueName);
      NodeResource providedYarnResource =
          externalResourceService.getResource(ResourceType.Yarn, labelContainer, yarnIdentifier);
      YarnResource maxResource = (YarnResource) providedYarnResource.getMaxResource();
      YarnResource usedResource = (YarnResource) providedYarnResource.getUsedResource();
      yarnResource.put("maxResource", maxResource.toJson());
      yarnResource.put("usedResource", usedResource.toJson());
      // 获取用户配置值于对比yarn资源进行对比
      long confMemory = Long.parseLong(EMUtils.getConfValue(configlist, "spark.executor.memory"));
      int confInstances =
          Integer.parseInt(EMUtils.getConfValue(configlist, "spark.executor.instances"));
      int confCores = Integer.parseInt(EMUtils.getConfValue(configlist, "spark.executor.cores"));
      long maxMemory = ByteTimeUtils.negativeByteStringAsGb(maxResource.queueMemory() + "b");
      long usedMemory = ByteTimeUtils.negativeByteStringAsGb(usedResource.queueMemory() + "b");
      boolean yarnMemoryResult = maxMemory - usedMemory > confMemory * confInstances;
      boolean yarnCoresResult =
          maxResource.queueCores() - usedResource.queueCores() > confCores * confInstances;
      checkYarnResult = yarnCoresResult && yarnMemoryResult;
    }
    // 获取ecm列表数据
    List<EMNodeVo> emNodeVos = AMUtils.copyToEMVo(emInfoService.getAllEM());
    String finalTenant = tenant;
    List<EMNodeVo> ecmResource =
        emNodeVos.stream()
            .filter(
                emNodeVo -> {
                  Stream<Label> labelStream = emNodeVo.getLabels().stream();
                  if (StringUtils.isNotBlank(finalTenant)) {
                    return labelStream.anyMatch(
                        label ->
                            KEY_TENANT.equals(label.getLabelKey())
                                && label.getStringValue().contains(finalTenant));
                  } else {
                    return labelStream.noneMatch(label -> KEY_TENANT.equals(label.getLabelKey()));
                  }
                })
            .filter(emNodeVo -> emNodeVo.getNodeHealthy().equals(NodeHealthy.Healthy))
            .collect(Collectors.toList());
    // 数据对比（ecm内存资源的比对)，内存剩余资源 > 引擎启动配置，剩余核心>0，剩余实例>0
    long ecmMemory = 0L;
    long ecmCores = 0L;
    if (engineType.toLowerCase().equals("spark")) {
      ecmMemory = Long.parseLong(EMUtils.getConfValue(configlist, "spark.executor.memory"));
      ecmCores = Integer.parseInt(EMUtils.getConfValue(configlist, "spark.driver.cores"));
    } else {
      ecmMemory =
          Long.parseLong(
              EMUtils.getConfValue(configlist, "wds.linkis.engineconn.java.driver.memory"));

      ecmCores =
          Integer.parseInt(
              EMUtils.getConfValue(configlist, "wds.linkis.engineconn.java.driver.cores"));
    }
    boolean ecmResult = false;
    boolean ecmResults = false;
    for (EMNodeVo emNodeVo : ecmResource) {
      Map leftResource = emNodeVo.getLeftResource();
      long memory =
          ByteTimeUtils.negativeByteStringAsGb(leftResource.get("memory").toString() + "b");
      int cores = (int) leftResource.get("cores");
      ecmResult = memory > ecmMemory;
      ecmResults = cores > ecmCores;
    }
    return Message.ok()
        .data("tenant", tenant)
        .data("userConf", configlist)
        .data("userResource", userResources)
        .data("ecmResource", ecmResource)
        .data("yarnResource", yarnResource)
        .data("checkResult", checkYarnResult && ecmResult && ecmResults);
  }
}
