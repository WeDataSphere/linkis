# language: zh-CN
@regression @performance @jmx
Feature: performance JMX 性能回归测试

  作为测试系统，需要保证 performance 模块的 JMX 性能测试稳定
  以便确保该模块在负载条件下的响应时间和吞吐量满足要求

  @regression @performance
  Scenario Outline: 任务提交接口性能测试
    Given JMX 测试计划 "linkis-entrance_task-submit.jmx" 已就绪
    And 外部阈值配置: P95<2000ms, P99<5000ms, 错误率<1.0%, 吞吐量>50.0TPS
    When 执行 JMeter 测试计划，并发用户 100 人
    Then 总请求数应大于 0
    And P95 响应时间应低于 2000ms
    And P99 响应时间应低于 5000ms
    And 错误率应低于 1.0%
    And 吞吐量应高于 50.0 TPS
    And JMX 内置断言应全部通过

    Examples:
      | jmx文件名 | p95 | p99 | errorRate | throughput | users |
      | linkis-entrance_task-submit.jmx | 2000 | 5000 | 1.0 | 50.0 | 100 |

  @regression @performance
  Scenario Outline: 任务查询接口性能测试
    Given JMX 测试计划 "linkis-entrance_task-query.jmx" 已就绪
    And 外部阈值配置: P95<1500ms, P99<3000ms, 错误率<0.5%, 吞吐量>80.0TPS
    When 执行 JMeter 测试计划，并发用户 200 人
    Then 总请求数应大于 0
    And P95 响应时间应低于 1500ms
    And P99 响应时间应低于 3000ms
    And 错误率应低于 0.5%
    And 吞吐量应高于 80.0 TPS
    And JMX 内置断言应全部通过

    Examples:
      | jmx文件名 | p95 | p99 | errorRate | throughput | users |
      | linkis-entrance_task-query.jmx | 1500 | 3000 | 0.5 | 80.0 | 200 |

  @regression @performance
  Scenario Outline: 数据源元数据查询性能测试
    Given JMX 测试计划 "linkis-datasource_query-metadata.jmx" 已就绪
    And 外部阈值配置: P95<1000ms, P99<2000ms, 错误率<0.1%, 吞吐量>100.0TPS
    When 执行 JMeter 测试计划，并发用户 50 人
    Then 总请求数应大于 0
    And P95 响应时间应低于 1000ms
    And P99 响应时间应低于 2000ms
    And 错误率应低于 0.1%
    And 吞吐量应高于 100.0 TPS
    And JMX 内置断言应全部通过

    Examples:
      | jmx文件名 | p95 | p99 | errorRate | throughput | users |
      | linkis-datasource_query-metadata.jmx | 1000 | 2000 | 0.1 | 100.0 | 50 |
