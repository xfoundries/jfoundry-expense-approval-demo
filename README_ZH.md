# 团队费用报销审批 Demo

中文 | [English](README.md)

> **当前分支：** 本 README 和当前检出的 `hexagonal/jpa` 分支描述 Hexagonal Architecture 实现。
> Onion Simple 实现单独维护在
> [`onion-architecture`](https://github.com/xfoundries/jfoundry-expense-approval-demo/tree/onion-architecture) 分支。

这是一个业务轻量、架构链路完整的 Demo，用于验证 AI Agent 能否借助
[domain-architecture-skills](https://github.com/xfoundries/domain-architecture-skills) 插件和可选的 [jfoundry](https://github.com/xfoundries/jfoundry)，从完整需求出发完成领域建模、架构决策、
实现与自动化验收。项目刻意不扩展组织、审批矩阵等复杂业务。当前 `hexagonal/jpa` 分支重点验证 DDD、
Hexagonal Architecture、CQRS、Outbox、Inbox、Kafka、分布式锁和持久化能力能否正确组合；
独立 Onion 分支复用相同业务与验收基线验证 Onion Simple，不改变 `hexagonal/jpa` 的架构风格。

本项目明确选择了本地 jfoundry，以验证插件的 `using-jfoundry` 落地阶段。
**jfoundry 不是 `domain-architecture` 插件的必备条件**；不使用 jfoundry 的项目仍可独立使用
领域建模和架构指导能力。

## 架构变体

| 分支 | 架构风格 | 本 README 中的状态 |
|------|----------|--------------------|
| `hexagonal/jpa` | Hexagonal Architecture | 仓库默认分支上的当前实现；下文包结构和运行时说明均以此分支为准。 |
| [`onion-architecture`](https://github.com/xfoundries/jfoundry-expense-approval-demo/tree/onion-architecture) | Onion Simple Architecture | 单独维护的验证变体，拥有自己的 README 和架构语义。 |

除非某一节明确写明“跨变体”或直接点名 Onion 分支，本 README 中关于 `application`、Port、
Adapter、包结构和依赖方向的说明都指当前 Hexagonal `hexagonal/jpa` 分支。

## 项目结构

```text
jfoundry-expense-approval-demo/
├── integration-contracts/          # 版本化的跨进程消息契约，不共享领域模型
├── expense-approval-service/       # DDD 费用审批服务；hexagonal/jpa 为 Hexagonal，变体分支为 Onion Simple
├── payment-processor-simulator/    # 业务简单的外部支付系统模拟器
└── end-to-end-tests/               # 两个应用及真实中间件的完整链路测试
```

根项目是 Maven 聚合工程。普通构建包含前三个模块；`end-to-end-tests` 只在 `e2e` profile
中启用，避免日常模块构建无条件启动完整容器拓扑。

## 架构与技术（`hexagonal/jpa`：Hexagonal）

- Java 21、Maven、Spring Boot 3.5.16、jfoundry 1.0.0-SNAPSHOT
- 费用审批服务：Jakarta Persistence、PostgreSQL 17、Flyway
- 支付处理模拟器：MyBatis-Plus、PostgreSQL 17、Flyway
- Kafka、Redis/Redisson、Testcontainers
- JUnit 5、ArchUnit、Awaitility
- 命令侧使用 `ExpenseClaim` 聚合和显式 Command/Handler/Dispatcher
- 查询侧使用 JPA 读适配器与支付状态投影，不还原聚合
- CQRS 不使用 Event Sourcing，命令表和查询投影仍位于同一个费用数据库

费用服务在明确表达 Hexagonal 角色的同时，按业务能力组织应用核心：

```text
expenseapproval
├── domain                    聚合、值对象、Repository 与领域策略
├── application
│   ├── claim/command         命令与处理器；port/in 放置命令 Primary Port
│   ├── claim/query           查询服务与视图模型；port/in 与 port/out 放置边界契约
│   ├── approval              最终审批；port/out 放置已审批金额 Port
│   ├── payment               支付投影；port/out 放置其持久化 Port
│   └── identity              审批参与者与角色
└── adapter
    ├── in                    HTTP 与 Kafka Primary Adapter
    │   ├── web
    │   └── messaging
    └── out                   持久化与查询 Secondary Adapter
        ├── persistence
        └── query
```

应用核心以业务能力优先，只在需要方向契约时向下设置 `port.in` 和 `port.out`。本分支使用
`UseCase`、`Port`、`Adapter`，是因为它明确选择了 Hexagonal Architecture；这些名称不是通用
DDD 规则。本分支选择 `adapter.in/out` 包约定；`adapter.primary/secondary` 是等价替代，但同一
项目不能混用两套词汇。

`ClaimViews` 位于中立的 `application.claim.query.view`，因为入站与出站查询契约都会使用它。
它归 application 的报销查询能力所有，不归任一 Port 方向，也不属于领域模型或出站 Adapter 实现。

各机制承担不同职责：

- `ExpenseClaim` 保护单张报销单的状态转换和审批规则。
- JPA `@Version` 乐观锁保护单个聚合的并发修改。
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

在 Java 21、Spring Boot、Jakarta Persistence、PostgreSQL、Kafka 和 Redis 这一已选技术栈内，费用
审批服务当前在 `hexagonal/jpa` 使用 Hexagonal Architecture。支付处理模拟器仍是有意保持简单的
MyBatis-Plus 应用。本分支证明 `domain-architecture` 插件与可选的
jfoundry 可以支撑 AI Agent 从业务需求、领域建模和 Hexagonal 架构决策，一直完成领域实现、
CQRS、Outbox、Inbox、分布式锁、持久化和端到端验收。

### 跨变体验证

单独维护的 `onion-architecture` 分支把同一个应用改为 Onion Simple Architecture，没有改变业务
规则、数据库模型、集成契约或验收场景。两个分支共同验证两种可选的架构决策；它们不是同时存在于
当前 `hexagonal/jpa` 代码中的两套架构风格。

这个结论不是由脚手架结构得出的，而是来自真实业务链路：HTTP 命令进入聚合，业务数据与 Outbox
在同一事务提交，Kafka 跨进程投递，两个消费者通过 Inbox 保证幂等，支付结果最终进入查询投影；
跨聚合月度额度则由 Redis 锁与数据库事务共同保护。

### 跨变体验证证据

- jfoundry 在 Java 21 和 Java 25 下完成两套 67 模块测试矩阵。
- `domain-architecture` 插件的全部 skill、Codex plugin manifest 和 Claude marketplace 通过校验。
- 两个架构变体均通过同一套完整自动化测试，其中完整容器 E2E 为 `5/5`。
- Onion 验证覆盖显式 Domain、Application、Infrastructure Ring、向内依赖规则、DDD Repository
  约定和已有的按需 CQRS 结构。
- E2E 覆盖支付成功、支付失败、重复投递、并发月度额度，以及超额回滚且不写 Outbox。
- 独立启动两个应用和真实中间件后，HTTP 审批到支付状态 `PAID` 的完整链路再次通过。

### Demo 自身得到的修正

Demo 从原来的单模块费用审批项目演进为四模块项目，并保持业务逻辑刻意简单。CQRS 没有演变成通用
命令总线，支付模拟器也没有被强行建模为 DDD 聚合；每个机制只承担一个真实且必要的职责。

同时修正了混合包使用包级 Port 注解、E2E 无独立 Maven profile、固定 sleep 等项目表达问题，
并让 Kafka listener 与 Outbox dispatcher 在普通本地启动时默认关闭、在完整集成环境中显式开启。
应用核心现在按报销单、审批、支付和身份组织，不再使用全局 `command`、`service`、`integration`、
`port` 技术桶，方向性的 Port 包改为位于其所属业务能力之下；领域策略回到 domain，同时在类型级
继续明确表达 Hexagonal 的 Primary/Secondary Port 与 Adapter 角色。

随后在不改变验收行为的前提下，将同一业务实现迁移为 Onion Ring：Web 与消息入口进入
infrastructure，应用依赖采用职责优先命名，领域 Repository 继续保留为内环的 DDD 契约。

### 能力边界

当前可以确认的是：在已经验证的技术栈和业务复杂度内，jfoundry 与 `domain-architecture` 插件足以
支撑 AI Agent 开发一个基于 DDD、分别经过验证的 Hexagonal 或 Onion Simple 架构风格，以及可靠消息的
完整业务项目。两种风格仍是独立的架构选择，不是一套合并模型。

本次 Onion 验证是在一个 Maven 应用模块内通过包级 Ring 完成的，因此验证了 ArchUnit 依赖规则，
但尚未验证 Ring 拆分为独立 Maven 模块后的编译期隔离。结论也不能直接外推到非 Spring Runtime、
其他 ORM 或其他消息中间件；安全、可观测性、部署、容量和生产运维也不在本 Demo 的验证范围。
Spring 7 复合注解属性映射问题
已经由 jMolecules 上游的开放 [issue #153](https://github.com/xmolecules/jmolecules/issues/153) 跟踪，
不应在 jfoundry 的框架中立模块中通过 Spring `@AliasFor` 兼容。Demo 暴露的 BeanPostProcessor
提前初始化问题实际来自 jfoundry Spring AOP 自动配置，而不是 `jmolecules-jackson`；jfoundry
现已统一使用 Spring 规范的 auto-proxy creator 并延迟解析 advisor interceptor。后续验证应优先
选择真正不同的 Runtime 或基础设施实现，而不是继续增加费用审批业务复杂度。
