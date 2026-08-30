# kkfileview 5.0.2 Dockerfile
# Build: triggered by .github/workflows/build-main.yml
# Base image: built by build-base.yml (含 JDK 21 + LibreOffice + 中文字体)
#
# 用法（GitHub Actions 中）：
#   docker buildx build \
#     --build-arg BASE_IMAGE=ghcr.io/kinaxng/kkfileview-base:5.0.0 \
#     -t ghcr.io/kinaxng/kkfileview:5.0.2 \
#     --push .
#
# 本地 build（如果 base 已拉）：
#   docker build -t kkfileview:5.0.2-local --build-arg BASE_IMAGE=keking/kkfileview-base:5.0.0 .

ARG BASE_IMAGE=keking/kkfileview-base:5.0.0
FROM ${BASE_IMAGE}

# 主镜像层：只加 jar 包
ADD server/target/kkFileView-*.tar.gz /opt/

# 设置 KK 文件路径（5.0.2 是官方 pom 里的 version）
ENV KKFILEVIEW_BIN_FOLDER=/opt/kkFileView-5.0.2/bin

ENTRYPOINT ["java", \
    "-Dfile.encoding=UTF-8", \
    "-Dspring.config.location=/opt/kkFileView-5.0.2/config/application.properties", \
    "-jar", \
    "/opt/kkFileView-5.0.2/bin/kkFileView-5.0.2.jar"]