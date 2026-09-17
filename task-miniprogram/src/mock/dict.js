export function mockRoleDict() {
  return Promise.resolve([
    { code: '01', name: '超级管理员' },
    { code: '02', name: '院系领导' },
    { code: '03', name: '科系领导' },
    { code: '04', name: '科员' }
  ])

}
export function mockDeptsDict() {
  return Promise.resolve([
    { code: '01', name: '工办' },
    { code: '02', name: '教务办' },
    { code: '03', name: '科研办' },
    { code: '04', name: '院办' }
  ])
}
export function mockCollegeDict() {
  return Promise.resolve([
    { code: '01', name: '计算机学院' },
  ])

}
export function mockServerityDict() {
  return Promise.resolve([
    { code: '01', name: '重要' },
    { code: '02', name: '常规' },
    { code: '03', name: '不重要' },
    { code: '04', name: '日常' }
  ])
}
export function mockTaskTypeDict() {
  return Promise.resolve([
    {
      code: '06',
      name: '人才培养',
      children: [
        {
          code: '07',
          name: '研究生培养',
          children: [
            { code: '02', name: '前期培养' }
          ]
        },
        {
          code: '01',
          name: '本科生培养',
          children: []
        }
      ]
    },
    { code: '03', name: '人才引进', children: [] },
    { code: '04', name: '教学任务', children: [] },
    { code: '05', name: '科研申报', children: [] }
  ])
}
export function mockStateDict() {
  return Promise.resolve([
    { code: '01', name: '已完成' },
    { code: '02', name: '进行中' },
    { code: '03', name: '已撤销' },
    { code: '04', name: '待开始' }
  ])
}

// Mock tenant (学院) data
export function mockGetAllTenant() {
  return Promise.resolve({
    statusCode: 200,
    data: {
      code: '200',
      data: [
        { id: '1', name: '计算机学院' },
        { id: '2', name: '电子工程学院' },
        { id: '3', name: '机械工程学院' }
      ]
    }
  })
}

// Mock department (科室) data
export function mockGetDepartmentByTid(tid) {
  const departmentData = {
    '1': [
      { id: '101', name: '软件工程系', tid: '1' },
      { id: '102', name: '网络工程系', tid: '1' },
      { id: '103', name: '计算机科学系', tid: '1' }
    ],
    '2': [
      { id: '201', name: '电子工程系', tid: '2' },
      { id: '202', name: '通信工程系', tid: '2' }
    ],
    '3': [
      { id: '301', name: '机械设计系', tid: '3' },
      { id: '302', name: '智能制造系', tid: '3' }
    ]
  }
  
  return Promise.resolve({
    statusCode: 200,
    data: {
      code: '200',
      data: departmentData[tid] || []
    }
  })
}