export const subAppRoutes = {
  path: '',
  name: 'layout',
  component: () => import('./view/layout.vue'),
  redirect: '/urm',
  meta: {
    title: 'DataSphere Studio',
    publicPage: true, // 权限公开
  },
  children: []
}

export default [
  {
    path: 'urm',
    name: 'URM',
    redirect: '/urm/udfManagement',
    component: () => import('./view/urm/index.vue'),
    meta: {
      title: 'linkis',
      publicPage: true,
    },
    children: [{
      name: 'udfFunctionManage',
      path: 'udfManagement',
      component: () =>
        import('./module/udfManagement/index.vue'),
      meta: {
        title: 'udfManagement',
        publicPage: true,
      },
    },
    {
      name: 'functionManagement',
      path: 'functionManagement',
      component: () =>
        import('./module/functionManagement/index.vue'),
      meta: {
        title: 'functionManagement',
        publicPage: true,
      },
    }]
  }
]
