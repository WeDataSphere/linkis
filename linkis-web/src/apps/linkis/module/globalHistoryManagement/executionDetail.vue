<!--
  ~ Licensed to the Apache Software Foundation (ASF) under one or more
  ~ contributor license agreements.  See the NOTICE file distributed with
  ~ this work for additional information regarding copyright ownership.
  ~ The ASF licenses this file to You under the Apache License, Version 2.0
  ~ (the "License"); you may not use this file except in compliance with
  ~ the License.  You may obtain a copy of the License at
  ~
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  -->

<template>
  <div class="execution-detail">
    <!-- Loading state: jobhistoryTask is undefined -->
    <Spin v-if="isLoading" fix>
      <Icon type="ios-loading" size="32" class="spin-icon-load"></Icon>
      <div>{{ $t('message.linkis.logLoading') }}</div>
    </Spin>

    <!-- Error state: jobhistoryTask is null (API failed) -->
    <div v-else-if="isLoadFailed" class="execution-detail-error">
      <Alert type="error" show-icon>
        {{ $t('message.linkis.execDetail.loadFailed') }}
        <Button type="primary" size="small" @click="reload">
          {{ $t('message.linkis.execDetail.reload') }}
        </Button>
      </Alert>
    </div>

    <!-- Normal data display: full beautified JSON -->
    <div v-else class="execution-detail-content">
      <div v-if="displayData" class="execution-detail-json">
        <pre>{{ displayData }}</pre>
      </div>
      <div v-else class="execution-detail-empty">
        {{ $t('message.linkis.execDetail.noData') }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'executionDetail',
  props: {
    jobhistoryTask: {
      type: Object,
      default: null
    }
  },
  computed: {
    isLoading() {
      // undefined = data not loaded yet
      return this.jobhistoryTask === undefined;
    },
    isLoadFailed() {
      // null = API request failed
      return this.jobhistoryTask === null;
    },
    /**
     * Merge the full task object, parsing JSON-string fields (paramsJson,
     * sourceJson, metrics, etc.) into real nested objects so they display
     * as readable JSON rather than escaped strings.
     */
    displayData() {
      const task = this.jobhistoryTask;
      if (!task) return '';

      const result = {};
      for (const key of Object.keys(task)) {
        const val = task[key];
        if (val === null || val === undefined) {
          result[key] = val;
        } else if (typeof val === 'object') {
          // Already an object/array — keep as-is
          result[key] = val;
        } else if (typeof val === 'string') {
          // Attempt to parse JSON-string fields into real objects
          const parsed = this.safeParse(val);
          result[key] = parsed !== null ? parsed : val;
        } else {
          result[key] = val;
        }
      }
      return JSON.stringify(result, null, 2);
    }
  },
  methods: {
    /**
     * Safely parse a JSON string, returning null on any failure.
     * @param {string} raw
     * @returns {object|null}
     */
    safeParse(raw) {
      if (!raw || typeof raw !== 'string') return null;
      try {
        return JSON.parse(raw);
      } catch (e) {
        window.console.warn('[executionDetail] JSON parse failed:', e);
        return null;
      }
    },
    reload() {
      this.$emit('reload');
    }
  }
}
</script>

<style lang="scss" scoped>
.execution-detail {
  height: 100%;
  padding: 16px;
  overflow-y: auto;

  &-error {
    padding: 20px;
  }

  &-content {
    height: 100%;
  }

  &-json {
    pre {
      margin: 0;
      padding: 12px;
      background-color: #f8f8f9;
      border: 1px solid #e8eaec;
      border-radius: 4px;
      font-family: 'Courier New', Consolas, monospace;
      font-size: 13px;
      line-height: 1.6;
      white-space: pre-wrap;
      word-break: break-all;
      max-height: calc(100vh - 300px);
      overflow-y: auto;
    }
  }

  &-empty {
    padding: 40px 0;
    text-align: center;
    color: #999;
    font-size: 14px;
  }
}
</style>
