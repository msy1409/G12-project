# 多阶段构建：Maven 编译 -> 精简 JRE 运行
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# 使用阿里云镜像（仓库所在网络直连 repo1.maven.org 会超时）
COPY settings.xml /root/.m2/settings.xml

# 先只拷 pom，把依赖层单独缓存，改代码时不必重新下载依赖
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY src ./src
RUN mvn -B -q clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/g12-backend.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
