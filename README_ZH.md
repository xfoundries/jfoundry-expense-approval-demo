# 团队费用报销审批 Demo

中文 | [English](README.md)

> **当前分支：** 本 README 和当前检出的 `onion-architecture` 分支描述 Onion Simple
> Architecture 实现。Hexagonal 实现单独维护在
> [`main`](https://github.com/xfoundries/jfoundry-expense-approval-demo/tree/main) 分支。

这是一个业务轻量、架构链路完整的 Demo，用于验证 AI Agent 能否借助
`domain-architecture` 插件和可选的 jfoundry，从完整需求出发完成领域建模、架构决策、
实现与自动化验收。项目刻意不扩展组织、审批矩阵等复杂业务。当前 `onion-architecture` 分支重点
验证 DDD、Onion Simple Architecture、CQRS、Outbox、Inbox、Kafka、分布式锁和持久化能力能否
正确组合；独立 `main` 分支复用相同业务与验收基线验证 Hexagonal Architecture，不改变本分支的
架构风格。

本项目明确选择了本地 jfoundry，以验证插件的 `using-jfoundry` 落地阶段。
**jfoundry 不是 `domain-architecture` 插件的必备条件**；不使用 jfoundry 的项目仍可独立使用
领域建模和架构指导能力。

## 架构变体

| 分支 | 架构风格 | 本 README 中的状态 |
|------|----------|--------------------|
| `onion-architecture` | Onion Simple Architecture | 当前实现；下文 Ring、包结构和依赖说明均以此分支为准。 |
| [`main`](https://github.com/xfoundries/jfoundry-expense-approval-demo/tree/main) | Hexagonal Architecture | 单独维护的默认分支变体，拥有自己的 README 和架构语义。 |

除非某一节明确写明“跨变体”或直接点名 `main`，本 README 中关于 Ring、包结构、命名和依赖方向的
说明都指当前 Onion Simple 分支。

## 项目结构

```text
jfoundry-expense-approval-demo/
├── integration-contracts/          # 版本化的跨进程消息契约，不共享领域模型
├── expense-approval-service/       # DDD + Onion Simple 的费用审批服务
├── payment-processor-simulator/    # 业务简单的外部支付系统模拟器
└── end-to-end-tests/               # 两个应用及真实中间件的完整链路测试
```

根项目是 Maven 聚合工程。普通构建包含前三个模块；`end-to-end-tests` 只在 `e2e` profile
中启用，避免日常模块构建无条件启动完整容器拓扑。

## 架构与技术（`onion-architecture`：Onion Simple）

- Java 21、Maven、Spring Boot 3.5.16、jfoundry 1.0.0-SNAPSHOT
- MyBatis-Plus、PostgreSQL 17、Flyway
- Kafka、Redis/Redisson、Testcontainers
- JUnit 5、ArchUnit、Awaitility
- 命令侧使用 `ExpenseClaim` 聚合和显式 Command/Handler/Dispatcher
- 查询侧使用面向视图的 MyBatis 查询与支付状态投影，不还原聚合
- CQRS 不使用 Event Sourcing，命令表和查询投影仍位于同一个费用数据库

费用服务通过包和 jfoundry 注解表达 Onion Ring：

```text
expenseapproval
├── domain              @DomainRing：领域模型与聚合 Repository 契约
├── application         @ApplicationRing：报销单、审批、支付与身份能力
├── infrastructure      @InfrastructureRing：Web、消息、持久化与查询实现
└── boot                组合根与运行时装配，不属于业务 Ring
```

依赖从 infrastructure 经 application 指向 domain。CQRS、Repository、Outbox、Inbox 和分布式锁
继续承担独立于所选主架构风格的各自职责。

包和类型命名以 DDD 为先。`application.claim.command/query` 把 CQRS 角色放在报销单业务能力之下，
审批和支付编排分别拥有自己的能力包。Onion Architecture 没有 Port 或 Adapter 角色体系。本 Demo
使用 `ExpenseClaimViewReader`、`ApprovedExpenseAmountReader` 和
`PaymentStatusProjectionStore`，是因为 `Reader`、`Store` 能直接表达这些 Java 契约的职责。
这些后缀只是本项目约定，不是 DDD 或 Onion 官方模式，也不是 jfoundry 的强制要求；
`MybatisExpenseClaimViewReader` 这类技术名称只出现在基础设施实现中。

这些持久化 Reader 与 Store 继承 jfoundry 的 `AbstractPersistenceAdapter`：契约方法显式使用
`query` 或 `modify`，Spring 运行时集成负责技术异常翻译，不再通过构造器注入 translator 或在业务
Adapter 中手写 try/catch。

各机制承担不同职责：

- `ExpenseClaim` 保护单张报销单的状态转换和审批规则。
- MyBatis-Plus 乐观锁保护单个聚合的并发修改。
- Redis 锁串行化“员工 + 月份”维度的跨聚合额度判断。
- Transactional Outbox 保证业务修改与待发布消息在同一数据库事务提交。
- Inbox 以 `eventId + consumerName` 处理 Kafka 至少一次投递产生的重复消息。
- Kafka 只承载版本化集成事件，不直接发布内部领域事件。
- 支付结果属于查询投影，不进入 `ExpenseClaim` 聚合，也不会因支付失败撤销审批。

## 完整消息链路

```text
HTTP 命令
  -> ExpenseClaim 聚合
  -> 费用 PostgreSQL + 审批 Outbox
  -> Kafka: expense-approval.events.v1
  -> 支付 Inbox + 支付结果 Outbox
  -> Kafka: payment.events.v1
  -> 费用 Inbox + claim_payment_status 投影
  -> HTTP 查询返回 PENDING / PAID / FAILED
```

默认业务规则只保留验证架构所需的最小复杂度：员工每月最终批准额度为
`10,000.00 CNY`；单笔支付金额不超过 `8,000.00 CNY` 时成功，超过时返回确定性的失败结果。

## 前置条件

- JDK 21
- Maven 3.9+
- Docker 可用；容器测试会启动两个 PostgreSQL、Kafka 和 Redis
- 本地 jfoundry 源码位于 `/Users/huangxiao/Workspace/mine/jfoundry`

首次构建前安装当前本地 jfoundry：

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry
mvn clean install -DskipTests
```

## 测试与验收

普通模块测试：

```bash
cd /Users/huangxiao/Workspace/mine/jfoundry-expense-approval-demo
mvn test
```

包含完整跨应用链路的验收：

```bash
mvn clean verify -Pe2e
```

`e2e` profile 使用 Testcontainers 启动两个独立 PostgreSQL、Kafka、Redis 和两个 Spring Boot
应用上下文，验证以下 5 个真实场景：

1. 最终审批后支付成功并投影为 `PAID`。
2. 单笔金额超过支付上限后投影为 `FAILED`。
3. 重复投递同一个审批事件时，支付 Inbox 只处理一次。
4. 同一员工同月两笔并发额度竞争时，只允许一笔最终批准。
5. 月度额度超限时聚合状态回滚，且不写审批 Outbox。

Demo 不重复 jfoundry 已覆盖的 dispatcher 重试、恢复、清理和并发 claim 细节；这里验证的是
框架能力能否在真实业务边界上正确组合。

## 本地运行

本地运行需要预先准备：

- 费用 PostgreSQL：`localhost:5432/expense_approval`
- 支付 PostgreSQL：`localhost:5432/payment_simulator`
- Kafka：`localhost:9092`
- Redis：`localhost:6379`

两个应用启动时会分别执行各自模块 `src/main/resources/db/migration/` 下的 Flyway migration。
也可以通过下列环境变量覆盖连接信息：

| 环境变量 | 默认值 |
|---|---|
| `EXPENSE_APPROVAL_DB_URL` | `jdbc:postgresql://localhost:5432/expense_approval` |
| `EXPENSE_APPROVAL_DB_USERNAME` / `EXPENSE_APPROVAL_DB_PASSWORD` | `expense_approval` |
| `PAYMENT_SIMULATOR_DB_URL` | `jdbc:postgresql://localhost:5432/payment_simulator` |
| `PAYMENT_SIMULATOR_DB_USERNAME` / `PAYMENT_SIMULATOR_DB_PASSWORD` | `payment_simulator` |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `EXPENSE_APPROVAL_REDIS_HOST` / `EXPENSE_APPROVAL_REDIS_PORT` | `localhost` / `6379` |

默认配置会关闭 Kafka listener 和 Outbox dispatcher，避免只启动单个应用时产生无效后台重试。
完整联调时显式开启：

```bash
export JFOUNDRY_OUTBOX_DISPATCHER_MODE=scheduled
export EXPENSE_APPROVAL_KAFKA_LISTENER_ENABLED=true
export PAYMENT_SIMULATOR_KAFKA_LISTENER_ENABLED=true

mvn -pl payment-processor-simulator spring-boot:run
```

另开终端启动费用服务：

```bash
mvn -pl expense-approval-service spring-boot:run
```

费用服务默认监听 `8080`：

- Swagger UI：<http://localhost:8080/swagger-ui.html>
- OpenAPI JSON：<http://localhost:8080/v3/api-docs>

支付模拟器是 Kafka 驱动的无 Web 应用，不暴露 HTTP 端口。

## 业务接口

每个费用业务请求使用两个简化身份请求头：

```text
X-User-Id: employee-1
X-User-Role: EMPLOYEE
```

角色可选值为 `EMPLOYEE`、`MANAGER`、`FINANCE`。主要流程是员工创建草稿、维护费用项并提交；
主管审批低金额报销后直接进入最终批准，高金额报销继续进入财务审批；最终批准会检查月度额度并
异步发起支付。任何人不能审批自己的报销单。

创建报销单示例：

```bash
curl --fail-with-body -i -X POST http://localhost:8080/api/claims \
  -H 'Content-Type: application/json' \
  -H 'X-User-Id: employee-1' \
  -H 'X-User-Role: EMPLOYEE' \
  -d '{"title":"客户拜访"}'
```

查询报销单详情时，响应中的支付状态会从 `PENDING` 最终收敛为 `PAID` 或 `FAILED`。

---

## 验证结论与能力边界

### 当前分支判断

在 Java 21、Spring Boot、MyBatis-Plus、PostgreSQL、Kafka 和 Redis 这一已选技术栈内，本 Demo
当前在 `onion-architecture` 使用 Onion Simple Architecture。本分支证明 `domain-architecture`
插件与可选的 jfoundry 可以支撑 AI Agent 从业务需求、领域建模和 Onion 架构决策，一直完成领域
实现、CQRS、Outbox、Inbox、分布式锁、持久化和端到端验收。

### 跨变体验证

单独维护的 `main` 分支把同一个应用实现为 Hexagonal Architecture，没有改变业务规则、数据库模型、
集成契约或验收场景。两个分支共同验证两种可选的架构决策；它们不是同时存在于当前
`onion-architecture` 代码中的两套架构风格。

这个结论不是由脚手架结构得出的，而是来自真实业务链路：HTTP 命令进入聚合，业务数据与 Outbox
在同一事务提交，Kafka 跨进程投递，两个消费者通过 Inbox 保证幂等，支付结果最终进入查询投影；
跨聚合月度额度则由 Redis 锁与数据库事务共同保护。

### 跨变体验证证据

- jfoundry 在 Java 21 和 Java 25 下完成两套 67 模块测试矩阵。
- `domain-architecture` 插件的全部 skill、Codex plugin manifest 和 Claude marketplace 通过校验。
- 两个架构变体均通过同一套完整自动化测试，其中完整容器 E2E 为 `5/5`。
- Onion 验证覆盖显式 Domain、Application、Infrastructure Ring、向内依赖规则、DDD Repository
  约定和已有 CQRS 结构。
- E2E 覆盖支付成功、支付失败、重复投递、并发月度额度，以及超额回滚且不写 Outbox。
- 独立启动两个应用和真实中间件后，HTTP 审批到支付状态 `PAID` 的完整链路再次通过。

### Demo 自身得到的修正

Demo 从原来的单模块费用审批项目演进为四模块项目，并保持业务逻辑刻意简单。CQRS 没有演变成通用
命令总线，支付模拟器也没有被强行建模为 DDD 聚合；每个机制只承担一个真实且必要的职责。

同时修正了混合包使用包级 Port 注解、E2E 无独立 Maven profile、固定 sleep 等项目表达问题，
并让 Kafka listener 与 Outbox dispatcher 在普通本地启动时默认关闭、在完整集成环境中显式开启。
随后在不改变业务的前提下，将 Hexagonal 角色迁移为 Onion Ring：Web 与消息入口进入基础设施层，
应用依赖采用职责优先命名，领域 Repository 继续保留为内环的 DDD 契约。

### 仍需单独评估的技术范围

Spring 7 复合注解属性映射问题已经由 jMolecules 上游的开放
[issue #153](https://github.com/xmolecules/jmolecules/issues/153) 跟踪，不应在 jfoundry 的框架中立
模块中通过 Spring `@AliasFor` 兼容。Demo 暴露的 BeanPostProcessor 提前初始化问题实际来自
jfoundry Spring AOP 自动配置，而不是 `jmolecules-jackson`；jfoundry 现已统一使用 Spring 规范的
auto-proxy creator 并延迟解析 advisor interceptor。后续验证应优先选择真正不同的 Runtime 或
基础设施实现，而不是继续增加费用审批业务复杂度。

### 能力边界

当前可以确认的是：在已经验证的技术栈和业务复杂度内，jfoundry 与 `domain-architecture` 插件足以
支撑 AI Agent 开发一个基于 DDD、经过分别验证的 Hexagonal 或 Onion Simple 架构风格，以及可靠
消息的完整业务项目。这不代表两种架构可以混用，也不代表可以跳过前序架构决策直接选择其中之一。

当前分支仍是 Onion Simple；Hexagonal 结论来自单独维护的 `main` 分支。

本次 Onion 验证是在一个 Maven 应用模块内通过包级 Ring 完成的，因此验证了 ArchUnit 依赖规则，
但尚未验证 Ring 拆分为独立 Maven 模块后的编译期隔离。结论也不能直接外推到非 Spring Runtime、
其他 ORM 或其他消息中间件；安全、可观测性、部署、容量和生产运维也不在本 Demo 的验证范围。
