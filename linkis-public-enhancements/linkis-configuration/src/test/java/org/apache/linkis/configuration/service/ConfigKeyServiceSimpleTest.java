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

package org.apache.linkis.configuration.service;

import org.apache.linkis.configuration.dao.ConfigMapper;
import org.apache.linkis.configuration.entity.ConfigValue;
import org.apache.linkis.configuration.exception.ConfigurationException;
import org.apache.linkis.configuration.service.impl.ConfigKeyServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Simplified Mock Unit Tests for ConfigKeyService 测试ConfigKeyService层的deleteConfigValueById方法 - 简化版
 */
@ExtendWith({MockitoExtension.class})
@DisplayName("ConfigKeyService Mock单元测试")
public class ConfigKeyServiceSimpleTest {

  @Mock private ConfigMapper configMapper;

  @InjectMocks private ConfigKeyServiceImpl configKeyService;

  private ConfigValue testConfigValue;

  @BeforeEach
  public void setUp() {
    testConfigValue = new ConfigValue();
    testConfigValue.setId(123L);
    testConfigValue.setConfigValue("100G");
    testConfigValue.setConfigLabelId(456);
  }

  @Test
  @DisplayName("删除配置值-成功场景")
  public void testDeleteConfigValueByIdSuccess() {
    // Given
    Long configId = 123L;
    when(configMapper.getConfigValueById(configId)).thenReturn(testConfigValue);
    doNothing().when(configMapper).deleteConfigValueById(configId);

    // When
    ConfigValue result = configKeyService.deleteConfigValueById(configId);

    // Then
    assertNotNull(result);
    assertEquals(configId, result.getId());
    assertEquals("100G", result.getConfigValue());

    verify(configMapper, times(1)).getConfigValueById(configId);
    verify(configMapper, times(1)).deleteConfigValueById(configId);
  }

  @Test
  @DisplayName("删除配置值-配置不存在")
  public void testDeleteConfigValueByIdNotExists() {
    // Given
    Long configId = 999999L;
    when(configMapper.getConfigValueById(configId)).thenReturn(null);

    // When
    ConfigValue result = configKeyService.deleteConfigValueById(configId);

    // Then
    assertNull(result, "配置不存在时应返回null");
    verify(configMapper, times(1)).getConfigValueById(configId);
    verify(configMapper, never()).deleteConfigValueById(anyLong());
  }

  @Test
  @DisplayName("删除配置值-参数为null")
  public void testDeleteConfigValueByIdWithNull() {
    // When & Then
    assertThrows(
        ConfigurationException.class,
        () -> {
          configKeyService.deleteConfigValueById(null);
        },
        "应抛出ConfigurationException");

    verify(configMapper, never()).getConfigValueById(anyLong());
    verify(configMapper, never()).deleteConfigValueById(anyLong());
  }

  @Test
  @DisplayName("删除配置值-参数为负数")
  public void testDeleteConfigValueByIdWithNegative() {
    // Given
    Long configId = -1L;

    // When & Then
    assertThrows(
        ConfigurationException.class,
        () -> {
          configKeyService.deleteConfigValueById(configId);
        },
        "应抛出ConfigurationException");

    verify(configMapper, never()).getConfigValueById(anyLong());
    verify(configMapper, never()).deleteConfigValueById(anyLong());
  }

  @Test
  @DisplayName("删除配置值-参数为0")
  public void testDeleteConfigValueByIdWithZero() {
    // Given
    Long configId = 0L;

    // When & Then
    assertThrows(
        ConfigurationException.class,
        () -> {
          configKeyService.deleteConfigValueById(configId);
        },
        "应抛出ConfigurationException");

    verify(configMapper, never()).getConfigValueById(anyLong());
    verify(configMapper, never()).deleteConfigValueById(anyLong());
  }

  @Test
  @DisplayName("删除配置值-性能测试")
  public void testDeleteConfigValueByIdPerformance() {
    // Given
    Long configId = 123L;
    when(configMapper.getConfigValueById(configId)).thenReturn(testConfigValue);
    doNothing().when(configMapper).deleteConfigValueById(configId);

    // When
    long startTime = System.currentTimeMillis();
    for (int i = 0; i < 100; i++) {
      configKeyService.deleteConfigValueById(configId);
    }
    long endTime = System.currentTimeMillis();

    // Then
    long totalTime = endTime - startTime;
    long avgTime = totalTime / 100;

    assertTrue(avgTime < 10, "平均删除时间: " + avgTime + "ms 应小于 10ms");
  }

  @Test
  @DisplayName("删除配置值-验证Mapper调用顺序")
  public void testDeleteConfigValueByIdCallOrder() {
    // Given
    Long configId = 123L;
    when(configMapper.getConfigValueById(configId)).thenReturn(testConfigValue);
    doNothing().when(configMapper).deleteConfigValueById(configId);

    // When
    configKeyService.deleteConfigValueById(configId);

    // Then - 验证调用顺序：先查询，后删除
    org.mockito.InOrder inOrder = inOrder(configMapper);
    inOrder.verify(configMapper).getConfigValueById(configId);
    inOrder.verify(configMapper).deleteConfigValueById(configId);
  }
}
