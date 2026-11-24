# Element Plus 2.14 依赖 @vueuse/core 15，要求 Node >= 22
FROM node:22-alpine AS build
# 容器内 DNS 可能只返回 IPv6，强制 IPv4 优先，避免 npm 下载中途崩溃
ENV NODE_OPTIONS=--dns-result-order=ipv4first
WORKDIR /app
COPY package*.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
ENV BACKEND_HOST=g12-backend BACKEND_PORT=8080
EXPOSE 80
