<template>
  <div style="position:relative">
    <Tabs @on-click="onClickTabs" :value="tabName">
      <TabPane name="stdout" label="stdout"></TabPane>
      <TabPane name="stderr" label="stderr"></TabPane>
    </Tabs>
    <Button class="backButton" type="primary" @click="back">{{$t('message.linkis.back')}}</Button>
    <log :logs="logs" :scriptViewState="scriptViewState"/>
    <Page
      ref="page"
      :total="page.totalSize"
      :page-size="page.pageSize"
      :current="page.pageNow"
      class-name="page engine-log-page"
      size="small"
      prev-text="上一页" next-text="下一页"
      @on-change="change"/>
  </div>
</template>
<script>
import api from '@/common/service/api';
import log from '@/components/consoleComponent/log.vue'
import elementResizeEvent from '@/common/helper/elementResizeEvent';
export default {
  components: {
    log
  },
  data() {
    return {
      tabName: 'stdout',
      page: {
        totalSize: 0,
        pageSize: 1000,
        pageNow: 1,
      },
      logs: {
        all: ''
      },
      scriptViewState: {
        bottomContentHeight: window.innerHeight - 353
      }
    };
  },
  computed: {
  },
  created() {

  },
  mounted() {
    window.addEventListener("resize", this.resize);
    elementResizeEvent.bind(this.$el, this.resize);
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resize);
    elementResizeEvent.unbind(this.$el);
  },
  methods: {
    // 切换分页
    change(val) {
      this.page.pageNow = val;
      this.getLogs((val - 1) * this.page.pageSize)
    },
    async getLogs(fromLine, param) {
      if (param) {
        this.param = param
      }
      if (this.param) {
        const params = {
          applicationName: this.param.applicationName,
          emInstance: this.param.emInstance,
          instance: this.param.instance,
          parameters: {
            pageSize: 1000,
            fromLine,
            logType: this.tabName
          }
        }
        let res = await api.fetch('/linkisManager/openEngineLog', params, 'post') || {};
        if (res && res.result) {
          if (res.result.rows < 1000) { // 最后一页
            this.page.totalSize = this.page.pageNow * 1000
          } else {
            this.page.totalSize = (this.page.pageNow + 1) * 1000
          }
          this.logs = {
            all: res.result.logs ? res.result.logs.join('\n') : ''
          }
        }
      }
    },
    resize() {
      this.scriptViewState = {
        bottomContentHeight: window.innerHeight - 353
      }
    },
    onClickTabs(name) {
      this.tabName = name
      this.page.pageNow = 1
      this.getLogs(0)
    },
    back() {
      this.$emit('back')
    }
  }
};
</script>
<style lang="scss" scoped>
 .backButton {
  position: absolute;
  top: -2px;
  right: 20px;
}
.page {
  text-align: center
}
</style>
<style>
.engine-log-page .ivu-page-item, .engine-log-page .ivu-page-item-jump-prev {
  display: none;
}
.engine-log-page li.ivu-page-next {
  padding-left: 20px;
}
</style>