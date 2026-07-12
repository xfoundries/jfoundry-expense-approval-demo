# 团队费用报销审批 Demo

这是一个从完整业务需求出发构建的 REST API Demo，用于验证 `domain-architecture` 插件能否支撑需求理解、领域建模、架构选择、可选框架落地、实施与验收的完整过程。

本项目选择使用本地 jfoundry，是为了验证插件的 `using-jfoundry` 阶段。**jfoundry 不是 domain-architecture 插件的必备条件**；非 jfoundry 项目仍可独立使用领域建模和架构指导能力。

## 技术与架构

- Java 21、Maven、Spring Boot 3.5.16
- jfoundry 1.0.0-SNAPSHOT
- MyBatis-Plus 3.5.16、H2 文件数据库
- springdoc-openapi 2.8.17
- 单 Maven 模块，包级 Hexagonal Architecture 边界
- JFoundryRules、jMolecules DDD 和 aggregate repository 架构测试

业务核心是 `ExpenseClaim` 聚合。写用例通过 primary port 进入 application service，聚合 repository 和查询 port 由 MyBatis adapter 实现。项目没有采用 CQRS、Event Sourcing、Outbox、Inbox、消息队列或分布式锁。

## 前置条件

- JDK 21
- Maven 3.9+
- 本地 jfoundry 源码位于 `/Users/huangxiao/Workspace/mine/jfoundry`
- 运行 shell 验收脚本时需要 `curl` 和 `jq`

首次构建前安装本地 jfoundry：

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install -DskipTests
```

如果用户级 Maven 配置中的私有仓库不可用，可以临时使用仅包含 Maven Central 的 settings；本项目本身不要求修改全局 Maven 配置。

## 启动

```bash
cd /Users/huangxiao/Workspace/mine/expense-approval-demo
mvn spring-boot:run
```

启动后访问：

- Swagger UI: <http://localhost:8080/swagger-ui.html>
- OpenAPI JSON: <http://localhost:8080/v3/api-docs>

默认数据库文件在 `data/expense-approval.mv.db`。应用正常重启后数据仍然存在；需要清空 Demo 数据时，停止应用后删除 `data/` 目录。

## 身份模拟

每个业务请求都使用两个请求头：

```text
X-User-Id: employee-1
X-User-Role: EMPLOYEE
```

角色可选值为 `EMPLOYEE`、`MANAGER` 和 `FINANCE`。这是 Demo 的简化身份机制，不包含登录、JWT、用户或组织管理。

## 业务流程

1. 员工创建草稿并维护费用项。
2. 至少包含一个费用项后可以提交。
3. 总额不超过 2,000.00 CNY 时，主管批准即完成。
4. 总额超过 2,000.00 CNY 时，主管批准后还需要财务批准。
5. 当前审批阶段可以驳回，且必须填写原因。
6. 员工可将已驳回报销单重新打开为草稿，修改并再次提交。
7. 最终批准前员工可以撤回；已批准和已撤回是终态。
8. 任何人都不能审批自己的报销单。

## 快速调用

创建草稿：

```bash
curl --fail-with-body -i -X POST http://localhost:8080/api/claims \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: employee-1' \
  -H 'X-User-Role: EMPLOYEE' \
  -d '{"title":"客户拜访"}'
```

响应的 `Location` 是新报销单地址。向报销单添加费用项：

```bash
curl --fail-with-body -X POST http://localhost:8080/api/claims/CLAIM_ID/items \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: employee-1' \
  -H 'X-User-Role: EMPLOYEE' \
  -d '{"expenseDate":"2026-07-10","category":"TRAVEL","amount":120.50,"description":"出租车","receiptReference":"receipt-1"}'
```

提交并由主管批准：

```bash
curl --fail-with-body -X POST http://localhost:8080/api/claims/CLAIM_ID/submit \
  -H 'X-User-Id: employee-1' -H 'X-User-Role: EMPLOYEE'

curl --fail-with-body -X POST http://localhost:8080/api/claims/CLAIM_ID/manager-approval \
  -H 'X-User-Id: manager-1' -H 'X-User-Role: MANAGER'
```

查看详情：

```bash
curl --fail-with-body http://localhost:8080/api/claims/CLAIM_ID \
  -H 'X-User-Id: employee-1' -H 'X-User-Role: EMPLOYEE' | jq
```

## 测试与验收

```bash
mvn test
mvn clean verify
```

应用在 8080 端口运行时，可执行真实 HTTP 验收：

```bash
bash scripts/acceptance.sh http://localhost:8080
```

测试覆盖 domain、application、persistence、query、Web、Hexagonal 架构规则以及随机端口端到端场景。错误响应使用 RFC 9457 `ProblemDetail`。

