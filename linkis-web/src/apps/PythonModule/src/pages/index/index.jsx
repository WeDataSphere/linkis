import { onMounted, defineComponent } from 'vue';
import { defineRouteMeta, useI18n } from '@fesjs/fes';
import { FForm, FFormItem, FButton, FModal, FText, FConfigProvider, enUS, zhCN } from '@fesjs/fes-design';
import { UploadOutlined } from '@fesjs/fes-design/icon';
import {
  WInput,
  WSelect,
  WRadioGroup,
  WUpload,
  WTextarea,
  WTable,
} from '@webank/fes-design-material';
import { useSharedLetgoGlobal } from '../../letgo/useLetgoGlobal';
import { useJSQuery } from '../../letgo/useJSQuery';
import { letgoRequest } from '../../letgo/letgoRequest';
import { Main } from './main';
import { markClassReactive } from '../../letgo/reactive.js';
import { isGetterProp } from '../../letgo/shared.js';

defineRouteMeta({
  name: 'index',
  title: 'agent生成',
});

export default defineComponent({
  name: 'index',
  setup() {
    const { $context, $utils } = useSharedLetgoGlobal();
    const locale = localStorage.getItem('locale') === 'en' ? enUS : zhCN;

    const apiPythonfileexistUdf = useJSQuery({
      id: 'apiPythonfileexistUdf',
      query(params) {
        return letgoRequest(
          '/api/rest_j/v1/udf/python-file-exist',
          params,
          {
            method: 'GET',
            headers: {},
          },
        );
      },

      runCondition: 'manual',

      successEvent: [],
      failureEvent: [],
    });

    const apiPythonuploadFilesystem = useJSQuery({
      id: 'apiPythonuploadFilesystem',
      query(params) {
        return letgoRequest(
          '/api/rest_j/v1/filesystem/python-upload',
          params,
          {
            method: 'POST'
          },
        );
      },

      runCondition: 'manual',

      successEvent: [],
      failureEvent: [],
    });

    const apiPythonsaveUdf = useJSQuery({
      id: 'apiPythonsaveUdf',
      query(params) {
        return letgoRequest('/api/rest_j/v1/udf/python-save', params, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        });
      },

      runCondition: 'manual',

      successEvent: [],
      failureEvent: [],
    });

    const apiPythondeleteUdf = useJSQuery({
      id: 'apiPythondeleteUdf',
      query(params) {
        return letgoRequest(
          '/api/rest_j/v1/udf/python-delete',
          params,
          {
            method: 'GET',
            headers: {},
          },
        );
      },

      runCondition: 'manual',

      successEvent: [],
      failureEvent: [],
    });

    const apiPythonlistUdf = useJSQuery({
      id: 'apiPythonlistUdf',
      query(params) {
        return letgoRequest('/api/rest_j/v1/udf/python-list', params, {
          method: 'GET',
          headers: {},
        });
      },

      runCondition: 'manual',

      successEvent: [],
      failureEvent: [],
    });
    const { t: $t } = useI18n(); 
    const __instance__ = new Main({
      globalCtx: {
        $utils,
        $context,
      },
      instances: {},
      codes: {
        apiPythonlistUdf,
        apiPythonfileexistUdf,
        apiPythonuploadFilesystem,
        apiPythonsaveUdf,
        apiPythondeleteUdf,
        $t,
      },
    });
    const $$ = markClassReactive(__instance__, (member) => {
      if (
        member.startsWith('_') ||
                member.startsWith('$') ||
                isGetterProp(__instance__, member) ||
                typeof __instance__[member] === 'function'
      )
        return false;

      return true;
    });

    onMounted($$.onMounted.bind($$))

    const wTable2Columns9RenderSlots = (slotProps) => {
      return (
        <div style={{width: '200px', display: 'flex'}}>
          <FButton
            disabled={slotProps.row.isExpire === 1}
            onClick={[
              () => {
                $$.showEditModuleModal(slotProps.row);
              },
            ]}
            type="link"
            style={{padding: '0px', width: '45px'}}
          >
            {$t('edit')}
          </FButton>
          <FButton
            disabled={slotProps.row.isExpire === 1}
            onClick={[
              () => {
                $$.showLoadStatusChangeConfirmation(
                  slotProps.row,
                );
              },
            ]}
            type="link"
            style={{padding: '0px', width: '45px'}}
          >
            {slotProps.row.isLoad === 1 ? $t('loaded') : $t('unload')}
          </FButton>
          <FButton
            disabled={slotProps.row.isExpire === 1}
            onClick={[
              () => {
                $$.showDeleteConfirmation(slotProps.row);
              },
            ]}
            type="link"
            style={{ color: slotProps.row.isExpire === 1 ? '#CFD0D3' : 'red', padding: '0px', width: '45px'}}
          >
            {$t('delete')}
          </FButton>
        </div>
      );
    };

    return () => {
      return (
        <div
          class="letgo-page"
          style={{
            padding: '10px',
          }}
        >
          <FConfigProvider
            locale={locale}
          >
            <FForm labelWidth={80} layout={`inline`}>
              <WInput
                _label={$t('moduleName')}
                placeholder={$t('moduleNamePlaceholder')}
                v-model={$$.pythonModuleName}
                span={5}
                style={{
                  width: '260px',
                }}
                labelWidth={`60px`}
              />
              <WInput
                _label={$t('userName')}
                placeholder={$t('userNamePlaceholder')}
                v-model={$$.userName}
                span={5}
                style={{
                  width: '260px',
                }}
                labelWidth={`60px`}
              />
              <WSelect
                _label={$t('engineType')}
                labelWidth={`60px`}
                span={5}
                style={{
                  width: '260px',
                }}
                v-model={$$.engineType}
                options={[
                  {
                    value: 'spark',
                    label: 'Spark',
                  },
                  {
                    value: 'python',
                    label: 'Python',
                  },
                  {
                    value: 'all',
                    label: $t('general'),
                  },
                  {
                    value: '',
                    label: $t('all')
                  }
                ]}
              />
              <WSelect
                _label={$t('isExpire')}
                labelWidth={60}
                span={5}
                style={{
                  width: '260px',
                }}
                v-model={$$.isExpired}
                options={[
                  {
                    value: 0,
                    label: $t('no'),
                  },
                  {
                    value: 1,
                    label: $t('yes'),
                  },
                  {
                    value: '',
                    label: $t('all'),
                  }
                ]}
              />
              <WSelect
                _label={$t('isLoaded')}
                labelWidth={60}
                span={5}
                style={{
                  width: '260px',
                }}
                v-model={$$.isLoaded}
                options={[
                  {
                    value: 0,
                    label: $t('no'),
                  },
                  {
                    value: 1,
                    label: $t('yes'),
                  },
                  {
                    value: '',
                    label: $t('all'),
                  }
                ]}
              />
              <FFormItem style={{
                gridColumn: 'auto'
              }}>
                <FButton
                  type="primary"
                  style={{
                    marginRight: '10px'
                  }}
                  onClick={[
                    () => {
                      $$.currentPage = 1
                      $$.loadPythonModuleList(
                        $$.pythonModuleName,
                        $$.userName,
                        $$.engineType,
                        $$.isExpired,
                        $$.isLoaded,
                        $$.currentPage,
                        $$.pageSize,
                      );
                    },
                  ]}
                >
                  {$t('search')}
                </FButton>
                <FButton
                  onClick={[
                    () => {
                      $$.currentPage = 1;
                      $$.resetQueryParameters();
                    },
                  ]}
                  type="primary"
                  style={{ backgroundColor: '#ff9900', color: '#fff', borderColor: '#ff9900', marginRight: '10px' }}
                >
                  {$t('clear')}
                </FButton>
                <FButton
                  onClick={[
                    () => {
                      $$.showAddModuleModal();
                    },
                  ]}
                  type="primary"
                  style={{ backgroundColor: '#19be6b', color: '#fff', borderColor: '#19be6b', marginRight: '10px' }}
                >
                  {$t('add')}
                </FButton>
                <FButton
                  onClick={[
                    () => {
                      $$.showTutorial();
                    },
                  ]}
                  type="primary">
                  {$t('tutorial')}
                </FButton>
              </FFormItem>
            </FForm>
            <FModal
              show={$$.editPythonModuleVisible}
              title={$t('editPythonModule')}
              displayDirective={'show'}
              closable={true}
              mask={false}
              maskClosable={true}
              width={520}
              top={50}
              verticalCenter={false}
              center={false}
              fullScreen={false}
              footer={true}
              okText={$t('confirm')}
              okLoading={false}
              cancelText={$t('cancel')}
              contentClass={''}
              getContainer={() => document.body}
              onUpdate:show={[
                () => {
                  $$.closeEditModuleModal();
                },
              ]}
              onOk={[
                () => {
                  $$.handleEditModule();
                },
              ]}
              onCancel={[
                () => {
                  $$.closeEditModuleModal();
                },
              ]}
            >
              <FForm ref={(el) => $$.editFormRef = el} model={$$.selectedModule} labelWidth={80}>
                <WInput
                  _label={$t('moduleName')}
                  v-model={$$.selectedModule.name}
                  rules={[
                    {
                      required: true,
                      message: $t('moduleNameRequired'),
                    },
                  ]}
                  placeholder={$t('moduleNamePlaceholder')}
                  disabled
                />
                <WRadioGroup
                  _label={$t('engineType')}
                  v-model={$$.selectedModule.engineType}
                  rules={[
                    {
                      required: true,
                      message: $t('selectEngineType'),
                    },
                  ]}
                  options={[
                    {
                      value: 'spark',
                      label: 'Spark',
                    },
                    {
                      value: 'python',
                      label: 'Python',
                    },
                    {
                      value: 'all',
                      label: $t('general'),
                    },
                  ]}
                />
                <WUpload
                  v-model:fileList={$$.selectedModule.fileList}
                  _label={$t('moduleResource')}
                  action={`/api/rest_j/v1/udf/python-upload`}
                  rules={[
                    {
                      required: true,
                      message: $t('uploadResource'),
                      type: 'array',
                      validator: (rule, value) => {
                        if($$.selectedModule.path && $$.selectedModule.name) {
                          return true;
                        }
                        return false
                      }
                    },
                  ]}
                  v-slots={{
                    tip: () => {
                      if(!$$.selectedModule.name) {
                        return (
                          <FText
                            type={'default'}
                            style={{
                              width: '200px',
                            }}
                          >
                            {$t('onlyPyAndZip')}
                          </FText>
                        );
                      }
                      else {
                        return ''
                      }
                                        
                    },
                    fileList: () => {
                      if($$.selectedModule.name && $$.selectedModule.path) {
                        return (
                          <div>
                            <FText type={'default'}>{$$.selectedModule.name + '.' + $$.selectedModule.path?.split('.')[1] || ''}</FText>
                          </div>
                        );
                      }
                      else {
                        return ''
                      }
                                        
                    }
                  }}
                  beforeUpload={(...args) => $$.validateModuleFile(...args)}
                  accept={['.zip', '.py']}
                  httpRequest={(...args) => $$.handleUploadHttpRequest(...args)}
                >
                  <FButton 
                    type={'default'}
                    v-slots={{
                      icon: () => <UploadOutlined />
                    }}
                  >
                    {$t('uploadFile')}
                  </FButton>
                </WUpload>
                <WRadioGroup
                  _label={$t('isLoaded')}
                  v-model={$$.selectedModule.isLoad}
                  rules={[
                    {
                      required: true,
                      message: $t('selectIsLoaded'),
                      type: 'number',
                    },
                  ]}
                  options={[
                    {
                      value: 1,
                      label: $t('yes'),
                    },
                    {
                      value: 0,
                      label: $t('no'),
                    },
                  ]}
                />
                <WRadioGroup
                  _label={$t('isExpire')}
                  v-model={$$.selectedModule.isExpire}
                  rules={[
                    {
                      required: true,
                      message: $t('selectIsExpire'),
                      type: 'number',
                    },
                  ]}
                  options={[
                    {
                      value: 1,
                      label: $t('yes'),
                    },
                    {
                      value: 0,
                      label: $t('no'),
                    },
                  ]}
                />
                <WInput
                  _label={$t('moduleDescription')}
                  v-model={$$.selectedModule.description}
                  rules={[
                    {
                      required: false,
                      message: $t('moduleDescriptionPlaceholder'),
                    },
                  ]}
                  type="textarea"
                  placeholder={$t('moduleDescriptionPlaceholder')}
                  autosize={{minRows: 2, maxRows: 5}}
                />
              </FForm>
            </FModal>
            <FModal
              show={$$.addPythonModuleVisible}
              mask={false}
              title={$t('addPythonModule')}
              contentClass={
                $$.addModuleModalVisible ? 'custom-class' : ''
              }
              okText={$t('confirm')} 
              cancelText={$t('cancel')} 
              onUpdate:show={[
                () => {
                  $$.closeAddModuleModal();
                },
              ]}
              onOk={() => {
                $$.handleAddModule();
              }}
            >
              <FForm ref={(el) => $$.addFormRef = el} model={$$.selectedModule} labelWidth={80}>
                <WInput
                  _label={$t('moduleName')}
                  v-model={$$.selectedModule.name}
                  rules={[
                    {
                      required: true,
                      message: $t('moduleNameRequired'),
                    },
                  ]}
                  placeholder={$t('moduleNamePlaceholder')}
                  disabled
                />
                <WRadioGroup
                  _label={$t('engineType')}
                  v-model={$$.selectedModule.engineType}
                  rules={[
                    {
                      required: true,
                      message: $t('selectEngineType'),
                    },
                  ]}
                  options={[
                    {
                      value: 'spark',
                      label: 'Spark',
                    },
                    {
                      value: 'python',
                      label: 'Python',
                    },
                    {
                      value: 'all',
                      label: $t('general'),
                    },
                  ]}
                />
                <WUpload
                  v-model:fileList={$$.selectedModule.fileList}
                  _label={$t('moduleResource')}
                  action={`/api/rest_j/v1/udf/python-upload`}
                  rules={[
                    {
                      required: true,
                      message: $t('uploadResource'),
                      type: 'array',
                      validator: (rule, value) => {
                        if($$.selectedModule.path && $$.selectedModule.name) {
                          return true;
                        }
                        return false
                      }
                    },
                  ]}
                  v-slots={{
                    tip: () => {
                      if(!$$.selectedModule.name) {
                        return (
                          <FText
                            type={'default'}
                            style={{
                              width: '200px',
                            }}
                          >
                            {$t('onlyPyAndZip')}
                          </FText>
                        );
                      }
                      else {
                        return ''
                      }
                                        
                    },
                    fileList: () => {
                      if($$.selectedModule.name && $$.selectedModule.path) {
                        return (
                          <div>
                            <FText type={'default'}>{$$.selectedModule.name + '.' + $$.selectedModule.path?.split('.')[1] || ''}</FText>
                          </div>
                        );
                      }
                      else {
                        return ''
                      }
                                        
                    }
                  }}
                  beforeUpload={(...args) => $$.validateModuleFile(...args)}
                  accept={['.zip', '.py']}
                  httpRequest={(...args) => $$.handleUploadHttpRequest(...args)}
                >
                  <FButton 
                    type={'default'}
                    v-slots={{
                      icon: () => <UploadOutlined />
                    }}
                  >
                    {$t('uploadFile')}
                  </FButton>
                </WUpload>
                <WRadioGroup
                  _label={$t('isLoaded')}
                  v-model={$$.selectedModule.isLoad}
                  rules={[
                    {
                      required: true,
                      message: $t('selectIsLoaded'),
                      type: 'number',
                    },
                  ]}
                  options={[
                    {
                      value: 1,
                      label: $t('yes'),
                    },
                    {
                      value: 0,
                      label: $t('no'),
                    },
                  ]}
                />
                <WRadioGroup
                  _label={$t('isExpire')}
                  v-model={$$.selectedModule.isExpire}
                  rules={[
                    {
                      required: true,
                      message: $t('selectIsExpire'),
                      type: 'number',
                    },
                  ]}
                  options={[
                    {
                      value: 1,
                      label: $t('yes'),
                    },
                    {
                      value: 0,
                      label: $t('no'),
                    },
                  ]}
                />
                <WInput
                  _label={$t('moduleDescription')}
                  v-model={$$.selectedModule.description}
                  rules={[
                    {
                      required: false,
                      message: $t('moduleDescriptionPlaceholder'),
                    },
                  ]}
                  type="textarea"
                  placeholder={$t('moduleDescriptionPlaceholder')}
                  autosize={{minRows: 2, maxRows: 5}}
                />
              </FForm>
            </FModal>
            <FModal
              show={$$.tutorialVisible}
              displayDirective={'if'}
              closable={false}
              showCancel={false}
              mask={false}
              width={800}
              top={50}
              verticalCenter={false}
              center={false}
              fullScreen={false}
              footer={true}
              okText={$t('known')}
              okLoading={false}
              cancelText={$t('cancel')}
              contentClass={''}
              maxHeight={'500px'}
              getContainer={() => document.body}
              v-slots={{
                title: () => {
                  return(<h1>{$t('useTutorial')}</h1>)
                }
              }}
              onUpdate:show={[
                () => {
                  $$.tutorialVisible = false;
                },
              ]}
              onOk={[
                () => {
                  localStorage.setItem('hasRead', 'true');
                  $$.tutorialVisible = false;
                },
              ]}
            >
              <div style={{padding: '0px 20px'}}>
                <h2 style="color: #333;">1. 首先你需要准备好对应的模块物料，支持.py文件和.zip文件</h2>
                <p>a. 对于py 文件你的模块名一定要和文件名一致，如hello_world.py 模块名就是hello_world,您可以在里面定义你的python方法和class等，如下：</p>
                <pre style="background-color: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto;">
                  <code><span style={{ color: '#999' }}>#文件名一定是 hello_world.py<br /></span>
def add_hello_world(name):<br />
                    <span>&nbsp;&nbsp;</span>return "hello world {}".format(name)
                  </code>
                </pre>
                <p>b. 对于zip文件需要稍微复杂点，你得遵循Python模块包的文件格式，如下：</p>
                <pre style="background-color: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto;">
                  <code><span style={{ color: '#999' }}>#zip文件名hello_world.zip</span><br />
hello_world.zip<br />
├── hello_world  <span style={{ color: '#999' }}># 包目录，必须为模块名</span><br />
   ├── __init__.py <span style={{ color: '#999' }}># 必须有该文件</span><br />
   ├── hello_world.py <span style={{ color: '#999' }}># 其它python文件</span><br />
                    <span style={{ color: '#999' }}>#hello_world.py文件内容如下：</span><br />
def add_hello_world(name):<br />
                    <span>&nbsp;&nbsp;</span>return "hello world {}".format(name)<br />
                  </code>
                </pre>
                <p>需要有 <code style="background-color: #f4f4f4; padding: 2px 4px; border-radius: 4px;">__init__.py</code> 文件才能让 Python 将包含该文件的目录当作包来处理，可以只是一个空文件。你也可以直接去pip网站上面下载对应的包，然后通过zip压缩即可</p>
                <h2 style="color: #333;">2. 在linkis 管理台建立对应的模块：</h2>
                <p>第一步点击新增</p>
                <p>第二步选择对应的引擎，分别对于Spark（.py脚本），python（.python脚本），通用（.py脚本和.python脚本）</p>
                <p>第三步上传之前准备的模块物料，如果要更新模块内容需要先将旧的模块删除后再重新上传</p>
                <p>第四步选择加载才可以进行使用</p>
                <p>第五步如果你的模块不需要了，可以选择过期，等同于删除</p>
                <img style={{width: '100%'}} src="data:image/png;base64,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" />
                <h2 style="color: #333;">3. 在scritpis进行使用</h2>
                <p>1. 如果你的模块已经加好了，可以直接在脚本中通过import进行使用如下：</p>
                <pre style="background-color: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto;">
                  <code>from hello_world import add_hello_world<br />
add_hello_world("peace")
                  </code>
                </pre>
                <p>2. 可以将对应的模块注册成Spark的UDF进行使用</p>
                <pre style="background-color: #f4f4f4; padding: 10px; border-radius: 4px; overflow-x: auto;">
                  <code>from pyspark.sql.functions import udf<br />
from pyspark.sql.types import StringType<br />
from hello_world import add_hello_world<br />
add_hello_world_udf = udf(add_hello_world, StringType())<br />
df=spark.sql("select * from dws_demo.demo_user_info limit 100")<br />
df_transformed = df.withColumn("username", add_hello_world_udf(df["username"]))<br />
df_transformed.show()
                  </code>
                </pre>
              </div>
            </FModal>
            <WTable
              columns={[
                {
                  prop: 'name',
                  label: $t('moduleName'),
                },
                {
                  prop: 'engineType',
                  label: $t('engineType'),
                  formatter: ({
                    row,
                    column,
                    rowIndex,
                    coloumIndex,
                    cellValue,
                  }) => {
                    return row.engineType === 'all'
                      ? $t('general') : row.engineType === 'spark' ? 'Spark'
                        : 'Python';
                  },
                },
                {
                  prop: 'status',
                  label: $t('status'),
                  formatter: ({
                    row,
                    column,
                    rowIndex,
                    coloumIndex,
                    cellValue,
                  }) => {
                    return row.isExpire === 0
                      ? $t('normal')
                      : $t('expire');
                  },
                },
                {
                  prop: 'path',
                  label: $t('pathInfo'),
                },
                {
                  prop: 'description',
                  label: $t('moduleDescription'),
                },
                {
                  prop: 'createTime',
                  label: $t('createTime'),
                  formatter: ({
                    row,
                    column,
                    rowIndex,
                    columnIndex,
                    cellValue,
                  }) => {
                    return $utils
                      .dayjs(row.createTime)
                      .format('YYYY-MM-DD HH:mm:ss');
                  },
                },
                {
                  prop: 'updateTime',
                  label: $t('updateTime'),
                  formatter: ({
                    row,
                    column,
                    rowIndex,
                    columnIndex,
                    cellValue,
                  }) => {
                    return $utils
                      .dayjs(row.updateTime)
                      .format('YYYY-MM-DD HH:mm:ss');
                  },
                },
                {
                  prop: 'createUser',
                  label: $t('createUser'),
                },
                {
                  prop: 'updateUser',
                  label: $t('updateUser'),
                },
                {
                  prop: '',
                  label: $t('option'),
                  width: 200,
                  render: wTable2Columns9RenderSlots,
                },
              ]}
              data={$$.pythonModuleList}
              pagination={{
                currentPage: $$.currentPage,
                pageSize: $$.pageSize,
                totalCount: $$.totalRecords,
                showTotal: true,
                alwayShow: true,
                showQuickJumper: true,
              }}
              onChange={[(...args) => $$.handlePageChange(...args)]}
            />
          </FConfigProvider>
        </div>
      );
    };
  },
});