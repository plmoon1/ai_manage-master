# 基础镜像
FROM openjdk:8u342
# 设定时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
# 拷贝jar包
COPY springboot-0.0.1-SNAPSHOT.jar /app.jar
# 入口
ENTRYPOINT ["java", "-jar", "/app.jar"]