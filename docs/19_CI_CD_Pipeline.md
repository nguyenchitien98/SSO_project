# SSO Platform - Đặc Tả Quy Trình CI/CD (GitLab CI Pipeline)

Tài liệu này đặc tả quy trình tích hợp và triển khai liên tục (CI/CD) tự động cho dự án **SSO Platform** sử dụng công cụ **GitLab CI/CD**.

---

## 1. Tổng Quan Pipeline Workflow

Pipeline được cấu hình chạy tự động mỗi khi có commit mới được push lên các nhánh git:
- Nhánh `feature/*`: Chỉ chạy các jobs compile, test và code quality scan.
- Nhánh `develop`: Build và push Docker images nhãn `:latest` lên GitLab Container Registry.
- Nhánh `main` (hoặc `master`): Build và push Docker images nhãn `:release-X.Y` và tự động deploy lên môi trường Staging/Production.

Pipeline bao gồm 4 stages chính:
```
[Stage: Build] ──> [Stage: Test & Quality] ──> [Stage: Dockerize] ──> [Stage: Deploy]
```

---

## 2. Đặc Tả File Cấu Hình `.gitlab-ci.yml`

```yaml
stages:
  - build
  - test
  - dockerize
  - deploy

variables:
  MAVEN_OPTS: "-Dmaven.repo.local=.m2/repository"
  DOCKER_REGISTRY: "registry.gitlab.com/sso-platform/sso-project"

cache:
  paths:
    - .m2/repository/

# ==================== STAGE: BUILD ====================
compile:
  stage: build
  image: maven:3.9.6-eclipse-temurin-21-alpine
  script:
    - mvn clean compile
  only:
    - merge_requests
    - develop
    - main

# ==================== STAGE: TEST ====================
unit-tests:
  stage: test
  image: maven:3.9.6-eclipse-temurin-21-alpine
  script:
    - mvn test
  artifacts:
    reports:
      junit: "**/target/surefire-reports/TEST-*.xml"
    paths:
      - "**/target/site/jacoco/"
    expire_in: 7 days
  only:
    - merge_requests
    - develop
    - main

spotless-check:
  stage: test
  image: maven:3.9.6-eclipse-temurin-21-alpine
  script:
    - mvn spotless:check
  only:
    - merge_requests

# ==================== STAGE: DOCKERIZE ====================
.docker-build-template:
  stage: dockerize
  image: docker:24.0.7
  services:
    - docker:24.0.7-dind
  before_script:
    - echo "$CI_REGISTRY_PASSWORD" | docker login -u "$CI_REGISTRY_USER" --password-stdin $CI_REGISTRY
  script:
    - docker build -t $DOCKER_REGISTRY/$SERVICE_NAME:$CI_COMMIT_REF_SLUG-$CI_COMMIT_SHORT_SHA .
    - docker push $DOCKER_REGISTRY/$SERVICE_NAME:$CI_COMMIT_REF_SLUG-$CI_COMMIT_SHORT_SHA
    - |
      if [ "$CI_COMMIT_BRANCH" = "main" ]; then
        docker tag $DOCKER_REGISTRY/$SERVICE_NAME:$CI_COMMIT_REF_SLUG-$CI_COMMIT_SHORT_SHA $DOCKER_REGISTRY/$SERVICE_NAME:latest
        docker push $DOCKER_REGISTRY/$SERVICE_NAME:latest
      fi
  only:
    - develop
    - main

docker-sso-server:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "sso-server"
  before_script:
    - cd sso-server

docker-api-gateway:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "api-gateway"
  before_script:
    - cd microservice-app/api-gateway

docker-order-service:
  extends: .docker-build-template
  variables:
    SERVICE_NAME: "order-service"
  before_script:
    - cd microservice-app/order-service

# ==================== STAGE: DEPLOY ====================
deploy-to-kubernetes:
  stage: deploy
  image: dtzar/helm-kubectl:latest
  script:
    - kubectl config set-cluster k8s-cluster --server="$K8S_SERVER" --insecure-skip-tls-verify=true
    - kubectl config set-credentials gitlab-admin --token="$K8S_TOKEN"
    - kubectl config set-context default --cluster=k8s-cluster --user=gitlab-admin
    - kubectl config use-context default
    # Apply các file manifest Kubernetes lên namespace sso-platform
    - kubectl apply -f k8s/configmaps/
    - kubectl apply -f k8s/secrets/
    - kubectl apply -f k8s/deployments/
    - kubectl apply -f k8s/services/
    - kubectl apply -f k8s/ingresses/
    # Thực hiện rollout restart để kéo các image mới nhất vừa push
    - kubectl rollout restart deployment/sso-server -n sso-platform
    - kubectl rollout restart deployment/api-gateway -n sso-platform
    - kubectl rollout restart deployment/order-service -n sso-platform
  only:
    - main
```

---

## 3. Các Quy Tắc Bảo Mật Cho Pipeline

1. **Quản lý Secrets (Variables):** Tuyệt đối không hardcode mật khẩu DB, khóa API, JWT private key hay OAuth2 credentials của client vào tệp `.gitlab-ci.yml`. Sử dụng tính năng **Masked & Protected Variables** trong GitLab Settings > CI/CD để lưu trữ các biến nhạy cảm (như `$K8S_TOKEN`, `$CI_REGISTRY_PASSWORD`).
2. **Quét lỗ hổng ảnh (Trivy Scan):** Nên tích hợp thêm công cụ Trivy quét lỗ hổng bảo mật của các Docker Base Images trước khi push lên GitLab Registry để phòng ngừa các lỗ hổng hệ điều hành thư viện.
3. **Protected Branches:** Chỉ cho phép Maintainers push/merge code vào nhánh `main` và `develop` để kích hoạt các job build Docker và deploy, ngăn chặn các lập trình viên không có thẩm quyền tự ý cập nhật môi trường chạy thực tế.
