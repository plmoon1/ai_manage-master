# 项目配置步骤详解

# 项目数据库配置步骤

## 1. 创建数据库

在Mysql8中创建名为 `task_manage` 的数据库。

## 2. 运行SQL脚本

在 MySQL 环境中执行脚本文件 `springboot/sql/task_manage.sql`。

## 3. 配置数据库连接信息

修改后端项目路径 `springboot/src/main/resources/application.yml` 中的数据库配置，具体配置内容如下：

```YAML

spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    # 数据库用户名
    username: root
    password:  # 数据库密码
    url: jdbc:mysql://localhost:3306/task_manage?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 100MB
```

# 开发与生产（部署）
## 修改application.yml文件
```YAML
# 由于我们现在只有一个yml配置文件，而没有使用application-dev.yml和application-prod.yml文件，并在application.yml中配置profile.active，
# 故采用注释的方法进行开发和生产（部署）的切换，开发与生产（部署）注释和启动相应的配置即可


# 数据库配置
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root   #数据库用户名
    password: Qzb15607074531 #数据库密码
    url: jdbc:mysql://localhost:3306/task_manage?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true
#    以下为Linux部署==================================
#    password: Qzb123456* #数据库密码
#    url: jdbc:mysql://118.31.165.121:3306/task_manage?useUnicode=true&characterEncoding=utf-8&allowMultiQueries=true&useSSL=false&serverTimezone=GMT%2b8&allowPublicKeyRetrieval=true



# 日志配置-生产环境=============================
#logging:
#  # 日志输出路径（指定到你的日志目录）
#  file:
#    name: /work/task_manage/logs/backend.log
#  level:
#    com.qzb.springboot.mapper: debug # 打印SQL日志
## 日志配置-开发环境=============================
logging:
  level:
    com.qzb.springboot.mapper: debug # 打印SQL日志


# 自定义文件上传路径（核心：改为绝对路径，不要用相对路径）
# Linux环境============================================
#file:
#  upload:
#    # Windows 示例：D:/nginx/files/
#    # Linux/Mac 示例：/usr/local/nginx/files/
#    path: /work/task_manage/resource/  # 指向隔离目录
#    # 前端访问上传文件的前缀（和 Nginx 配置对应）
#    access-path: /resource/     # 前端访问前缀

# 开发环境=====================================
file:
  upload:
    # Windows 示例：D:/nginx/files/
    # Linux/Mac 示例：/usr/local/nginx/files/
    path: F:/zjnu_workspace/task_manage/backend/files/  # 指向隔离目录
    # 前端访问上传文件的前缀（和 Nginx 配置对应）
    access-path: /files/      # 前端访问前缀
```
> 