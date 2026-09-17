# 文件上传接口参数分析

## 错误分析
- **错误位置**: FileNginxController.upload:37
- **错误类型**: NullPointerException  
- **影响页面**: task-add, meeting-add, works-add

## 可能缺失的参数

### 1. 业务参数
```java
// 后端可能期望的参数
@RequestParam("file") MultipartFile file
@RequestParam("tid") String tid           // ← 最可能缺失
@RequestParam("uid") String uid           // ← 可能缺失  
@RequestParam("type") String businessType  // ← 可能缺失
@RequestParam("category") String category  // ← 可能缺失
```

### 2. 请求头参数
```java
@RequestHeader("Authorization") String token
@RequestHeader("X-Tenant-ID") String tenantId
@RequestHeader("X-User-ID") String userId
```

### 3. 其他必需参数
```java
// 可能需要的上下文信息
String businessId = request.getParameter("businessId")
String relatedType = request.getParameter("relatedType")
```

## 前端当前实现
```javascript
const formData = new FormData()
formData.append('file', {
  uri: filePath,
  name: 'file', 
  type: 'file'
})
// ❌ 只传递了file参数
```

## 建议的修复方案

### 方案1: 添加业务参数
```javascript
const formData = new FormData()
formData.append('file', {
  uri: filePath,
  name: 'file',
  type: 'file'
})
// ✅ 添加业务参数
formData.append('tid', userInfo.tid)
formData.append('uid', userInfo.id)  
formData.append('type', 'task') // 或 'meeting', 'works'
formData.append('businessId', '') // 如果有相关业务ID
```

### 方案2: 添加请求头
```javascript
const res = await fileApi.upload(formData, {
  headers: {
    'Content-Type': 'multipart/form-data',
    'X-Tenant-ID': userInfo.tid,
    'X-User-ID': userInfo.id,
    'Authorization': `Bearer ${token}`
  }
})
```

### 方案3: 检查后端接口文档
需要查看后端接口文档，确认：
1. 必需参数列表
2. 可选参数说明
3. 认证方式要求
4. 业务上下文要求