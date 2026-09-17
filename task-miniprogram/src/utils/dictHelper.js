// 将数组转换为 Map/Object，方便通过 key 快速查找
// 默认使用 'code' 作为 key，可通过第二个参数指定
// 示例：arrayToMap([{code: '01', name: '张三'}, {code: '02', name: '李四'}])
// 结果：{ '01': {code: '01', name: '张三'}, '02': {code: '02', name: '李四'} }
export function arrayToMap(arr, key = 'code') {
  if (!Array.isArray(arr)) {
    return {}
  }
  
  return arr.reduce((map, item) => {
    if (item && item[key]!== undefined && item[key] !== null) {
      map[item[key]] = item
    }
    return map
  }, {})
}